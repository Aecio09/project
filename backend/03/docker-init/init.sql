-- Script executado quando o MySQL é inicializado
-- Garante que o usuário pode conectar de qualquer host

CREATE DATABASE IF NOT EXISTS projectdb;

-- Cria usuário com acesso de qualquer host
CREATE USER IF NOT EXISTS 'projectuser'@'%' IDENTIFIED BY 'projectpass';
GRANT ALL PRIVILEGES ON projectdb.* TO 'projectuser'@'%';

-- Garante que root também pode conectar de qualquer host
CREATE USER IF NOT EXISTS 'root'@'%' IDENTIFIED BY 'rootpassword';
GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' WITH GRANT OPTION;

FLUSH PRIVILEGES;

USE projectdb;
