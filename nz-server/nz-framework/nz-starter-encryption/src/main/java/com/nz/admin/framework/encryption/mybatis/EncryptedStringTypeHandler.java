package com.nz.admin.framework.encryption.mybatis;

import com.nz.admin.framework.encryption.core.FieldCipherHolder;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 仅用于显式标记字段的字符串加解密处理器。
 */
public class EncryptedStringTypeHandler extends BaseTypeHandler<String> {

    @Override
    public void setNonNullParameter(PreparedStatement statement, int index, String parameter, JdbcType jdbcType)
            throws SQLException {
        statement.setString(index, FieldCipherHolder.get().encrypt(parameter));
    }

    @Override
    public String getNullableResult(ResultSet resultSet, String columnName) throws SQLException {
        return decrypt(resultSet.getString(columnName));
    }

    @Override
    public String getNullableResult(ResultSet resultSet, int columnIndex) throws SQLException {
        return decrypt(resultSet.getString(columnIndex));
    }

    @Override
    public String getNullableResult(CallableStatement statement, int columnIndex) throws SQLException {
        return decrypt(statement.getString(columnIndex));
    }

    private String decrypt(String value) {
        return value == null ? null : FieldCipherHolder.get().decrypt(value);
    }
}
