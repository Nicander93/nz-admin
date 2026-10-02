package com.nz.admin.modules.system.service.file;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nz.admin.modules.system.entity.dto.file.FileConfigSaveRequest;
import com.nz.admin.modules.system.entity.vo.file.FileConfigVO;

public interface FileConfigService {
    IPage<FileConfigVO> page(Integer pageNum, Integer pageSize, String configName, String storageType, Integer status);
    FileConfigVO get(Long id);
    Long create(FileConfigSaveRequest request);
    void update(FileConfigSaveRequest request);
    void delete(Long id);
    void activate(Long id);
    void testConnection(Long id);
}
