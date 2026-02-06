-- Designate existing account 'yoon' as main super administrator (Admin).
-- This account is the fixed super-admin; role is persisted in DB.
UPDATE TB_USERS
SET ROLE = 'Admin'
WHERE USERNAME = 'yoon';
