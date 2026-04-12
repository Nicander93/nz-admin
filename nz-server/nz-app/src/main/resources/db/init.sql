CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGSERIAL PRIMARY KEY,
    dept_id     BIGINT,
    username    VARCHAR(64)  NOT NULL UNIQUE,
    password    VARCHAR(128) NOT NULL,
    nickname    VARCHAR(64),
    email       VARCHAR(128),
    phone       VARCHAR(20),
    status      SMALLINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_role (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(64)  NOT NULL,
    role_key    VARCHAR(64)  NOT NULL UNIQUE,
    sort        INT DEFAULT 0,
    status      SMALLINT DEFAULT 0,
    remark      VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_dept (
    id          BIGSERIAL PRIMARY KEY,
    parent_id   BIGINT DEFAULT 0,
    name        VARCHAR(64)  NOT NULL,
    sort        INT DEFAULT 0,
    status      SMALLINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_menu (
    id          BIGSERIAL PRIMARY KEY,
    parent_id   BIGINT DEFAULT 0,
    name        VARCHAR(64)  NOT NULL,
    path        VARCHAR(200),
    component   VARCHAR(255),
    icon        VARCHAR(64),
    sort        INT DEFAULT 0,
    type        CHAR(1),          -- M=目录 C=菜单 F=按钮
    perm        VARCHAR(128),     -- 权限标识，如 system:user:list
    visible     SMALLINT DEFAULT 0, -- 0=显示 1=隐藏
    status      SMALLINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_dict_type (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(128) NOT NULL,
    type        VARCHAR(128) NOT NULL UNIQUE,
    status      SMALLINT DEFAULT 0,
    remark      VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_dict_data (
    id          BIGSERIAL PRIMARY KEY,
    dict_type   VARCHAR(128) NOT NULL,
    label       VARCHAR(128) NOT NULL,
    value       VARCHAR(128) NOT NULL,
    sort        INT DEFAULT 0,
    status      SMALLINT DEFAULT 0,
    remark      VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id     BIGINT NOT NULL,
    role_id     BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS sys_role_menu (
    role_id     BIGINT NOT NULL,
    menu_id     BIGINT NOT NULL,
    PRIMARY KEY (role_id, menu_id)
);
