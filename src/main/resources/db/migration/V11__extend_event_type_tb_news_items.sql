-- V11: TB_NEWS_ITEMS EVENT_TYPE 컬럼 길이 확장 (50 → 500)
-- 적용 전 DB 백업 권장. 롤백: db/migration/rollback/V11_rollback.sql 참고.
-- DART report_nm 등 긴 보고서명 저장 시 50자 초과 오류 방지.

ALTER TABLE TB_NEWS_ITEMS
    MODIFY COLUMN EVENT_TYPE VARCHAR(500) NULL COMMENT '이벤트 유형';
