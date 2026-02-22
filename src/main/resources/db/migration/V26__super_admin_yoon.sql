-- Designate existing account 'yoon' as main super administrator (Admin).
-- This account is the fixed super-admin; role is persisted in DB.
-- No-op when TB_USERS does not exist (e.g. before V25_1) or when 'yoon' does not exist.
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = 'tb_users') THEN
    UPDATE TB_USERS SET ROLE = 'Admin' WHERE USERNAME = 'yoon';
  END IF;
END $$;
