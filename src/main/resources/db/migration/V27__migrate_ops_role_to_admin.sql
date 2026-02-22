-- Migrate legacy Ops role to Admin. Role is now User or Admin only.
-- No-op when TB_USERS does not exist. (Comment for ROLE is set in V25_1.)
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = 'tb_users') THEN
    UPDATE TB_USERS SET ROLE = 'Admin' WHERE ROLE = 'Ops';
  END IF;
END $$;
