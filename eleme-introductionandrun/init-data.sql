SET NAMES utf8mb4;

INSERT INTO takeoutweb.manager (name, password) VALUES ('admin', 'admin');

INSERT INTO takeoutweb.business (account, password, name, address, phone, deactivate, check_pass) 
VALUES ('developer', 'developer', '测试商家', '测试地址', '13800138000', 0, 1);
