package com.nz.admin.modules.generator.repository;

import com.nz.admin.modules.generator.model.GeneratorColumn;
import com.nz.admin.modules.generator.model.GeneratorTable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * PostgreSQL 元数据读取实现。
 */
@Repository
public class JdbcGeneratorMetadataRepository implements GeneratorMetadataRepository {

    private static final String TABLE_SQL = """
            SELECT t.table_schema,
                   t.table_name,
                   COALESCE(obj_description(
                       (quote_ident(t.table_schema) || '.' || quote_ident(t.table_name))::regclass,
                       'pg_class'
                   ), '') AS table_comment,
                   COUNT(c.column_name) AS column_count
            FROM information_schema.tables t
            LEFT JOIN information_schema.columns c
              ON c.table_schema = t.table_schema
             AND c.table_name = t.table_name
            WHERE t.table_type = 'BASE TABLE'
              AND t.table_schema = ?
              AND (? = ''
                   OR LOWER(t.table_name) LIKE LOWER(?)
                   OR LOWER(COALESCE(obj_description(
                       (quote_ident(t.table_schema) || '.' || quote_ident(t.table_name))::regclass,
                       'pg_class'
                   ), '')) LIKE LOWER(?))
            GROUP BY t.table_schema, t.table_name
            ORDER BY t.table_name
            """;

    private static final String COLUMN_SQL = """
            SELECT c.ordinal_position,
                   c.column_name,
                   COALESCE(pg_catalog.col_description(pc.oid, c.ordinal_position), '') AS column_comment,
                   c.data_type,
                   c.udt_name,
                   c.is_nullable = 'YES' AS nullable,
                   c.column_default,
                   EXISTS (
                       SELECT 1
                       FROM information_schema.table_constraints tc
                       JOIN information_schema.key_column_usage kcu
                         ON kcu.constraint_name = tc.constraint_name
                        AND kcu.table_schema = tc.table_schema
                        AND kcu.table_name = tc.table_name
                       WHERE tc.constraint_type = 'PRIMARY KEY'
                         AND tc.table_schema = c.table_schema
                         AND tc.table_name = c.table_name
                         AND kcu.column_name = c.column_name
                   ) AS primary_key,
                   c.is_identity = 'YES' AS identity
            FROM information_schema.columns c
            JOIN pg_catalog.pg_namespace pn
              ON pn.nspname = c.table_schema
            JOIN pg_catalog.pg_class pc
              ON pc.relnamespace = pn.oid
             AND pc.relname = c.table_name
            WHERE c.table_schema = ?
              AND c.table_name = ?
            ORDER BY c.ordinal_position
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcGeneratorMetadataRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<GeneratorTable> listTables(String schemaName, String keyword) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        String pattern = "%" + normalizedKeyword + "%";
        return jdbcTemplate.query(TABLE_SQL, (rs, rowNum) -> {
            GeneratorTable table = new GeneratorTable();
            table.setSchemaName(rs.getString("table_schema"));
            table.setTableName(rs.getString("table_name"));
            table.setTableComment(rs.getString("table_comment"));
            table.setColumnCount(rs.getInt("column_count"));
            return table;
        }, schemaName, normalizedKeyword, pattern, pattern);
    }

    @Override
    public List<GeneratorColumn> listColumns(String schemaName, String tableName) {
        return jdbcTemplate.query(COLUMN_SQL, (rs, rowNum) -> {
            GeneratorColumn column = new GeneratorColumn();
            column.setOrdinalPosition(rs.getInt("ordinal_position"));
            column.setColumnName(rs.getString("column_name"));
            column.setColumnComment(rs.getString("column_comment"));
            column.setDataType(rs.getString("data_type"));
            column.setUdtName(rs.getString("udt_name"));
            column.setNullable(rs.getBoolean("nullable"));
            column.setDefaultValue(rs.getString("column_default"));
            column.setPrimaryKey(rs.getBoolean("primary_key"));
            column.setIdentity(rs.getBoolean("identity"));
            return column;
        }, schemaName, tableName);
    }
}
