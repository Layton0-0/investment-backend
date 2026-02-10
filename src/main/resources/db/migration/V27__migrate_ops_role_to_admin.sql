-- Migrate legacy Ops role to Admin. Role is now User or Admin only.
UPDATE TB_USERS SET ROLE = 'Admin' WHERE ROLE = 'Ops';

COMMENT ON COLUMN TB_USERS.ROLE IS '역할: User, Admin';
