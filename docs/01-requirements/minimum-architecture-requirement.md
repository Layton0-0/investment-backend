This design document outlines a **Quantitative Trading System (QTS)** tailored for a Korean individual investor, applying institutional-grade logic within a lean, low-cost infrastructure.

The core premise is **Asymmetric Sophistication**: The intellectual capital (strategy, risk, portfolio construction) rivals a hedge fund, while the physical implementation (servers, code complexity) remains strictly utilitarian and budget-conscious.

---

### 1. Architectural Philosophy

* **Logic:** **Institutional.** We do not trade "tickers"; we trade factor exposures and risk premia. We optimize for risk-adjusted returns (Sharpe/Sortino), not raw PnL.
* **Infrastructure:** **Monolithic & Robust.** Microservices are a liability for a single developer. We use a modular monolith deployed via Docker.
* **Execution:** **Defensive.** We assume the API will fail, the internet will disconnect, and data will be dirty.
* **Tax-Aware Alpha:** For a Korean investor, Alpha is calculated *after* the 0.20% (approx) domestic transaction tax and the 22% overseas capital gains tax.

---

### 2. Infrastructure Architecture (Low Budget / High Reliability)

We avoid Kubernetes and distributed clusters. We utilize a **"Fat VPS"** approach.

* **Compute:** Single VPS (e.g., AWS Lightsail, Vultr, or cleanup desktop at home with static IP).
* *Spec:* 4 vCPU, 8GB RAM minimum (for batch processing).
* *OS:* Ubuntu LTS.


* **Containerization:** Docker Compose.
* **Database:** PostgreSQL with **TimescaleDB** extension.
* *Reasoning:* Relational integrity is non-negotiable for financial ledgers. Timescale handles time-series market data efficiently on a single node.


* **Job Scheduling:** `Cron` or `Systemd` timers.
* *Reasoning:* Airflow/Celery adds overhead. Python scripts triggered by OS-level cron are easier to debug and recover.


* **Deployment:** GitHub Actions (Free tier)  SSH pull & restart.

**The "Offline" Compromise:**
Due to budget, we do **not** run real-time tick processing or HFT. We operate on **1-minute** or **Daily (EOD)** bars. ML training happens on your local machine (GPU), and only the lightweight *inference* model is deployed to the VPS.

---

### 3. Backend Architecture (The Core)

**Language:** Python 3.11+ (Type hinting is mandatory).
**Framework:** FastAPI (High performance, auto-doc).

#### **Module 1: The Gateway (KIS Adapter)**

This wraps the Korea Investment & Securities Open API.

* **Token Manager:** Auto-refreshes OAuth tokens.
* **Rate Limiter:** Hard-coded limits (e.g., 20 requests/sec) to prevent IP bans.
* **Normalization:** Converts KIS specific distinct JSON structures (Domestic vs. Overseas) into a unified internal `Order` and `Quote` object.

#### **Module 2: The Data Engine**

* **Ingestion:** Fetches OHLCV data daily.
* **Adjuster:** *Crucial.* Institutional logic requires adjusting for splits and dividends. Raw prices destroy backtests.
* **Feature Store:** Pre-calculates technical indicators (RSI, Bollinger) and factor exposures (Momentum, Value) and stores them in SQL to avoid re-calculation at runtime.

#### **Module 3: The Brain (Strategy & Portfolio)**

* **Signal Generator:** Pure functions taking Data  Signal (-1 to 1).
* **Optimizer:** Takes Signals + Risk Model  Target Portfolio Weights.
* *Math:* Mean-Variance Optimization (Markowitz) or Equal Risk Contribution (ERC).


* **Rebalancer:** Calculates the difference between `Current Holdings` and `Target Weights`, generating a list of "diff" orders.

#### **Module 4: The Risk Guard (The "Compliance Officer")**

This module sits between the *Brain* and the *Gateway*. It rejects orders if:

* Gross Leverage > 1.0 (No margin).
* Single Position > 10% of NAV.
* Correlated Sector Exposure > 30%.
* **Kill Switch:** If activated, cancels all open orders and (optionally) liquidates to cash.

---

### 4. Quant & Strategy Engine (Institutional Logic)

We implement a **Factor-Based Investing** approach.

#### **Supported Strategies**

1. **Cross-Sectional Momentum (Global):**
* Buy top N assets with best 6-12 month returns, sell bottom N.
* *Korean Nuance:* High turnover hurts Domestic returns due to transaction tax. Apply longer lookback periods for KR stocks.


2. **Mean Reversion (Bollinger/RSI):**
* Tactical entry/exit for stable large-caps (Samsung Electronics, SK Hynix).


3. **Asset Allocation (Risk Parity):**
* Balance risk between SPY (US Equity), KOSPI (KR Equity), and TLT (US Treasuries).



#### **Tax & Currency Logic**

* **Domestic (KR):**
* Cost: ~0.20% Transaction Tax + ~0.015% Fee.
* *Rule:* Minimum expected Alpha per trade must exceed 0.50% to be viable.


* **Overseas (US):**
* Cost: ~0.10% FX Spread + Fee.
* Tax: 22% on gains > 2.5M KRW.
* *Rule:* **Tax Loss Harvesting.** In December, the algorithm must identify losing positions to realize losses, offsetting gains to minimize the 22% tax bill.



#### **Performance Metrics**


We optimize for **Calmar Ratio** (Annual Return / Max Drawdown) rather than pure CAGR, prioritizing sleep-at-night stability.

---

### 5. AI / ML Layer (Lightweight & Focused)

We do not use "Black Box" end-to-end trading. We use ML for **Meta-Labeling** or **Regime Detection**.

* **Architecture:**
* **Training (Local PC):** Train Random Forest / XGBoost on historical data. Target: "Probability of profit > 0 in next 5 days."
* **Inference (VPS):** Upload the `.json` or `.joblib` model file. The VPS calculates features and runs `model.predict()`.


* **Application:**
* *Regime Filter:* If ML predicts "High Volatility / Crash," the system forces the Strategy Engine to reduce position sizing by 50%.
* *Asset Selection:* Use ML to rank stocks, but use traditional Convex Optimization to size positions.



---

### 6. Frontend (Web Dashboard)

**Tech:** React + Vite (Static build served by Nginx or FastAPI).
**Design:** "Dark Terminal" aesthetic.

**Views:**

1. **The Blotter:** Real-time table of today's executed trades.
2. **Portfolio Heatmap:** Tree map showing exposure by Sector and Country (KR/US).
3. **Risk Monitor:**
* Current Drawdown.
* VaR (Value at Risk) 95%.
* Estimated Tax Liability (Year-to-date).


4. **Control Panel:**
* **BIG RED BUTTON:** "Halt Trading."
* **Emergency Liquidate:** "Sell All."
* **Mode Toggle:** Simulation vs. Live.



---

### 7. Year-End Tax & Reporting

The system must generate a PDF/CSV report specifically for the **National Tax Service (Hometax)**.

* **Overseas Equities:**
* Calculate realized gains using FIFO (First-In-First-Out) or Moving Average cost basis (consistent with KIS logic).
* Highlight the 2.5M KRW deduction usage.


* **Domestic Equities:**
* Track "Grandfathered" status (if applicable) and dividend income (which is taxed at 15.4%).



---

### 8. Operational Reality (The "What Ifs")

* **Server Crash:** The system state is in Postgres. On restart, it checks KIS API for current positions vs. DB positions. If mismatch  Send Alert (Telegram/Slack) and Halt.
* **Data Delay:** Strategies check timestamp of data. If `Last_Price_Time < Now - 30min`, no trading occurs.
* **Manual Intervention:** The trader can log into the KIS Mobile App (MTS) to manually close positions. The system must have a "Re-sync" button to download the new reality from KIS.

---

### 9. Development Roadmap

**Phase 1: The Foundation (Months 1-2)**

* Setup KIS API wrapper.
* Build Data Ingestion (SQL).
* Implement simple "Buy & Hold" logic.
* Paper Trading only.

**Phase 2: The Quant Engine (Months 3-4)**

* Implement Backtester with transaction costs/taxes.
* Deploy Momentum Strategy.
* Build Risk Manager (Position limits).
* Go Live with small capital (e.g., 1M KRW).

**Phase 3: Intelligence (Months 5-6)**

* Add Overseas (US) support + FX handling.
* Implement Tax Harvesting logic.
* Deploy offline-trained ML regime filter.

---

### 10. Sample Code Structure (Python/FastAPI)

```python
# Project Structure
/quant_system
  /core
    strategy_engine.py   # Alpha logic
    risk_manager.py      # Limits & Kill Switch
    portfolio_optimizer.py # Weights calculation
  /data
    ingestion.py         # KIS API Data fetcher
    store.py             # Postgres Handler
  /execution
    kis_wrapper.py       # Korea Invest API Auth & Orders
    order_router.py      # Splits orders (KR vs US)
  /web
    api.py               # FastAPI endpoints
    dashboard/           # React frontend
  config.py              # Env vars, Limits
  main.py                # Entry point

```