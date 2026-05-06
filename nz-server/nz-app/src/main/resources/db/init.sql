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

CREATE TABLE IF NOT EXISTS sys_oper_log (
    id             BIGSERIAL PRIMARY KEY,
    title          VARCHAR(128) NOT NULL,
    business_type  SMALLINT,
    oper_content   VARCHAR(255),
    method         VARCHAR(255),
    request_method VARCHAR(16),
    oper_name      VARCHAR(64),
    oper_url       VARCHAR(255),
    oper_ip        VARCHAR(128),
    user_agent     VARCHAR(500),
    oper_param     TEXT,
    json_result    TEXT,
    status         SMALLINT DEFAULT 0,
    error_msg      TEXT,
    oper_time      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_login_log (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT,
    username    VARCHAR(64),
    ip          VARCHAR(128),
    status      SMALLINT DEFAULT 0,
    msg         VARCHAR(255),
    login_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_post (
    id          BIGSERIAL PRIMARY KEY,
    post_code   VARCHAR(64)  NOT NULL UNIQUE,
    post_name   VARCHAR(64)  NOT NULL,
    sort        INT DEFAULT 0,
    status      SMALLINT DEFAULT 0,
    remark      VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_user_post (
    user_id     BIGINT NOT NULL,
    post_id     BIGINT NOT NULL,
    PRIMARY KEY (user_id, post_id)
);

CREATE TABLE IF NOT EXISTS sys_config (
    id          BIGSERIAL PRIMARY KEY,
    config_name VARCHAR(128) NOT NULL,
    config_key  VARCHAR(128) NOT NULL UNIQUE,
    config_value VARCHAR(500),
    config_type SMALLINT DEFAULT 2,
    status      SMALLINT DEFAULT 0,
    remark      VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_notice (
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(128) NOT NULL,
    content     TEXT,
    type        SMALLINT DEFAULT 1,
    status      SMALLINT DEFAULT 0,
    remark      VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_job (
    id              BIGSERIAL PRIMARY KEY,
    job_name        VARCHAR(64)  NOT NULL,
    job_group       VARCHAR(64)  DEFAULT 'DEFAULT',
    invoke_target   VARCHAR(500) NOT NULL,
    cron_expression VARCHAR(64)  NOT NULL,
    misfire_policy  SMALLINT DEFAULT 1,
    concurrent      SMALLINT DEFAULT 1,
    status          SMALLINT DEFAULT 0,
    remark          VARCHAR(500),
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_job_log (
    id          BIGSERIAL PRIMARY KEY,
    job_id      BIGINT NOT NULL,
    job_name    VARCHAR(64),
    job_group   VARCHAR(64),
    invoke_target VARCHAR(500),
    job_message VARCHAR(500),
    status      SMALLINT DEFAULT 0,
    exception_info TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_file (
    id          BIGSERIAL PRIMARY KEY,
    file_name   VARCHAR(128) NOT NULL,
    original_name VARCHAR(128),
    file_suffix VARCHAR(32),
    file_size   BIGINT,
    file_path   VARCHAR(255),
    url         VARCHAR(255),
    storage_type SMALLINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
