package com.nz.admin.modules.system.service.file;

import java.util.Map;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.nz.admin.common.core.BusinessException;
import com.nz.admin.framework.file.FileConfigSecretCodec;
import com.nz.admin.framework.file.FileStorageProperties;
import com.nz.admin.framework.file.FileStorageService;
import com.nz.admin.framework.test.core.ut.BaseMockitoUnitTest;
import com.nz.admin.modules.system.entity.dataobject.file.FileConfigDO;
import com.nz.admin.modules.system.entity.dto.file.FileConfigSaveRequest;
import com.nz.admin.modules.system.mapper.file.FileConfigMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileConfigServiceImplTest extends BaseMockitoUnitTest {

    @Mock
    private FileConfigMapper mapper;
    @Mock
    private FileStorageService localStorageService;
    @Mock
    private FileStorageService ossStorageService;
    @Mock
    private FileStorageService s3StorageService;

    private FileStorageProperties properties;
    private FileConfigSecretCodec codec;
    private FileConfigServiceImpl service;

    @BeforeEach
    void setUpService() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), FileConfigDO.class);
        properties = new FileStorageProperties();
        properties.setConfigEncryptionKey("file-config-unit-test-key");
        codec = new FileConfigSecretCodec(properties);
        service = new FileConfigServiceImpl(properties, codec, Map.of(
                "localFileStorageService", localStorageService,
                "ossFileStorageService", ossStorageService,
                "s3FileStorageService", s3StorageService
        ));
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
    }

    @Test
    void createEncryptsSecretBeforePersistence() {
        FileConfigSaveRequest request = ossRequest();
        when(mapper.insert(any(FileConfigDO.class))).thenAnswer(invocation -> {
            invocation.<FileConfigDO>getArgument(0).setId(9L);
            return 1;
        });

        assertThat(service.create(request)).isEqualTo(9L);

        ArgumentCaptor<FileConfigDO> captor = ArgumentCaptor.forClass(FileConfigDO.class);
        verify(mapper).insert(captor.capture());
        assertThat(captor.getValue().getAccessKeySecret()).isNotEqualTo("plain-secret");
        assertThat(codec.decrypt(captor.getValue().getAccessKeySecret())).isEqualTo("plain-secret");
        assertThat(captor.getValue().getStatus()).isEqualTo(1);
    }

    @Test
    void getMasksCredentials() {
        FileConfigDO config = ossConfig();
        when(mapper.selectById(1L)).thenReturn(config);

        var result = service.get(1L);

        assertThat(result.getAccessKeyIdMasked()).isEqualTo("abc***xyz");
        assertThat(result.isAccessKeySecretConfigured()).isTrue();
    }

    @Test
    void updateKeepsCredentialsWhenRequestLeavesThemBlank() {
        FileConfigDO current = ossConfig();
        when(mapper.selectById(1L)).thenReturn(current);
        FileConfigSaveRequest request = ossRequest();
        request.setId(1L);
        request.setAccessKeyId("");
        request.setAccessKeySecret("");

        service.update(request);

        ArgumentCaptor<FileConfigDO> captor = ArgumentCaptor.forClass(FileConfigDO.class);
        verify(mapper).updateById(captor.capture());
        assertThat(captor.getValue().getAccessKeyId()).isEqualTo("abcdefxyz");
        assertThat(codec.decrypt(captor.getValue().getAccessKeySecret())).isEqualTo("plain-secret");
    }
    @Test
    void activateAppliesLocalConfigImmediately() {
        FileConfigDO config = new FileConfigDO()
                .setId(2L).setConfigName("local").setStorageType("local")
                .setBasePath("/data/nz").setLocalAccessUrlPrefix("/files/")
                .setMaxFileSizeBytes(2048L).setStatus(1);
        when(mapper.selectById(2L)).thenReturn(config);

        service.activate(2L);

        assertThat(properties.getStorageType()).isEqualTo("local");
        assertThat(properties.getBasePath()).isEqualTo("/data/nz");
        assertThat(properties.getMaxFileSizeBytes()).isEqualTo(2048L);
        verify(mapper).updateById(any(FileConfigDO.class));
    }

    @Test
    void deleteRejectsActiveConfig() {
        when(mapper.selectById(3L)).thenReturn(new FileConfigDO().setId(3L).setStatus(0));
        assertThatThrownBy(() -> service.delete(3L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能删除");
    }


    @Test
    void activateAppliesS3ConfigImmediately() {
        FileConfigDO config = new FileConfigDO()
                .setId(4L).setConfigName("minio").setStorageType("s3")
                .setEndpoint("http://minio:9000").setAccessKeyId("nzadmin")
                .setAccessKeySecret(codec.encrypt("minio-secret")).setBucketName("nz-admin")
                .setRegion("us-east-1").setMaxFileSizeBytes(4096L).setStatus(1);
        when(mapper.selectById(4L)).thenReturn(config);

        service.activate(4L);

        assertThat(properties.getStorageType()).isEqualTo("s3");
        assertThat(properties.getS3().getEndpoint()).isEqualTo("http://minio:9000");
        assertThat(properties.getS3().getAccessKeySecret()).isEqualTo("minio-secret");
        assertThat(properties.getS3().getBucketName()).isEqualTo("nz-admin");
    }

    @Test
    void testConnectionUsesActiveStorageOnly() {
        FileConfigDO config = new FileConfigDO()
                .setId(5L).setConfigName("local").setStorageType("local")
                .setBasePath("/data/nz").setMaxFileSizeBytes(1024L).setStatus(0);
        when(mapper.selectById(5L)).thenReturn(config);

        service.testConnection(5L);

        verify(localStorageService).checkAvailable();
    }

    @Test
    void testConnectionRejectsInactiveConfig() {
        when(mapper.selectById(6L)).thenReturn(new FileConfigDO().setId(6L).setStatus(1));
        assertThatThrownBy(() -> service.testConnection(6L)).isInstanceOf(BusinessException.class);
    }
    private FileConfigSaveRequest ossRequest() {
        FileConfigSaveRequest request = new FileConfigSaveRequest();
        request.setConfigName("oss");
        request.setStorageType("oss");
        request.setEndpoint("https://oss.example.com");
        request.setAccessKeyId("abcdefxyz");
        request.setAccessKeySecret("plain-secret");
        request.setBucketName("bucket");
        request.setMaxFileSizeBytes(1024L);
        return request;
    }

    private FileConfigDO ossConfig() {
        return new FileConfigDO()
                .setId(1L).setConfigName("oss").setStorageType("oss")
                .setAccessKeyId("abcdefxyz").setAccessKeySecret(codec.encrypt("plain-secret"))
                .setMaxFileSizeBytes(1024L).setStatus(1);
    }
}
