package com.nz.admin.modules.system.service.file;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nz.admin.common.core.BusinessException;
import com.nz.admin.framework.file.FileConfigSecretCodec;
import com.nz.admin.framework.file.FileStorageProperties;
import com.nz.admin.framework.file.FileStorageService;
import com.nz.admin.framework.file.FileStorageAdapterFactory;
import com.nz.admin.framework.tenant.core.TenantContextHolder;
import com.nz.admin.modules.system.entity.dataobject.file.FileConfigDO;
import com.nz.admin.modules.system.mapper.file.FileConfigMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Component
public class FileStorageFactory {

    private static final Set<String> SNAPSHOT_FIELDS = Set.of(
            "version", "tenantId", "configId", "storageType", "basePath", "endpoint",
            "accessKeyId", "accessKeySecret", "bucketName", "region", "domain", "pathPrefix",
            "localAccessUrlPrefix", "maxFileSizeBytes"
    );

    public record Binding(String storageType, String snapshot, FileStorageService storage) {
    }

    private final FileConfigMapper mapper;
    private final FileStorageProperties defaults;
    private final FileConfigSecretCodec secretCodec;
    private final Map<String, FileStorageAdapterFactory> adapterFactories;

    public FileStorageFactory(FileConfigMapper mapper, FileStorageProperties defaults,
                              FileConfigSecretCodec secretCodec,
                              Map<String, FileStorageAdapterFactory> adapterFactories) {
        this.mapper = mapper;
        this.defaults = defaults;
        this.secretCodec = secretCodec;
        this.adapterFactories = Map.copyOf(adapterFactories);
    }

    public Binding current() {
        Long tenantId = currentTenantId();
        FileConfigDO active = mapper.selectOne(new QueryWrapper<FileConfigDO>()
                .eq("tenant_id", tenantId).eq("status", 0).last("LIMIT 1"));
        return active == null
                ? fromSnapshot(snapshotFromDefaults(tenantId))
                : fromSnapshot(fromConfigDO(active, tenantId));
    }

    public Binding fromSnapshot(String snapshot) {
        if (StrUtil.isBlank(snapshot)) {
            throw new BusinessException("文件存储快照不能为空");
        }
        JSONObject values;
        try {
            values = JSONUtil.parseObj(snapshot);
        } catch (RuntimeException exception) {
            throw new BusinessException("文件存储快照格式无效");
        }
        if (!SNAPSHOT_FIELDS.containsAll(values.keySet())
                || !values.keySet().containsAll(Set.of("version", "tenantId", "storageType", "maxFileSizeBytes"))
                || values.getInt("version", 0) != 1) {
            throw new BusinessException("文件存储快照字段或版本无效");
        }
        Long tenantId = values.getLong("tenantId");
        Long maxFileSizeBytes = values.getLong("maxFileSizeBytes");
        Long currentTenantId = currentTenantId();
        if (tenantId == null || maxFileSizeBytes == null || maxFileSizeBytes <= 0
                || StrUtil.isBlank(values.getStr("storageType"))) {
            throw new BusinessException("文件存储快照缺少有效配置");
        }
        if (!tenantId.equals(currentTenantId)) {
            throw new BusinessException("文件存储快照不属于当前租户");
        }
        String storageType = values.getStr("storageType");
        FileStorageProperties bound = boundProperties(values);
        FileStorageService storage = createAdapter(storageType, bound);
        return new Binding(storageType, snapshot, storage);
    }

    public Binding fromConfig(Long id) {
        FileConfigDO config = mapper.selectOne(new QueryWrapper<FileConfigDO>()
                .eq("id", id).eq("tenant_id", currentTenantId()));
        if (config == null) {
            throw new BusinessException("文件配置不存在");
        }
        return fromSnapshot(fromConfigDO(config, currentTenantId()));
    }

    public String currentStorageType() {
        return current().storageType();
    }

    String fromConfigDO(FileConfigDO config, Long tenantId) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("version", 1);
        snapshot.put("tenantId", tenantId);
        snapshot.put("configId", config.getId());
        snapshot.put("storageType", config.getStorageType());
        snapshot.put("basePath", config.getBasePath());
        snapshot.put("endpoint", config.getEndpoint());
        snapshot.put("accessKeyId", config.getAccessKeyId());
        snapshot.put("accessKeySecret", config.getAccessKeySecret());
        snapshot.put("bucketName", config.getBucketName());
        snapshot.put("region", config.getRegion());
        snapshot.put("domain", config.getDomain());
        snapshot.put("pathPrefix", config.getPathPrefix());
        snapshot.put("localAccessUrlPrefix", config.getLocalAccessUrlPrefix());
        snapshot.put("maxFileSizeBytes", config.getMaxFileSizeBytes());
        return JSONUtil.toJsonStr(snapshot);
    }

    private String snapshotFromDefaults(Long tenantId) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("version", 1);
        snapshot.put("tenantId", tenantId);
        snapshot.put("configId", null);
        snapshot.put("storageType", defaults.getStorageType());
        snapshot.put("basePath", defaults.getBasePath());
        snapshot.put("endpoint", null);
        snapshot.put("accessKeyId", null);
        snapshot.put("accessKeySecret", null);
        snapshot.put("bucketName", null);
        snapshot.put("region", null);
        snapshot.put("domain", null);
        snapshot.put("pathPrefix", null);
        snapshot.put("localAccessUrlPrefix", defaults.getLocalAccessUrlPrefix());
        snapshot.put("maxFileSizeBytes", defaults.getMaxFileSizeBytes());
        if ("oss".equalsIgnoreCase(defaults.getStorageType())) {
            FileStorageProperties.Oss oss = defaults.getOss();
            snapshot.put("endpoint", oss.getEndpoint());
            snapshot.put("accessKeyId", oss.getAccessKeyId());
            snapshot.put("accessKeySecret", secretCodec.encrypt(oss.getAccessKeySecret()));
            snapshot.put("bucketName", oss.getBucketName());
            snapshot.put("domain", oss.getDomain());
            snapshot.put("pathPrefix", oss.getPathPrefix());
        } else if ("s3".equalsIgnoreCase(defaults.getStorageType())) {
            FileStorageProperties.S3 s3 = defaults.getS3();
            snapshot.put("endpoint", s3.getEndpoint());
            snapshot.put("accessKeyId", s3.getAccessKeyId());
            snapshot.put("accessKeySecret", secretCodec.encrypt(s3.getAccessKeySecret()));
            snapshot.put("bucketName", s3.getBucketName());
            snapshot.put("region", s3.getRegion());
            snapshot.put("domain", s3.getDomain());
            snapshot.put("pathPrefix", s3.getPathPrefix());
        }
        return JSONUtil.toJsonStr(snapshot);
    }

    private FileStorageProperties boundProperties(JSONObject snapshot) {
        FileStorageProperties bound = new FileStorageProperties();
        bound.setStorageType(snapshot.getStr("storageType"));
        bound.setBasePath(snapshot.getStr("basePath"));
        bound.setMaxFileSizeBytes(snapshot.getLong("maxFileSizeBytes"));
        bound.setAllowedExtensions(defaults.getAllowedExtensions() == null
                ? null : new java.util.ArrayList<>(defaults.getAllowedExtensions()));
        bound.setAllowedMimeTypes(defaults.getAllowedMimeTypes() == null
                ? null : new java.util.ArrayList<>(defaults.getAllowedMimeTypes()));
        String localAccessUrlPrefix = snapshot.getStr("localAccessUrlPrefix");
        bound.setLocalAccessUrlPrefix(StrUtil.blankToDefault(
                localAccessUrlPrefix, defaults.getLocalAccessUrlPrefix()));

        FileStorageProperties.Oss oss = new FileStorageProperties.Oss();
        oss.setEndpoint(snapshot.getStr("endpoint"));
        oss.setAccessKeyId(snapshot.getStr("accessKeyId"));
        oss.setAccessKeySecret(secretCodec.decrypt(snapshot.getStr("accessKeySecret")));
        oss.setBucketName(snapshot.getStr("bucketName"));
        oss.setDomain(snapshot.getStr("domain"));
        oss.setPathPrefix(StrUtil.blankToDefault(snapshot.getStr("pathPrefix"), ""));
        bound.setOss(oss);

        FileStorageProperties.S3 s3 = new FileStorageProperties.S3();
        s3.setEndpoint(snapshot.getStr("endpoint"));
        s3.setAccessKeyId(snapshot.getStr("accessKeyId"));
        s3.setAccessKeySecret(secretCodec.decrypt(snapshot.getStr("accessKeySecret")));
        s3.setBucketName(snapshot.getStr("bucketName"));
        s3.setRegion(StrUtil.blankToDefault(snapshot.getStr("region"), "us-east-1"));
        s3.setDomain(snapshot.getStr("domain"));
        s3.setPathPrefix(StrUtil.blankToDefault(snapshot.getStr("pathPrefix"), ""));
        bound.setS3(s3);
        return bound;
    }

    private FileStorageService createAdapter(String storageType, FileStorageProperties properties) {
        FileStorageAdapterFactory factory = adapterFactories.get(
                storageType.toLowerCase(java.util.Locale.ROOT) + "FileStorageAdapterFactory");
        if (factory == null) throw new BusinessException("不支持的存储类型");
        return factory.create(properties);
    }

    private Long currentTenantId() {
        Long tenantId = TenantContextHolder.getTenantIdOrNull();
        return tenantId == null ? 1L : tenantId;
    }
}
