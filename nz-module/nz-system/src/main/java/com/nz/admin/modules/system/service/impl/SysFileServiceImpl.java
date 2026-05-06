package com.nz.admin.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.SysFile;
import com.nz.admin.modules.system.mapper.SysFileMapper;
import com.nz.admin.modules.system.query.SysFileQuery;
import com.nz.admin.modules.system.service.SysFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 文件上传这块的服务实现。
 */
@Service
public class SysFileServiceImpl implements SysFileService {

    @Autowired
    private SysFileMapper fileMapper;

    /**
     * 按分页条件查文件记录。
     */
    @Override
    public Page<SysFile> listPage(SysFileQuery query) {
        return fileMapper.selectPageByCondition(query.toPage(), query);
    }

    /**
     * 按 id 拿文件详情。
     */
    @Override
    public SysFile getById(Long id) {
        return fileMapper.selectById(id);
    }

    /**
     * 新增一条文件记录。
     */
    @Override
    public void save(SysFile file) {
        fileMapper.insert(file);
    }

    /**
     * 批量新增文件记录。
     */
    @Override
    public void saveBatch(List<SysFile> files) {
        for (SysFile file : files) {
            fileMapper.insert(file);
        }
    }

    /**
     * 按 id 删掉文件记录。
     */
    @Override
    public void removeById(Long id) {
        fileMapper.deleteById(id);
    }
}
