-- 거래 사유(Trade Reason) 추적: 진입 시그널·청산 규칙 저장.
-- TB_ORDERS: 파이프라인 매수 시 진입 시그널(signal_type), 매도 시 청산 규칙(exit_rule_type).
-- TB_STRATEGY_POSITION: 포지션 진입 시그널(signal_type), 청산 시 규칙(exit_rule_type).
-- No-op when table does not exist (e.g. fresh DB before V29_1); safe for baseline-on-migrate.

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = 'tb_orders') THEN
    ALTER TABLE TB_ORDERS ADD COLUMN IF NOT EXISTS SIGNAL_TYPE VARCHAR(64);
    ALTER TABLE TB_ORDERS ADD COLUMN IF NOT EXISTS EXIT_RULE_TYPE VARCHAR(64);
    COMMENT ON COLUMN TB_ORDERS.SIGNAL_TYPE IS '진입 시그널 유형 (예: VOLATILITY_BREAKOUT, DUAL_MOMENTUM). 파이프라인 매수 시 설정';
    COMMENT ON COLUMN TB_ORDERS.EXIT_RULE_TYPE IS '청산 규칙 유형 (예: ATR_TRAILING_STOP, TIME_CUT, STOP_LOSS). 파이프라인 매도 시 설정';
  END IF;
END $$;

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = 'tb_strategy_position') THEN
    ALTER TABLE TB_STRATEGY_POSITION ADD COLUMN IF NOT EXISTS SIGNAL_TYPE VARCHAR(64);
    ALTER TABLE TB_STRATEGY_POSITION ADD COLUMN IF NOT EXISTS EXIT_RULE_TYPE VARCHAR(64);
    COMMENT ON COLUMN TB_STRATEGY_POSITION.SIGNAL_TYPE IS '진입 시그널 유형. 포지션 생성 시 설정';
    COMMENT ON COLUMN TB_STRATEGY_POSITION.EXIT_RULE_TYPE IS '청산 규칙 유형. 포지션 청산 시 설정';
  END IF;
END $$;
