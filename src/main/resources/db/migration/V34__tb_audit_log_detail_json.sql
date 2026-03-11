-- P6-3 트레이드 저널: 매매 결정 상세(JSON) 저장용 컬럼.
-- TRADE_DECISION 이벤트 시 action, symbol, market, strategyType 등이 JSON으로 저장됨.
ALTER TABLE TB_AUDIT_LOG ADD COLUMN IF NOT EXISTS DETAIL_JSON TEXT;
COMMENT ON COLUMN TB_AUDIT_LOG.DETAIL_JSON IS '트레이드 결정 등 상세 데이터 (JSON). EVENT_TYPE=TRADE_DECISION 시 사용';
