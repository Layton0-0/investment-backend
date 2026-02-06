-- V21 created tables BATCH_*_SEQ; V22 may have replaced them with SEQUENCEs.
-- Drop table or sequence by type so this migration is safe in either state.

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = 'batch_step_execution_seq') THEN
    DROP TABLE BATCH_STEP_EXECUTION_SEQ;
  ELSIF EXISTS (SELECT 1 FROM information_schema.sequences WHERE sequence_schema = current_schema() AND sequence_name = 'batch_step_execution_seq') THEN
    DROP SEQUENCE BATCH_STEP_EXECUTION_SEQ;
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = 'batch_job_execution_seq') THEN
    DROP TABLE BATCH_JOB_EXECUTION_SEQ;
  ELSIF EXISTS (SELECT 1 FROM information_schema.sequences WHERE sequence_schema = current_schema() AND sequence_name = 'batch_job_execution_seq') THEN
    DROP SEQUENCE BATCH_JOB_EXECUTION_SEQ;
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = 'batch_job_instance_seq') THEN
    DROP TABLE BATCH_JOB_INSTANCE_SEQ;
  ELSIF EXISTS (SELECT 1 FROM information_schema.sequences WHERE sequence_schema = current_schema() AND sequence_name = 'batch_job_instance_seq') THEN
    DROP SEQUENCE BATCH_JOB_INSTANCE_SEQ;
  END IF;
END $$;

CREATE SEQUENCE IF NOT EXISTS BATCH_JOB_SEQ
    START WITH 1 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1 NO CYCLE;
CREATE SEQUENCE IF NOT EXISTS BATCH_JOB_EXECUTION_SEQ
    START WITH 1 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1 NO CYCLE;
CREATE SEQUENCE IF NOT EXISTS BATCH_STEP_EXECUTION_SEQ
    START WITH 1 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1 NO CYCLE;
