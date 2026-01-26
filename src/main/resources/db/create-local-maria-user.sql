-- local-maria 사용자 생성 스크립트
-- application-local.yml에 정의된 사용자 정보와 일치합니다
-- root 계정으로 실행하세요: mysql -u root -p < create-local-maria-user.sql

-- 사용자 생성 (application-local.yml의 username: local_maria)
CREATE USER IF NOT EXISTS 'local_maria'@'localhost' IDENTIFIED BY 'local_maria_pass';

-- investment 데이터베이스에 대한 모든 권한 부여
GRANT ALL PRIVILEGES ON investment.* TO 'local_maria'@'localhost';

-- 모든 데이터베이스에 대한 SELECT 권한 부여 (MCP 서버용)
GRANT SELECT ON *.* TO 'local_maria'@'localhost';

-- 권한 적용
FLUSH PRIVILEGES;

-- 생성된 사용자 확인
SELECT User, Host FROM mysql.user WHERE User = 'local_maria';
