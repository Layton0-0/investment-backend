#!/usr/bin/env python3
"""
LSTM 학습용 일별 OHLCV 수집 (yfinance).
심볼·기간 지정 시 CSV로 저장. Spring DB 없이 파일 기반.

사용:
  python scripts/fetch_training_data.py --symbols AAPL,MSFT,GOOGL --start 2020-01-01 --end 2025-01-01 --output data/train_ohlcv.csv

필요: pip install yfinance pandas
"""
import argparse
import csv
import sys
from datetime import datetime, timedelta
from pathlib import Path


def fetch_ohlcv(symbol: str, start: str, end: str):
    """yfinance로 기간 내 일별 OHLCV 조회."""
    try:
        import yfinance as yf
    except ImportError:
        raise RuntimeError("yfinance 미설치: pip install yfinance")

    start_d = datetime.strptime(start, "%Y-%m-%d").date()
    end_d = datetime.strptime(end, "%Y-%m-%d").date()
    ticker = yf.Ticker(symbol)
    hist = ticker.history(start=start_d, end=end_d + timedelta(days=1), auto_adjust=False)
    if hist is None or hist.empty:
        return []
    rows = []
    for idx, row in hist.iterrows():
        d = idx.date() if hasattr(idx, "date") else idx
        o = row.get("Open")
        h = row.get("High")
        l_ = row.get("Low")
        c = row.get("Close")
        v = row.get("Volume", 0)
        if c is None or (hasattr(c, "item") and str(c) == "nan"):
            continue
        try:
            rows.append({
                "date": d.isoformat(),
                "symbol": symbol,
                "open": float(o) if o is not None else float(c),
                "high": float(h) if h is not None else float(c),
                "low": float(l_) if l_ is not None else float(c),
                "close": float(c),
                "volume": int(v) if v is not None else 0,
            })
        except (TypeError, ValueError):
            continue
    return rows


def main():
    parser = argparse.ArgumentParser(description="LSTM 학습용 OHLCV CSV 수집")
    parser.add_argument("--symbols", type=str, required=True, help="쉼표 구분 심볼 (예: AAPL,MSFT)")
    parser.add_argument("--start", type=str, required=True, help="시작일 YYYY-MM-DD")
    parser.add_argument("--end", type=str, required=True, help="종료일 YYYY-MM-DD")
    parser.add_argument("--output", type=str, required=True, help="출력 CSV 경로")
    args = parser.parse_args()

    symbols = [s.strip() for s in args.symbols.split(",") if s.strip()]
    if not symbols:
        print("--symbols가 비어 있습니다.", file=sys.stderr)
        sys.exit(1)

    Path(args.output).parent.mkdir(parents=True, exist_ok=True)
    fieldnames = ["date", "symbol", "open", "high", "low", "close", "volume"]
    total = 0
    with open(args.output, "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=fieldnames)
        w.writeheader()
        for sym in symbols:
            rows = fetch_ohlcv(sym, args.start, args.end)
            w.writerows(rows)
            total += len(rows)
    print(f"저장: {args.output}, 행 수: {total}")


if __name__ == "__main__":
    main()
