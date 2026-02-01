-- V11 롤백: TB_NEWS_ITEMS EVENT_TYPE 컬럼 길이 복원 (500 → 50)
-- 실행 전 DB 백업 권장.
-- 주의: 이미 50자 초과 데이터가 들어간 경우 롤백 시 오류 발생 가능. 해당 행 정리 후 실행하거나 백업에서 복원하세요.

ALTER TABLE TB_NEWS_ITEMS
    MODIFY COLUMN EVENT_TYPE VARCHAR(50) NULL COMMENT '이벤트 유형';
