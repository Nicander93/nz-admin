package com.nz.admin.modules.system.service.file;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.common.core.BusinessException;
import com.nz.admin.framework.file.FileConfigSecretCodec;
import com.nz.admin.framework.file.FileStorageProperties;
import com.nz.admin.framework.file.FileStorageService;
import com.nz.admin.framework.test.core.ut.BaseMockitoUnitTest;
import com.nz.admin.framework.tenant.core.TenantContextHolder;
import com.nz.admin.modules.system.entity.dataobject.file.FileConfigDO;
import com.nz.admin.modules.system.entity.dto.file.FileConfigSaveRequest;
import com.nz.admin.modules.system.entity.vo.file.FileConfigVO;
import com.nz.admin.modules.system.mapper.file.FileConfigMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileConfigServiceImplTest extends BaseMockitoUnitTest {

    @Mock
    private FileConfigMapper mapper;
    @Mock
    private FileStorageFactory storageFactory;

    private FileStorageProperties properties;
    private FileConfigSecretCodec codec;
    private FileConfigServiceImpl service;

    @BeforeEach
    void setUpService() {
        properties = new FileStorageProperties();
        properties.setConfigEncryptionKey("file-config-unit-test-key");
        codec = new FileConfigSecretCodec(properties);
        service = new FileConfigServiceImpl(codec, storageFactory);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
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
    void getMasksCredentialsAndFiltersByTenant() {
        when(mapper.selectOne(any(Wrapper.class))).thenReturn(ossConfig());

        FileConfigVO result = TenantContextHolder.callWithTenantId(8L, () -> service.get(1L));

        assertThat(result.getAccessKeyIdMasked()).isEqualTo("abc***xyz");
        assertThat(result.isAccessKeySecretConfigured()).isTrue();
        ArgumentCaptor<Wrapper<FileConfigDO>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(mapper).selectOne(captor.capture());
        QueryWrapper<?> query = (QueryWrapper<?>) captor.getValue();
        assertThat(query.getSqlSegment()).contains("tenant_id", "id");
        assertThat(query.getParamNameValuePairs().values()).contains(8L, 1L);
    }

    @Test
    void updateKeepsCredentialsWhenRequestLeavesThemBlankAndDoesNotMutateDefaults() {
        FileConfigDO current = ossConfig();
        when(mapper.selectOne(any(Wrapper.class))).thenReturn(current);
        when(mapper.update(any(FileConfigDO.class), any(Wrapper.class))).thenReturn(1);
        FileConfigSaveRequest request = ossRequest();
        request.setId(1L);
        request.setAccessKeyId("");
        request.setAccessKeySecret("");

        TenantContextHolder.runWithTenantId(8L, () -> service.update(request));

        ArgumentCaptor<FileConfigDO> update = ArgumentCaptor.forClass(FileConfigDO.class);
        ArgumentCaptor<Wrapper<FileConfigDO>> where = ArgumentCaptor.forClass(Wrapper.class);
        verify(mapper).update(update.capture(), where.capture());
        assertThat(update.getValue().getAccessKeyId()).isEqualTo("abcdefxyz");
        assertThat(codec.decrypt(update.getValue().getAccessKeySecret())).isEqualTo("plain-secret");
        assertThat(((UpdateWrapper<?>) where.getValue()).getSqlSegment()).contains("tenant_id");
        assertThat(properties.getStorageType()).isEqualTo("local");
    }

    @Test
    void updateRejectsWhenNoTenantOwnedRowWasChanged() {
        when(mapper.selectOne(any(Wrapper.class))).thenReturn(ossConfig());
        when(mapper.update(any(FileConfigDO.class), any(Wrapper.class))).thenReturn(0);
        FileConfigSaveRequest request = ossRequest();
        request.setId(1L);

        assertThatThrownBy(() -> service.update(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不存在");
    }

    @Test
    void activateChangesOnlyTheCurrentTenantAndDoesNotMutateGlobalProperties() {
        FileConfigDO target = new FileConfigDO().setId(2L).setConfigName("local")
                .setStorageType("local").setBasePath("/tenant/8/files")
                .setLocalAccessUrlPrefix("/files/").setMaxFileSizeBytes(2048L).setStatus(1);
        when(mapper.selectOne(any(Wrapper.class))).thenReturn(target);
        when(mapper.update(isNull(), any(Wrapper.class))).thenReturn(1);
        when(mapper.update(any(FileConfigDO.class), any(Wrapper.class))).thenReturn(1);

        TenantContextHolder.runWithTenantId(8L, () -> service.activate(2L));

        ArgumentCaptor<Wrapper<FileConfigDO>> statusWhere = ArgumentCaptor.forClass(Wrapper.class);
        verify(mapper).update(isNull(), statusWhere.capture());
        assertThat(((UpdateWrapper<?>) statusWhere.getValue()).getSqlSegment())
                .contains("tenant_id", "status");
        assertThat(properties.getBasePath()).isNotEqualTo("/tenant/8/files");
        assertThat(properties.getStorageType()).isEqualTo("local");
    }

    @Test
    void deleteRejectsActiveConfigAndChecksAffectedRowsForInactiveConfig() {
        when(mapper.selectOne(any(Wrapper.class)))
                .thenReturn(new FileConfigDO().setId(3L).setStatus(0));
        assertThatThrownBy(() -> service.delete(3L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能删除");

        when(mapper.selectOne(any(Wrapper.class)))
                .thenReturn(new FileConfigDO().setId(4L).setStatus(1));
        when(mapper.delete(any(Wrapper.class))).thenReturn(0);
        assertThatThrownBy(() -> service.delete(4L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不存在");
    }

    @Test
    void connectionTestUsesAnInactiveTenantConfig() {
        FileStorageService storage = mock(FileStorageService.class);
        when(storageFactory.fromConfig(6L))
                .thenReturn(new FileStorageFactory.Binding("local", "snapshot", storage));

        TenantContextHolder.runWithTenantId(8L, () -> service.testConnection(6L));

        verify(storageFactory).fromConfig(6L);
        verify(storage).checkAvailable();
    }

    @Test
    void pageAddsExplicitTenantFilter() {
        when(mapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(new Page<>(1, 10));

        TenantContextHolder.runWithTenantId(12L,
                () -> service.page(1, 10, null, null, null));

        ArgumentCaptor<Wrapper<FileConfigDO>> where = ArgumentCaptor.forClass(Wrapper.class);
        verify(mapper).selectPage(any(Page.class), where.capture());
        QueryWrapper<?> query = (QueryWrapper<?>) where.getValue();
        assertThat(query.getSqlSegment()).contains("tenant_id");
        assertThat(query.getParamNameValuePairs().values()).contains(12L);
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
