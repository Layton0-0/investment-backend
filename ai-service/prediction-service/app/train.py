"""
LSTM 학습 진입점.
전처리 → DataLoader → 학습 루프 → best 모델 저장.

실행 (프로젝트 루트 또는 ai-service/prediction-service):
  python -m app.train --data-csv path/to/ohlcv.csv --lookback 30 --epochs 10 --output-dir ./models
  python -m app.train --data-csv path/to/ohlcv.csv --symbol AAPL --lookback 30 --epochs 10 --output-dir ./models
"""
import argparse
import sys
from pathlib import Path

# app 패키지 기준으로 실행되도록 루트를 prediction-service로
if __name__ == "__main__":
    _root = Path(__file__).resolve().parent
    if str(_root.parent) not in sys.path:
        sys.path.insert(0, str(_root.parent))

import numpy as np
import torch
from torch.utils.data import DataLoader, random_split

from app.data.loader import load_ohlcv_csv, SeriesDataset
from app.preprocessing.transform import build_sequences
from app.models.lstm_model import LSTMPredictor


def run(
    data_csv: str,
    lookback: int = 30,
    epochs: int = 10,
    output_dir: str = "./models",
    symbol: str = None,
    batch_size: int = 32,
    hidden_size: int = 64,
    num_layers: int = 2,
    lr: float = 1e-3,
    use_ohlcv: bool = True,
    val_ratio: float = 0.1,
    device: str = None,
):
    device = device or ("cuda" if torch.cuda.is_available() else "cpu")
    device = torch.device(device)
    Path(output_dir).mkdir(parents=True, exist_ok=True)

    df = load_ohlcv_csv(data_csv, symbol_filter=symbol)
    if df.empty or len(df) < lookback + 10:
        raise ValueError(f"데이터 부족: len={len(df)}, lookback={lookback}")

    close = df["close"].values
    open_ = df["open"].values
    high = df["high"].values
    low = df["low"].values
    volume = df["volume"].values

    sequences, targets, min_c, max_c = build_sequences(
        close, open_=open_, high=high, low=low, volume=volume,
        lookback=lookback, target_next_close=True, use_ohlcv=use_ohlcv, normalize=True,
    )
    if len(sequences) == 0:
        raise ValueError("시퀀스가 0개입니다.")

    dataset = SeriesDataset(sequences, targets)
    n = len(dataset)
    val_size = max(1, int(n * val_ratio))
    train_size = n - val_size
    train_ds, val_ds = random_split(dataset, [train_size, val_size])

    train_loader = DataLoader(train_ds, batch_size=batch_size, shuffle=True)
    val_loader = DataLoader(val_ds, batch_size=batch_size)

    input_size = 5 if use_ohlcv else 1
    model = LSTMPredictor(
        input_size=input_size,
        hidden_size=hidden_size,
        num_layers=num_layers,
        dropout=0.1,
        predict_return=False,
    ).to(device)
    criterion = torch.nn.MSELoss()
    optimizer = torch.optim.Adam(model.parameters(), lr=lr)

    best_val_loss = float("inf")
    for epoch in range(epochs):
        model.train()
        train_loss = 0.0
        for xb, yb in train_loader:
            xb, yb = xb.to(device), yb.to(device).unsqueeze(1)
            optimizer.zero_grad()
            out = model(xb)
            loss = criterion(out, yb)
            loss.backward()
            optimizer.step()
            train_loss += loss.item()
        train_loss /= len(train_loader)

        model.eval()
        val_loss = 0.0
        with torch.no_grad():
            for xb, yb in val_loader:
                xb, yb = xb.to(device), yb.to(device).unsqueeze(1)
                out = model(xb)
                val_loss += criterion(out, yb).item()
        val_loss /= len(val_loader)

        if val_loss < best_val_loss:
            best_val_loss = val_loss
            out_path = Path(output_dir) / "lstm_best.pt"
            torch.save(model.state_dict(), out_path)

        if (epoch + 1) % 5 == 0 or epoch == 0:
            print(f"epoch {epoch+1}/{epochs} train_loss={train_loss:.6f} val_loss={val_loss:.6f}")

    out_path = Path(output_dir) / "lstm_best.pt"
    print(f"저장: {out_path}")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--data-csv", type=str, required=True)
    parser.add_argument("--lookback", type=int, default=30)
    parser.add_argument("--epochs", type=int, default=10)
    parser.add_argument("--output-dir", type=str, default="./models")
    parser.add_argument("--symbol", type=str, default=None)
    parser.add_argument("--batch-size", type=int, default=32)
    parser.add_argument("--hidden-size", type=int, default=64)
    parser.add_argument("--num-layers", type=int, default=2)
    parser.add_argument("--lr", type=float, default=1e-3)
    parser.add_argument("--no-ohlcv", action="store_true", help="close만 사용")
    parser.add_argument("--val-ratio", type=float, default=0.1)
    parser.add_argument("--device", type=str, default=None)
    args = parser.parse_args()
    run(
        data_csv=args.data_csv,
        lookback=args.lookback,
        epochs=args.epochs,
        output_dir=args.output_dir,
        symbol=args.symbol,
        batch_size=args.batch_size,
        hidden_size=args.hidden_size,
        num_layers=args.num_layers,
        lr=args.lr,
        use_ohlcv=not args.no_ohlcv,
        val_ratio=args.val_ratio,
        device=args.device,
    )


if __name__ == "__main__":
    main()
