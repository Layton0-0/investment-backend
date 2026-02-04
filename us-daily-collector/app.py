"""
US 일봉 수집 HTTP API (yfinance).
Spring UsMarketCollectionService에서 이 서비스를 호출해 JSON을 받아 TB_DAILY_STOCK 저장.
Docker Compose로 기동 시 Python/yfinance 환경을 컨테이너로 고정.
"""
import json
import subprocess
import sys
from pathlib import Path

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

app = FastAPI(title="US Daily Collector", version="1.0")

# 스크립트 경로 (이미지 내 /app/collector.py)
COLLECTOR_SCRIPT = Path("/app/collector.py")


class UsDailyRequest(BaseModel):
    bas_dt: str  # YYYY-MM-DD
    symbols: list[str]  # ["SPY", "TLT", ...]


def run_collector(bas_dt: str, symbols: list[str]) -> list[dict]:
    if not COLLECTOR_SCRIPT.exists():
        raise RuntimeError("collector.py not found in /app")
    symbols_str = ",".join(s for s in symbols if s and str(s).strip())
    if not symbols_str:
        return []
    try:
        proc = subprocess.run(
            [sys.executable, str(COLLECTOR_SCRIPT), "--bas-dt", bas_dt, "--symbols", symbols_str],
            capture_output=True,
            text=True,
            timeout=120,
            cwd="/app",
        )
        if proc.returncode != 0:
            return []
        out = (proc.stdout or "").strip()
        if not out:
            return []
        return json.loads(out)
    except (json.JSONDecodeError, subprocess.TimeoutExpired, FileNotFoundError) as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/us-daily")
def us_daily(req: UsDailyRequest):
    """기준일 US 종목 OHLCV 수집. Spring이 이 JSON 배열을 파싱해 DB 저장."""
    rows = run_collector(req.bas_dt, req.symbols)
    return rows
