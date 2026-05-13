package com.nz.admin.framework.log.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 操作日志配置。
 */
@Data
@ConfigurationProperties(prefix = "nz.log")
public class OperationLogProperties {

    private List<String> sensitiveFields = new ArrayList<>(List.of("password", "pwd", "token", "secret"));
}
