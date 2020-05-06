CREATE DATABASE delilah;
\connect delilah;
CREATE USER delilah PASSWORD 'delilah-test';
GRANT ALL PRIVILEGES ON DATABASE delilah TO delilah;
