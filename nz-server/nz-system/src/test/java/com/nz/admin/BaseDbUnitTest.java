package com.nz.admin;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

/**
 * DB 单元测试基类。
 * <p>
 * 使用 H2 内存数据库，每个测试方法结束后自动回滚，测试间数据互不干扰。
 * <pre>{@code
 * class SysDeptServiceTest extends BaseDbUnitTest {
 *
 *     @Autowired
 *     private SysDeptService deptService;
 *
 *     @Test
 *     void testSave() {
 *         SysDept dept = new SysDept();
 *         dept.setName("技术部");
 *         deptService.save(dept);
 *         assertNotNull(dept.getId());
 *     }
 * }
 * }</pre>
 */
@SpringBootTest(classes = NzSystemTestApplication.class)
@Transactional
@Rollback
public abstract class BaseDbUnitTest {
}
