package com.nz.admin.modules.system.service.file;

import java.util.Map;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nz.admin.common.core.BusinessException;
import com.nz.admin.framework.file.FileConfigSecretCodec;
import com.nz.admin.framework.file.FileStorageProperties;
import com.nz.admin.framework.file.FileStorageService;
import com.nz.admin.modules.system.entity.dataobject.file.FileConfigDO;
import com.nz.admin.modules.system.entity.dto.file.FileConfigSaveRequest;
import com.nz.admin.modules.system.entity.vo.file.FileConfigVO;
import com.nz.admin.modules.system.mapper.file.FileConfigMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 文件存储配置服务实现。 */
@Service
public class FileConfigServiceImpl extends ServiceImpl<FileConfigMapper, FileConfigDO>
        implements FileConfigService {

    private final FileStorageProperties properties;
    private final FileConfigSecretCodec secretCodec;
    private final Map<String, FileStorageService> storageServices;

    public FileConfigServiceImpl(FileStorageProperties properties, FileConfigSecretCodec secretCodec,
                                 Map<String, FileStorageService> storageServices) {
        this.properties = properties;
        this.secretCodec = secretCodec;
        this.storageServices = storageServices;
    }

    @PostConstruct
    public void applyActiveConfigOnStartup() {
        FileConfigDO active = baseMapper.selectOne(new LambdaQueryWrapper<FileConfigDO>()
                .eq(FileConfigDO::getStatus, 0).last("LIMIT 1"));
        if (active != null) {
            apply(active);
        }
    }

    @Override
    public IPage<FileConfigVO> page(Integer pageNum, Integer pageSize, String configName,
                                   String storageType, Integer status) {
        Page<FileConfigDO> result = baseMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<FileConfigDO>()
                        .like(StrUtil.isNotBlank(configName), FileConfigDO::getConfigName, configName)
                        .eq(StrUtil.isNotBlank(storageType), FileConfigDO::getStorageType, storageType)
                        .eq(status != null, FileConfigDO::getStatus, status)
                        .orderByAsc(FileConfigDO::getStatus).orderByDesc(FileConfigDO::getId));
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
        baseMapper.insert(config);
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
        baseMapper.updateById(update);
        if (Integer.valueOf(0).equals(current.getStatus())) {
            validateForActivation(update);
            apply(update);
        }
    }

    @Override
    public void delete(Long id) {
        FileConfigDO config = getRequired(id);
        if (Integer.valueOf(0).equals(config.getStatus())) {
            throw new BusinessException("当前生效配置不能删除，请先启用其他配置");
        }
        baseMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activate(Long id) {
        FileConfigDO config = getRequired(id);
        validateForActivation(config);
        baseMapper.update(null, new LambdaUpdateWrapper<FileConfigDO>()
                .set(FileConfigDO::getStatus, 1).eq(FileConfigDO::getStatus, 0));
        FileConfigDO statusUpdate = new FileConfigDO();
        statusUpdate.setId(id);
        statusUpdate.setStatus(0);
        baseMapper.updateById(statusUpdate);
        apply(config);
    }

    @Override
    public void testConnection(Long id) {
        FileConfigDO config = getRequired(id);
        if (!Integer.valueOf(0).equals(config.getStatus())) {
            throw new BusinessException("只能测试当前生效的文件配置");
        }
        FileStorageService storageService = resolveStorageService(config.getStorageType());
        if (storageService == null) {
            throw new BusinessException("不支持的存储类型");
        }
        storageService.checkAvailable();
    }

    private FileConfigDO getRequired(Long id) {
        FileConfigDO config = baseMapper.selectById(id);
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

    private void apply(FileConfigDO config) {
        properties.setStorageType(config.getStorageType());
        properties.setMaxFileSizeBytes(config.getMaxFileSizeBytes());
        properties.setBasePath(config.getBasePath());
        if (StrUtil.isNotBlank(config.getLocalAccessUrlPrefix())) {
            properties.setLocalAccessUrlPrefix(config.getLocalAccessUrlPrefix());
        }
        if ("oss".equalsIgnoreCase(config.getStorageType())) {
            FileStorageProperties.Oss oss = properties.getOss();
            oss.setEndpoint(config.getEndpoint());
            oss.setAccessKeyId(config.getAccessKeyId());
            oss.setAccessKeySecret(secretCodec.decrypt(config.getAccessKeySecret()));
            oss.setBucketName(config.getBucketName());
            oss.setDomain(config.getDomain());
            oss.setPathPrefix(StrUtil.blankToDefault(config.getPathPrefix(), ""));
    }
        if ("s3".equalsIgnoreCase(config.getStorageType())) {
            FileStorageProperties.S3 s3 = properties.getS3();
            s3.setEndpoint(config.getEndpoint());
            s3.setAccessKeyId(config.getAccessKeyId());
            s3.setAccessKeySecret(secretCodec.decrypt(config.getAccessKeySecret()));
            s3.setBucketName(config.getBucketName());
            s3.setRegion(StrUtil.blankToDefault(config.getRegion(), "us-east-1"));
            s3.setDomain(config.getDomain());
            s3.setPathPrefix(StrUtil.blankToDefault(config.getPathPrefix(), ""));
        }
    }


    private FileStorageService resolveStorageService(String storageType) {
        if (StrUtil.isBlank(storageType)) {
            return null;
        }
        return storageServices.get(storageType.toLowerCase() + "FileStorageService");
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
}
