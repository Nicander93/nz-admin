# 后端规范

Controller 只处理请求、权限、校验和 DTO/VO 转换；Service 承载规则；Entity/DO 不直接返回。优先使用已有 Hutool、Spring、MyBatis-Plus。Test/IT 镜像生产包，新模块同步 SQL、权限和测试。

