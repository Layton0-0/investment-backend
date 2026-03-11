-- 서버 전역 설정 (키-값). DB 우선, 없으면 application.yml fallback.
-- PostgreSQL: unquoted identifiers → lowercase (호환: 기존 JPA/다른 마이그레이션으로 생성된 테이블과 동일).
CREATE TABLE IF NOT EXISTS tb_system_settings (
    key         VARCHAR(100) NOT NULL PRIMARY KEY,
    value       TEXT,
    description VARCHAR(500),
    updated_at  TIMESTAMP(6),
    updated_by  VARCHAR(36)
);
COMMENT ON TABLE tb_system_settings IS '서버 전역 설정. pipeline.autoExecute, pipeline.allowRealExecution, marketData.websocketEnabled 등.';

-- 실계좌 제외, 체결·웹소켓 "실행" 기본값 (INSERT ON CONFLICT DO NOTHING으로 기존 행 유지)
INSERT INTO tb_system_settings (key, value, description, updated_at, updated_by)
VALUES
  ('pipeline.autoExecute', 'true', '파이프라인 자동 실행(서버 기본)', CURRENT_TIMESTAMP, 'migration'),
  ('pipeline.allowRealExecution', 'false', '실계좌 자동 실행 허용(서버 기본). false=실계좌 제외', CURRENT_TIMESTAMP, 'migration'),
  ('marketData.websocketEnabled', 'true', 'WebSocket 실시간 연결 사용 여부', CURRENT_TIMESTAMP, 'migration')
ON CONFLICT (key) DO NOTHING;
