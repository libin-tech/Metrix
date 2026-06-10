-- 默认角色
INSERT INTO system_role (id, role_code, role_name, description, is_system, sort_order)
VALUES (1, 'ADMIN', '超级管理员', '系统超级管理员，拥有所有权限', TRUE, 1);

INSERT INTO system_role (id, role_code, role_name, description, is_system, sort_order)
VALUES (2,'USER', '普通用户', '普通用户', TRUE, 2);


-- 默认用户（管理员）密码MD5（admin@2026）
INSERT INTO users (id, username, password, email, role, is_active, create_time, update_time,
                   version, creator, modifier, nickname, avatar, openid, privacy_agreed,
                   privacy_agreed_time, status, freeze_reason)
VALUES (1, 'admin', 'dffd3d7872472b8fce1750da3f1e3cbd',
        'admin@example.com', 'ADMIN', true,
        now(), now(),
        0, 'SYSTEM', 'SYSTEM', null, null, null,
        false, null, 'NORMAL', null);

-- 默认用户角色
INSERT INTO system_user_role (id, user_id, role_id, create_time, update_time, version, creator, modifier)
VALUES (1, 1, 1, now(), now(), 0, 'SYSTEM', 'SYSTEM');
