package com.nz.admin.framework.tenant.core;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;

import java.sql.SQLException;
import java.util.Set;
import java.util.TreeSet;

import javax.sql.DataSource;

/** 检查当前 schema 的租户表，避免新表遗漏隔离注册。 */
public class TenantTableAudit {
    private final DataSource source;
    private final TenantLineHandler handler;
    private final Set<String> externallyManaged;

    public TenantTableAudit(
            DataSource source, TenantLineHandler handler, Set<String> externallyManaged) {
        this.source = source;
        this.handler = handler;
        this.externallyManaged = externallyManaged;
    }

    public void verify() throws SQLException {
        var missing = new TreeSet<String>();
        try (var connection = source.getConnection();
                var columns =
                        connection
                                .getMetaData()
                                .getColumns(
                                        connection.getCatalog(),
                                        connection.getSchema(),
                                        "%",
                                        handler.getTenantIdColumn())) {
            while (columns.next()) {
                String table = columns.getString("TABLE_NAME");
                if (handler.ignoreTable(table) && !externallyManaged.contains(table))
                    missing.add(table);
            }
        }
        if (!missing.isEmpty()) throw new IllegalStateException("租户表未注册隔离规则: " + missing);
    }
}
