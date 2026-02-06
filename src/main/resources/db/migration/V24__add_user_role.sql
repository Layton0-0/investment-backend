-- Add role column to TB_USERS for authorization (User / Ops).
-- Safe when TB_USERS is created later by Hibernate (init-db profile).

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = 'tb_users') THEN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = current_schema() AND table_name = 'tb_users' AND column_name = 'role') THEN
      ALTER TABLE TB_USERS ADD COLUMN ROLE VARCHAR(20) NOT NULL DEFAULT 'User';
      COMMENT ON COLUMN TB_USERS.ROLE IS '역할: User, Ops';
    END IF;
  END IF;
END $$;
