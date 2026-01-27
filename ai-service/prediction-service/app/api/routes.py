"""
API 라우터
"""
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import List, Optional
from datetime import datetime
from decimal import Decimal

router = APIRouter()


class PredictionRequest(BaseModel):
    """예측 요청 DTO"""
    symbol: str
    predictionMinutes: int
    modelType: Optional[str] = "ensemble"
    lookbackDays: Optional[int] = 30
    requestedAt: Optional[datetime] = None


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
    단일 종목 가격 예측
    
    현재는 Mock 데이터를 반환합니다.
    실제 모델 구현 후 예측 로직을 추가하세요.
    """
    # TODO: 실제 모델을 사용한 예측 구현
    # 현재는 Mock 데이터 반환
    
    # Mock 예측 결과
    current_price = Decimal("100.0")
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
        predictionMinutes=request.predictionMinutes
    )


@router.post("/predict/batch", response_model=List[PredictionResponse])
async def predict_batch(requests: List[PredictionRequest]):
    """
    배치 예측
    
    여러 종목에 대한 예측을 한 번에 수행합니다.
    """
    results = []
    for request in requests:
        try:
            prediction = await predict_price(request)
            results.append(prediction)
        except Exception as e:
            # 개별 예측 실패 시 로그만 남기고 계속 진행
            print(f"예측 실패: {request.symbol}, error: {str(e)}")
            continue
    
    return results
