-- Create Keycloak database (Keycloak creates its own schema on first start)
CREATE DATABASE IF NOT EXISTS keycloak;
GRANT ALL PRIVILEGES ON keycloak.* TO 'secureflow'@'%';
FLUSH PRIVILEGES;
