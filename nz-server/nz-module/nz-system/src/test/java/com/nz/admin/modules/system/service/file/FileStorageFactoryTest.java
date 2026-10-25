package com.nz.admin.modules.system.service.file;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nz.admin.framework.file.FileConfigSecretCodec;
import com.nz.admin.framework.file.FileStorageProperties;
import com.nz.admin.framework.file.FileStorageService;
import com.nz.admin.framework.file.LocalFileStorageServiceImpl;
import com.nz.admin.framework.file.OssFileStorageServiceImpl;
import com.nz.admin.framework.tenant.core.TenantContextHolder;
import com.nz.admin.modules.system.entity.dataobject.file.FileConfigDO;
import com.nz.admin.modules.system.mapper.file.FileConfigMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileStorageFactoryTest {

    private FileConfigMapper mapper;
    private FileStorageProperties defaults;
    private FileConfigSecretCodec codec;
    private FileStorageFactory factory;

    @BeforeEach
    void setUp() {
        mapper = mock(FileConfigMapper.class);
        defaults = new FileStorageProperties();
        defaults.setConfigEncryptionKey("factory-unit-test-encryption-key");
        defaults.setBasePath("/deployment/default");
        defaults.setLocalAccessUrlPrefix("/download/");
        codec = new FileConfigSecretCodec(defaults);
        factory = new FileStorageFactory(mapper, defaults, codec, factories());
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void currentSelectsOnlyTheExplicitTenantAndBuildsIsolatedAdapter() {
        FileConfigDO active = new FileConfigDO().setId(5L).setStorageType("local")
                .setBasePath("/tenant/42/files").setLocalAccessUrlPrefix("/tenant-files/")
                .setMaxFileSizeBytes(8192L).setStatus(0);
        when(mapper.selectOne(any(Wrapper.class))).thenReturn(active);

        FileStorageFactory.Binding binding = TenantContextHolder.callWithTenantId(42L, factory::current);

        assertThat(binding.storageType()).isEqualTo("local");
        assertThat(JSONUtil.parseObj(binding.snapshot()).getLong("tenantId")).isEqualTo(42L);
        FileStorageProperties bound = (FileStorageProperties) ReflectionTestUtils
                .getField(binding.storage(), "properties");
        assertThat(bound.getBasePath()).isEqualTo("/tenant/42/files");
        assertThat(bound).isNotSameAs(defaults);
        assertThat(defaults.getBasePath()).isEqualTo("/deployment/default");

        ArgumentCaptor<Wrapper<FileConfigDO>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(mapper).selectOne(captor.capture());
        QueryWrapper<?> query = (QueryWrapper<?>) captor.getValue();
        assertThat(query.getSqlSegment()).contains("tenant_id", "status");
        assertThat(query.getParamNameValuePairs().values()).contains(42L, 0);
    }

    @Test
    void missingTenantConfigUsesAnIndependentDeploymentSnapshot() {
        when(mapper.selectOne(any(Wrapper.class))).thenReturn(null);

        FileStorageFactory.Binding binding = TenantContextHolder.callWithTenantId(7L, factory::current);
        defaults.setBasePath("/deployment/changed-after-resolution");

        assertThat(binding.storageType()).isEqualTo("local");
        assertThat(binding.storage()).isInstanceOf(LocalFileStorageServiceImpl.class);
        FileStorageProperties bound = (FileStorageProperties) ReflectionTestUtils
                .getField(binding.storage(), "properties");
        assertThat(bound.getBasePath()).isEqualTo("/deployment/default");
        assertThat(JSONUtil.parseObj(binding.snapshot()).getLong("tenantId")).isEqualTo(7L);
    }

    @Test
    void snapshotRecreatesItsOriginalConfigWithoutConsultingCurrentConfig() {
        String snapshot = factory.current().snapshot();
        defaults.setBasePath("/new-default");

        FileStorageFactory.Binding historical = factory.fromSnapshot(snapshot);

        FileStorageProperties bound = (FileStorageProperties) ReflectionTestUtils
                .getField(historical.storage(), "properties");
        assertThat(bound.getBasePath()).isEqualTo("/deployment/default");
        verify(mapper).selectOne(any(Wrapper.class));
    }

    @Test
    void configBindingAllowsInactiveConfigAndExplicitlyScopesTheLookup() {
        FileConfigDO config = new FileConfigDO().setId(19L).setStorageType("local")
                .setBasePath("/tenant/9/old").setMaxFileSizeBytes(4096L).setStatus(1);
        when(mapper.selectOne(any(Wrapper.class))).thenReturn(config);

        FileStorageFactory.Binding binding = TenantContextHolder.callWithTenantId(9L,
                () -> factory.fromConfig(19L));

        assertThat(binding.storageType()).isEqualTo("local");
        assertThat(JSONUtil.parseObj(binding.snapshot()).getLong("configId")).isEqualTo(19L);
        ArgumentCaptor<Wrapper<FileConfigDO>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(mapper).selectOne(captor.capture());
        QueryWrapper<?> query = (QueryWrapper<?>) captor.getValue();
        assertThat(query.getSqlSegment()).contains("id", "tenant_id");
        assertThat(query.getParamNameValuePairs().values()).contains(19L, 9L);
    }

    @Test
    void providerSecretIsEncryptedInSnapshotAndOnlyDecryptedIntoBoundProperties() {
        String encrypted = codec.encrypt("provider-secret");
        FileConfigDO config = new FileConfigDO().setId(21L).setStorageType("oss")
                .setEndpoint("https://oss.example.test").setAccessKeyId("key-id")
                .setAccessKeySecret(encrypted).setBucketName("bucket")
                .setMaxFileSizeBytes(4096L);
        when(mapper.selectOne(any(Wrapper.class))).thenReturn(config);

        FileStorageFactory.Binding binding = TenantContextHolder.callWithTenantId(3L,
                () -> factory.fromConfig(21L));

        assertThat(JSONUtil.parseObj(binding.snapshot()).getStr("accessKeySecret"))
                .isEqualTo(encrypted).doesNotContain("provider-secret");
        assertThat(binding.storage()).isInstanceOf(OssFileStorageServiceImpl.class);
        FileStorageProperties bound = (FileStorageProperties) ReflectionTestUtils
                .getField(binding.storage(), "properties");
        assertThat(bound.getOss().getAccessKeySecret()).isEqualTo("provider-secret");
        assertThat(defaults.getOss().getAccessKeySecret()).isNull();
    }

    @Test
    void rejectsMalformedUnknownTypeAndCrossTenantSnapshots() {
        assertThatThrownBy(() -> factory.fromSnapshot("{"))
                .isInstanceOf(RuntimeException.class);
        String unknown = "{\"version\":1,\"tenantId\":1,\"storageType\":\"unsupported\",\"maxFileSizeBytes\":1}";
        assertThatThrownBy(() -> factory.fromSnapshot(unknown))
                .isInstanceOf(RuntimeException.class);

        FileStorageFactory.Binding binding = TenantContextHolder.callWithTenantId(1L, factory::current);
        assertThatThrownBy(() -> TenantContextHolder.callWithTenantId(2L,
                () -> factory.fromSnapshot(binding.snapshot())))
                .isInstanceOf(RuntimeException.class);
    }
    @Test
    void customAdapterFactoryReceivesIndependentHistoricalProperties() {
        com.nz.admin.framework.file.FileStorageAdapterFactory custom = mock(com.nz.admin.framework.file.FileStorageAdapterFactory.class);
        FileStorageService adapter = mock(FileStorageService.class);
        when(custom.create(any())).thenReturn(adapter);
        FileStorageFactory configured = new FileStorageFactory(mapper, defaults, codec,
                java.util.Map.of("localFileStorageAdapterFactory", custom));
        String snapshot = configured.current().snapshot();
        defaults.setBasePath("/later-default");
        assertThat(configured.fromSnapshot(snapshot).storage()).isSameAs(adapter);
        ArgumentCaptor<FileStorageProperties> arguments = ArgumentCaptor.forClass(FileStorageProperties.class);
        org.mockito.Mockito.verify(custom, org.mockito.Mockito.times(2)).create(arguments.capture());
        assertThat(arguments.getAllValues().get(0)).isNotSameAs(arguments.getAllValues().get(1));
        assertThat(arguments.getValue().getBasePath()).isEqualTo("/deployment/default");
    }

    private java.util.Map<String, com.nz.admin.framework.file.FileStorageAdapterFactory> factories() {
        var config = new com.nz.admin.framework.file.config.NzFileAutoConfiguration();
        return java.util.Map.of("localFileStorageAdapterFactory", config.localFileStorageAdapterFactory(),
                "ossFileStorageAdapterFactory", config.ossFileStorageAdapterFactory(),
                "s3FileStorageAdapterFactory", config.s3FileStorageAdapterFactory());
    }
}
