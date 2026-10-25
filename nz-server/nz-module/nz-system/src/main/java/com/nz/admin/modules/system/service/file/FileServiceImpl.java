package com.nz.admin.modules.system.service.file;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.common.core.BusinessException;
import com.nz.admin.framework.datascope.core.DataScopeContext;
import com.nz.admin.framework.file.FileStorageObject;
import com.nz.admin.framework.tenant.core.TenantContextHolder;
import com.nz.admin.modules.system.entity.dataobject.file.FileDO;
import com.nz.admin.modules.system.entity.query.file.FileQuery;
import com.nz.admin.modules.system.mapper.file.FileMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;

/** 文件操作先保留可恢复记录，再写外部存储；删除意图提交后才删除对象。 */
@Slf4j
@Service
public class FileServiceImpl implements FileService {
    private final FileMapper fileMapper;
    private final FileStorageFactory storageFactory;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate required;
    private final TransactionTemplate independent;

    public FileServiceImpl(FileMapper fileMapper, FileStorageFactory storageFactory,
                           JdbcTemplate jdbc, PlatformTransactionManager transactions) {
        this.fileMapper = fileMapper;
        this.storageFactory = storageFactory;
        this.jdbc = jdbc;
        required = new TransactionTemplate(transactions);
        independent = new TransactionTemplate(transactions);
        independent.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public Page<FileDO> listPage(FileQuery query) {
        return fileMapper.selectPageByCondition(query.toPage(), query);
    }

    @Override
    public String getStorageType() {
        return storageFactory.currentStorageType();
    }

    @Override
    public FileDO upload(MultipartFile file, Long uploaderId) throws IOException {
        FileStorageFactory.Binding binding = storageFactory.current();
        FileStorageObject target = binding.storage().prepareUpload(file, uploaderId);
        FileDO record = toFileDO(target);
        record.setTenantId(currentTenantId());
        record.setStorageType(binding.storageType());
        record.setStorageConfigSnapshot(binding.snapshot());
        record.setLifecycleStatus("UPLOADING");
        record.setCleanupAttempts(0);
        record.setCleanupAfter(LocalDateTime.now().plusMinutes(30));
        independent.executeWithoutResult(status -> checkChanged(fileMapper.insert(record)));
        try {
            return required.execute(status -> {
                // 恢复任务跳过这把行锁，慢上传和外层业务事务都不会被误清理。
                FileDO locked = fileMapper.selectForUpdate(record.getId(), record.getTenantId());
                if (locked == null || !"UPLOADING".equals(locked.getLifecycleStatus())) {
                    throw new BusinessException("上传记录已失效，请重新上传");
                }
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int completion) {
                        if (completion != STATUS_COMMITTED) cleanupRolledBackUpload(record);
                    }
                });
                try {
                    binding.storage().writeUpload(file, target);
                } catch (IOException exception) {
                    throw new UncheckedIOException(exception);
                }
                String url = binding.storage().getFileUrl(record.getId());
                checkChanged(fileMapper.update(null, identity(record.getId(), record.getTenantId())
                        .eq(FileDO::getLifecycleStatus, "UPLOADING")
                        .set(FileDO::getLifecycleStatus, "READY").set(FileDO::getFileUrl, url)
                        .set(FileDO::getCleanupAfter, null)));
                record.setFileUrl(url).setLifecycleStatus("READY").setCleanupAfter(null);
                return record;
            });
        } catch (UncheckedIOException exception) {
            throw exception.getCause();
        }
    }

    @Override
    public BatchUploadResult uploadBatch(MultipartFile[] files, Long uploaderId) {
        BatchUploadResult result = new BatchUploadResult();
        if (files == null) return result;
        result.setTotalCount(files.length);
        for (int index = 0; index < files.length; index++) {
            MultipartFile file = files[index];
            try {
                if (file == null || file.isEmpty()) throw new IllegalArgumentException("文件不能为空");
                result.getSuccessFiles().add(upload(file, uploaderId));
            } catch (IOException | RuntimeException exception) {
                BatchUploadResult.FailedFile failed = new BatchUploadResult.FailedFile();
                failed.setIndex(index);
                failed.setFileName(file == null ? "" : file.getOriginalFilename());
                failed.setReason(exception instanceof IllegalArgumentException
                        ? exception.getMessage() : "上传失败，请重试或联系管理员");
                result.getFailedFiles().add(failed);
            }
        }
        result.setSuccessCount(result.getSuccessFiles().size());
        result.setFailCount(result.getFailedFiles().size());
        return result;
    }

    @Override
    public void removeById(Long id) {
        long tenantId = currentTenantId();
        required.executeWithoutResult(status -> {
            FileDO record = fileMapper.selectForUpdate(id, tenantId);
            if (record == null) return;
            requireBinding(record);
            if ("UPLOADING".equals(record.getLifecycleStatus())) {
                throw new BusinessException("文件正在上传，请稍后重试");
            }
            checkChanged(fileMapper.update(null, identity(id, tenantId)
                    .set(FileDO::getLifecycleStatus, "DELETE_PENDING")
                    .set(FileDO::getCleanupAfter, LocalDateTime.now())));
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() { completeDeletion(id, tenantId); }
            });
        });
    }

    @Override
    public void downloadById(Long id, HttpServletResponse response) throws IOException {
        FileDO record = fileMapper.selectOne(new LambdaQueryWrapper<FileDO>()
                .eq(FileDO::getId, id).eq(FileDO::getTenantId, currentTenantId()));
        if (record == null || !"READY".equals(record.getLifecycleStatus())) {
            response.setStatus(404);
            return;
        }
        requireBinding(record);
        storageFactory.fromSnapshot(record.getStorageConfigSnapshot()).storage()
                .download(toStorageObject(record), response);
    }

    @Override
    public void bindStorage(Long id, Long configId) {
        required.executeWithoutResult(status -> {
            long tenantId = currentTenantId();
            FileDO record = fileMapper.selectForUpdate(id, tenantId);
            if (record == null) throw new BusinessException("文件不存在");
            if (!record.isStorageBindingRequired()) throw new BusinessException("文件已绑定存储配置");
            FileStorageFactory.Binding binding = storageFactory.fromConfig(configId);
            if (StrUtil.isNotBlank(record.getStorageType())
                    && !binding.storageType().equalsIgnoreCase(record.getStorageType())) {
                throw new BusinessException("绑定配置必须与原文件的存储类型一致");
            }
            checkChanged(fileMapper.update(null, identity(id, tenantId)
                    .isNull(FileDO::getStorageConfigSnapshot)
                    .set(FileDO::getStorageConfigSnapshot, binding.snapshot())
                    .set(FileDO::getStorageType, binding.storageType())));
        });
    }

    /** 扫描仅获取候选身份，后续逐条恢复租户上下文、加锁并复查状态。 */
    @Scheduled(fixedDelayString = "${nz.file.cleanup-interval-ms:60000}",
            initialDelayString = "${nz.file.cleanup-interval-ms:60000}")
    public void recoverPendingFiles() {
        jdbc.queryForList("SELECT id,tenant_id FROM sys_file WHERE lifecycle_status IN "
                + "('UPLOADING','DELETE_PENDING') AND cleanup_after <= CURRENT_TIMESTAMP "
                + "ORDER BY cleanup_after LIMIT 100").forEach(candidate -> {
            long id = ((Number) candidate.get("id")).longValue();
            long tenantId = ((Number) candidate.get("tenant_id")).longValue();
            TenantContextHolder.runWithTenantId(tenantId, () -> DataScopeContext.withoutFilter(() -> {
                try {
                    markExpiredUpload(id, tenantId, false);
                    completeDeletion(id, tenantId);
                } catch (RuntimeException exception) {
                    log.warn("文件清理尚未完成，保留重试记录 id={}, tenant={}", id, tenantId, exception);
                }
                return null;
            }));
        });
    }

    private void cleanupRolledBackUpload(FileDO record) {
        try {
            markExpiredUpload(record.getId(), record.getTenantId(), true);
            completeDeletion(record.getId(), record.getTenantId());
        } catch (RuntimeException exception) {
            log.warn("上传回滚后的清理延后，保留恢复记录 id={}", record.getId(), exception);
        }
    }

    private void markExpiredUpload(Long id, Long tenantId, boolean rolledBack) {
        independent.executeWithoutResult(status -> {
            FileDO record = fileMapper.selectForCleanup(id, tenantId);
            if (record == null || !"UPLOADING".equals(record.getLifecycleStatus())) return;
            if (!rolledBack && (record.getCleanupAfter() == null
                    || record.getCleanupAfter().isAfter(LocalDateTime.now()))) return;
            checkChanged(fileMapper.update(null, identity(id, tenantId)
                    .eq(FileDO::getLifecycleStatus, "UPLOADING")
                    .set(FileDO::getLifecycleStatus, "DELETE_PENDING")
                    .set(FileDO::getCleanupAfter, LocalDateTime.now())));
        });
    }

    private void completeDeletion(Long id, Long tenantId) {
        try {
            independent.executeWithoutResult(status -> {
                FileDO record = fileMapper.selectForCleanup(id, tenantId);
                if (record == null || !"DELETE_PENDING".equals(record.getLifecycleStatus())) return;
                requireBinding(record);
                storageFactory.fromSnapshot(record.getStorageConfigSnapshot()).storage()
                        .delete(toStorageObject(record));
                checkChanged(fileMapper.delete(new LambdaQueryWrapper<FileDO>()
                        .eq(FileDO::getId, id).eq(FileDO::getTenantId, tenantId)
                        .eq(FileDO::getLifecycleStatus, "DELETE_PENDING")));
            });
        } catch (RuntimeException exception) {
            try {
                independent.executeWithoutResult(status -> fileMapper.update(null, identity(id, tenantId)
                        .eq(FileDO::getLifecycleStatus, "DELETE_PENDING")
                        .setSql("cleanup_attempts = cleanup_attempts + 1")
                        .set(FileDO::getCleanupError, "文件清理失败，等待重试")
                        .set(FileDO::getCleanupAfter, LocalDateTime.now().plusMinutes(1))));
            } catch (RuntimeException persistenceFailure) {
                exception.addSuppressed(persistenceFailure);
            }
            throw exception;
        }
    }

    private Long currentTenantId() {
        Long tenantId = TenantContextHolder.getTenantIdOrNull();
        return tenantId == null ? 1L : tenantId;
    }

    private LambdaUpdateWrapper<FileDO> identity(Long id, Long tenantId) {
        return new LambdaUpdateWrapper<FileDO>().eq(FileDO::getId, id).eq(FileDO::getTenantId, tenantId);
    }

    private void requireBinding(FileDO record) {
        if (StrUtil.isBlank(record.getStorageConfigSnapshot())) {
            throw new BusinessException("历史文件需要先绑定原存储配置");
        }
    }

    private void checkChanged(int changed) {
        if (changed != 1) throw new BusinessException("文件状态已变化，请刷新后重试");
    }

    private FileDO toFileDO(FileStorageObject object) {
        return new FileDO().setOriginalName(object.getOriginalName()).setFileName(object.getFileName())
                .setFilePath(object.getFilePath()).setFileSize(object.getFileSize()).setFileExt(object.getFileExt())
                .setMimeType(object.getMimeType()).setUploaderId(object.getUploaderId());
    }

    private FileStorageObject toStorageObject(FileDO record) {
        return new FileStorageObject().setOriginalName(record.getOriginalName()).setFileName(record.getFileName())
                .setFilePath(record.getFilePath()).setFileSize(record.getFileSize()).setFileExt(record.getFileExt())
                .setMimeType(record.getMimeType()).setUploaderId(record.getUploaderId());
    }
}
