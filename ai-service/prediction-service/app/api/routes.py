"""
API 라우터
"""
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import List, Optional
from datetime import datetime
from decimal import Decimal

router = APIRouter()


class OHLCVPoint(BaseModel):
    """OHLCV 한 시점 (optional series용)"""
    date: Optional[str] = None
    open: float
    high: float
    low: float
    close: float
    volume: Optional[float] = 0.0


class PredictionRequest(BaseModel):
    """예측 요청 DTO"""
    symbol: str
    predictionMinutes: int
    modelType: Optional[str] = "ensemble"
    lookbackDays: Optional[int] = 30
    requestedAt: Optional[datetime] = None
    series: Optional[List[OHLCVPoint]] = None
    currentPrice: Optional[float] = None


class PredictionResponse(BaseModel):
    """예측 응답 DTO"""
    symbol: str
    currentPrice: Decimal
    predictedPrice: Decimal
    predictedPriceLower: Optional[Decimal] = None
    predictedPriceUpper: Optional[Decimal] = None
    expectedReturn: Decimal
    confidence: Decimal
    volatility: Optional[Decimal] = None
    direction: str
    modelType: str
    predictedAt: datetime
    predictionMinutes: int


def _mock_response(request: PredictionRequest) -> PredictionResponse:
    current_price = Decimal(str(request.currentPrice or "100.0"))
    predicted_price = Decimal("105.0")
    confidence = Decimal("0.75")
    return PredictionResponse(
        symbol=request.symbol,
        currentPrice=current_price,
        predictedPrice=predicted_price,
        predictedPriceLower=predicted_price * Decimal("0.95"),
        predictedPriceUpper=predicted_price * Decimal("1.05"),
        expectedReturn=(predicted_price - current_price) / current_price * Decimal("100"),
        confidence=confidence,
        volatility=Decimal("2.5"),
        direction="UP" if predicted_price > current_price else "DOWN",
        modelType=request.modelType or "ensemble",
        predictedAt=datetime.now(),
        predictionMinutes=request.predictionMinutes,
    )


@router.get("/health")
async def health_check():
    """Health check 엔드포인트"""
    return {
        "status": "ok",
        "service": "ai-prediction-service",
        "timestamp": datetime.now().isoformat()
    }


@router.post("/predict", response_model=PredictionResponse)
async def predict_price(request: PredictionRequest):
    """
    단일 종목 가격 예측.
    series·currentPrice가 있고 LSTM 모델이 로드된 경우 LSTM 추론, 그 외 Mock.
    """
    if request.series and len(request.series) >= (request.lookbackDays or 30):
        from app.services.predictor import get_model
        from app.preprocessing.transform import preprocess_series_for_serving
        import torch

        model = get_model()
        if model is not None:
            lookback = request.lookbackDays or 30
            close = [p.close for p in request.series]
            open_ = [p.open for p in request.series]
            high = [p.high for p in request.series]
            low = [p.low for p in request.series]
            volume = [p.volume or 0.0 for p in request.series]
            x = preprocess_series_for_serving(
                close=close, open_=open_, high=high, low=low, volume=volume,
                lookback=lookback, use_ohlcv=True,
            )
            if x.size == 0:
                return _mock_response(request)
            with torch.no_grad():
                t = torch.from_numpy(x)
                out = model(t)
                predicted_close = float(out[0, 0].item())
            predicted_close = max(0.0, predicted_close)
            current_price = request.currentPrice
            if current_price is None and close:
                current_price = float(close[-1])
            if current_price is None:
                current_price = 100.0
            current_price = float(current_price)
            direction = "UP" if predicted_close > current_price else "DOWN"
            expected_return = (predicted_close - current_price) / current_price * 100.0
            return PredictionResponse(
                symbol=request.symbol,
                currentPrice=Decimal(str(current_price)),
                predictedPrice=Decimal(str(round(predicted_close, 4))),
                predictedPriceLower=Decimal(str(round(predicted_close * 0.95, 4))),
                predictedPriceUpper=Decimal(str(round(predicted_close * 1.05, 4))),
                expectedReturn=Decimal(str(round(expected_return, 4))),
                confidence=Decimal("0.75"),
                volatility=Decimal("2.5"),
                direction=direction,
                modelType=request.modelType or "lstm",
                predictedAt=datetime.now(),
                predictionMinutes=request.predictionMinutes,
            )
    return _mock_response(request)


@router.post("/predict/batch", response_model=List[PredictionResponse])
async def predict_batch(requests: List[PredictionRequest]):
    """
    배치 예측. 각 요청에 series가 있으면 LSTM 추론 적용.
    """
    results = []
    for request in requests:
        try:
            prediction = await predict_price(request)
            results.append(prediction)
        except Exception as e:
            print(f"예측 실패: {request.symbol}, error: {str(e)}")
            continue
    return results
