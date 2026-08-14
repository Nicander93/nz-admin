package com.nz.admin.framework.encryption.mybatis;

import com.nz.admin.framework.encryption.config.FieldEncryptionProperties;
import com.nz.admin.framework.encryption.core.AesGcmFieldCipher;
import com.nz.admin.framework.encryption.core.FieldCipherHolder;
import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EncryptedStringTypeHandlerTest {

    private AesGcmFieldCipher cipher;

    @BeforeEach
    void setUp() {
        FieldEncryptionProperties properties = new FieldEncryptionProperties();
        properties.setEnabled(true);
        properties.setActiveKeyId("v1");
        properties.setKeys(new LinkedHashMap<>(Map.of(
                "v1", "test-field-key-material-v1"
        )));
        cipher = new AesGcmFieldCipher(properties);
        FieldCipherHolder.install(cipher);
    }

    @AfterEach
    void tearDown() {
        FieldCipherHolder.clear(cipher);
    }

    @Test
    void shouldEncryptParameterAndDecryptResult() throws Exception {
        EncryptedStringTypeHandler handler = new EncryptedStringTypeHandler();
        PreparedStatement statement = mock(PreparedStatement.class);

        handler.setNonNullParameter(statement, 1, "13800138000", JdbcType.VARCHAR);
        verify(statement).setString(eq(1), argThat(value -> value.startsWith("ENC$1$v1$")));

        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString("phone")).thenReturn(cipher.encrypt("13800138000"));
        assertThat(handler.getNullableResult(resultSet, "phone")).isEqualTo("13800138000");
    }
}
