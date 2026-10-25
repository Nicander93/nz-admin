package com.nz.admin.modules.system.service.file;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.nz.admin.common.core.BusinessException;
import com.nz.admin.framework.file.FileConfigSecretCodec;
import com.nz.admin.framework.tenant.core.TenantContextHolder;
import com.nz.admin.modules.system.entity.dataobject.file.FileConfigDO;
import com.nz.admin.modules.system.entity.dto.file.FileConfigSaveRequest;
import com.nz.admin.modules.system.entity.vo.file.FileConfigVO;
import com.nz.admin.modules.system.mapper.file.FileConfigMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FileConfigServiceImpl extends ServiceImpl<FileConfigMapper, FileConfigDO>
        implements FileConfigService {

    private final FileConfigSecretCodec secretCodec;
    private final FileStorageFactory storageFactory;

    public FileConfigServiceImpl(FileConfigSecretCodec secretCodec, FileStorageFactory storageFactory) {
        this.secretCodec = secretCodec;
        this.storageFactory = storageFactory;
    }

    @Override
    public IPage<FileConfigVO> page(Integer pageNum, Integer pageSize, String configName,
                                   String storageType, Integer status) {
        Page<FileConfigDO> result = baseMapper.selectPage(new Page<>(pageNum, pageSize),
                new QueryWrapper<FileConfigDO>()
                        .eq("tenant_id", currentTenantId())
                        .like(StrUtil.isNotBlank(configName), "config_name", configName)
                        .eq(StrUtil.isNotBlank(storageType), "storage_type", storageType)
                        .eq(status != null, "status", status)
                        .orderByAsc("status").orderByDesc("id"));
        return result.convert(this::toVO);
    }

    @Override
    public FileConfigVO get(Long id) {
        return toVO(getRequired(id));
    }

    @Override
    public Long create(FileConfigSaveRequest request) {
        FileConfigDO config = BeanUtil.copyProperties(request, FileConfigDO.class);
        config.setId(null);
        config.setStatus(1);
        config.setAccessKeySecret(secretCodec.encrypt(request.getAccessKeySecret()));
        if (baseMapper.insert(config) != 1) {
            throw new BusinessException("文件配置保存失败");
        }
        return config.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(FileConfigSaveRequest request) {
        FileConfigDO current = getRequired(request.getId());
        FileConfigDO update = BeanUtil.copyProperties(request, FileConfigDO.class);
        update.setStatus(current.getStatus());
        if ("local".equalsIgnoreCase(update.getStorageType())) {
            update.setAccessKeyId(null);
            update.setAccessKeySecret(null);
        } else {
            update.setAccessKeyId(StrUtil.isBlank(request.getAccessKeyId())
                    ? current.getAccessKeyId() : request.getAccessKeyId());
            update.setAccessKeySecret(StrUtil.isBlank(request.getAccessKeySecret())
                    ? current.getAccessKeySecret() : secretCodec.encrypt(request.getAccessKeySecret()));
        }
        int updated = baseMapper.update(update, new UpdateWrapper<FileConfigDO>()
                .eq("id", request.getId()).eq("tenant_id", currentTenantId()));
        if (updated != 1) {
            throw new BusinessException("文件配置不存在或已变更");
        }
        if (Integer.valueOf(0).equals(current.getStatus())) {
            validateForActivation(update);
        }
    }

    @Override
    public void delete(Long id) {
        FileConfigDO config = getRequired(id);
        if (Integer.valueOf(0).equals(config.getStatus())) {
            throw new BusinessException("当前生效配置不能删除，请先启用其他配置");
        }
        if (baseMapper.delete(new QueryWrapper<FileConfigDO>()
                .eq("id", id).eq("tenant_id", currentTenantId())) != 1) {
            throw new BusinessException("文件配置不存在或已变更");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activate(Long id) {
        FileConfigDO config = getRequired(id);
        validateForActivation(config);
        baseMapper.update(null, new UpdateWrapper<FileConfigDO>()
                .set("status", 1).eq("tenant_id", currentTenantId()).eq("status", 0));
        FileConfigDO statusUpdate = new FileConfigDO();
        statusUpdate.setStatus(0);
        if (baseMapper.update(statusUpdate, new UpdateWrapper<FileConfigDO>()
                .eq("id", id).eq("tenant_id", currentTenantId())) != 1) {
            throw new BusinessException("文件配置不存在或已变更");
        }
    }

    @Override
    public void testConnection(Long id) {
        storageFactory.fromConfig(id).storage().checkAvailable();
    }

    private FileConfigDO getRequired(Long id) {
        FileConfigDO config = baseMapper.selectOne(new QueryWrapper<FileConfigDO>()
                .eq("id", id).eq("tenant_id", currentTenantId()));
        if (config == null) {
            throw new BusinessException("文件配置不存在");
        }
        return config;
    }

    private void validateForActivation(FileConfigDO config) {
        if (config.getMaxFileSizeBytes() == null || config.getMaxFileSizeBytes() <= 0) {
            throw new BusinessException("单文件大小限制必须大于 0");
        }
        if ("local".equalsIgnoreCase(config.getStorageType())) {
            if (StrUtil.isBlank(config.getBasePath())) {
                throw new BusinessException("本地存储路径不能为空");
            }
            return;
        }
        if (!"oss".equalsIgnoreCase(config.getStorageType()) && !"s3".equalsIgnoreCase(config.getStorageType())) {
            throw new BusinessException("不支持的存储类型");
        }
        if (StrUtil.hasBlank(config.getEndpoint(), config.getAccessKeyId(),
                config.getAccessKeySecret(), config.getBucketName())) {
            throw new BusinessException("启用对象存储前必须完整配置 endpoint、AccessKey 和 bucket");
        }
    }

    private FileConfigVO toVO(FileConfigDO config) {
        FileConfigVO vo = BeanUtil.copyProperties(config, FileConfigVO.class);
        vo.setAccessKeyIdMasked(mask(config.getAccessKeyId()));
        vo.setAccessKeySecretConfigured(StrUtil.isNotBlank(config.getAccessKeySecret()));
        return vo;
    }

    private String mask(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        if (value.length() <= 6) {
            return "***";
        }
        return StrUtil.subPre(value, 3) + "***" + value.substring(value.length() - 3);
    }

    private Long currentTenantId() {
        Long tenantId = TenantContextHolder.getTenantIdOrNull();
        return tenantId == null ? 1L : tenantId;
    }
}
