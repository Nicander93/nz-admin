package com.nz.admin.framework.mybatis.properties;

import com.baomidou.mybatisplus.annotation.DbType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MyBatis starter 配置。
 */
@Data
@ConfigurationProperties(prefix = "nz.mybatis")
public class MybatisFrameworkProperties {

    private DbType dbType = DbType.POSTGRE_SQL;
}
