package @@PACKAGE@@.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import @@PACKAGE@@.entity.dataobject.@@CLASS@@DO;
import @@PACKAGE@@.entity.dto.@@CLASS@@CreateRequest;
import @@PACKAGE@@.entity.dto.@@CLASS@@UpdateRequest;
import @@PACKAGE@@.entity.query.@@CLASS@@Query;

/**
 * @@FEATURE_DOC@@服务。
 *
 * @author @@AUTHOR@@
 */
public interface @@CLASS@@Service {

    Page<@@CLASS@@DO> page(@@CLASS@@Query query);

    @@CLASS@@DO getRequired(@@PK_TYPE@@ @@PK_FIELD@@);

    @@PK_TYPE@@ create(@@CLASS@@CreateRequest request);

    void update(@@CLASS@@UpdateRequest request);

    void delete(@@PK_TYPE@@ @@PK_FIELD@@);
}
