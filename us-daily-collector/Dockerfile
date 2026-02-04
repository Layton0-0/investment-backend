# US 일봉 수집(yfinance) 전용 서비스. Spring이 HTTP로 호출.
FROM python:3.11-slim

WORKDIR /app

COPY us-daily-collector/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY scripts/us_daily_collector.py /app/collector.py
COPY us-daily-collector/app.py .

EXPOSE 8001
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8001"]
