-- 서버 전역 설정 (키-값). DB 우선, 없으면 application.yml fallback.
CREATE TABLE IF NOT EXISTS TB_SYSTEM_SETTINGS (
    "KEY"         VARCHAR(100) NOT NULL PRIMARY KEY,
    VALUE         TEXT,
    DESCRIPTION   VARCHAR(500),
    UPDATED_AT    TIMESTAMP(6),
    UPDATED_BY    VARCHAR(36)
);
COMMENT ON TABLE TB_SYSTEM_SETTINGS IS '서버 전역 설정. pipeline.autoExecute, pipeline.allowRealExecution, marketData.websocketEnabled 등.';

-- 실계좌 제외, 체결·웹소켓 "실행" 기본값 (INSERT ON CONFLICT DO NOTHING으로 기존 행 유지)
INSERT INTO TB_SYSTEM_SETTINGS ("KEY", VALUE, DESCRIPTION, UPDATED_AT, UPDATED_BY)
VALUES
  ('pipeline.autoExecute', 'true', '파이프라인 자동 실행(서버 기본)', CURRENT_TIMESTAMP, 'migration'),
  ('pipeline.allowRealExecution', 'false', '실계좌 자동 실행 허용(서버 기본). false=실계좌 제외', CURRENT_TIMESTAMP, 'migration'),
  ('marketData.websocketEnabled', 'true', 'WebSocket 실시간 연결 사용 여부', CURRENT_TIMESTAMP, 'migration')
ON CONFLICT ("KEY") DO NOTHING;
