package com.nz.admin.framework.datascope;

import static org.assertj.core.api.Assertions.*;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.framework.datascope.config.NzDatascopeAutoConfiguration;
import com.nz.admin.framework.datascope.core.*;
import com.nz.admin.framework.mybatis.config.MybatisPlusAutoConfiguration;
import com.nz.admin.framework.tenant.config.TenantAutoConfiguration;
import com.nz.admin.framework.tenant.core.TenantContextHolder;

import lombok.Data;

import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.*;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@SpringBootTest(
        classes = DataScopeIntegrationTest.Application.class,
        properties = {
            "spring.datasource.url=jdbc:h2:mem:data-scope;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.datasource.username=sa",
            "nz.tenant.enabled=true",
            "nz.tenant.included-tables[0]=scope_document"
        })
class DataScopeIntegrationTest {
    @Autowired DocumentMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired AtomicReference<DataScopeResult> scope;

    @BeforeEach
    void setUp() {
        jdbc.execute("DROP TABLE IF EXISTS scope_document");
        jdbc.execute(
                "CREATE TABLE scope_document(id BIGINT PRIMARY KEY, tenant_id BIGINT, dept_id"
                    + " BIGINT, owner_id BIGINT, title VARCHAR(50))");
        jdbc.update(
                "INSERT INTO scope_document VALUES"
                    + " (1,1,10,100,'own'),(2,1,11,200,'child'),(3,1,20,300,'other'),(4,2,10,100,'other"
                    + " tenant')");
        TenantContextHolder.setTenantId(1L);
        scope.set(new DataScopeResult(false, Set.of(11L), true, 100L));
    }

    @AfterEach
    void clear() {
        TenantContextHolder.clear();
    }

    @Test
    void filtersPaginationDetailAndJoinedCustomSql() {
        var page =
                mapper.selectPage(new Page<>(1, 10), new QueryWrapper<Document>().orderByAsc("id"));
        assertThat(page.getTotal()).isEqualTo(2);
        assertThat(page.getRecords()).extracting(Document::getId).containsExactly(1L, 2L);
        assertThat(mapper.selectById(3L)).isNull();
        assertThat(mapper.joinedIds()).containsExactly(1L, 2L);
    }

    @Test
    void restrictsUpdatesAndBatchDeletesInTheDatabase() {
        Document forbidden = new Document();
        forbidden.setId(3L);
        forbidden.setTitle("changed");
        assertThat(mapper.updateById(forbidden)).isZero();
        assertThat(mapper.delete(new QueryWrapper<Document>().in("id", List.of(1L, 3L, 4L))))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM scope_document", Long.class))
                .isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT title FROM scope_document WHERE id=3", String.class))
                .isEqualTo("other");
    }

    @Test
    void allDataAndInternalBypassStillRespectTenantIsolation() {
        scope.set(new DataScopeResult(true, Set.of(), false, 100L));
        assertThat(mapper.selectCount(null)).isEqualTo(3);
        scope.set(new DataScopeResult(false, Set.of(), false, 100L));
        assertThat(mapper.selectCount(null)).isZero();
        assertThat(DataScopeContext.withoutFilter(() -> mapper.selectCount(null))).isEqualTo(3);
        assertThat(mapper.selectCount(null)).isZero();
    }

    @Test
    void rejectsMissingIdentityAndRestoresNestedBypassAfterFailure() {
        scope.set(null);
        assertThatThrownBy(() -> mapper.selectList(null)).hasMessageContaining("无法解析数据权限");
        assertThatThrownBy(
                        () ->
                                DataScopeContext.withoutFilter(
                                        () -> {
                                            assertThat(
                                                            DataScopeContext.withoutFilter(
                                                                    DataScopeContext::isIgnored))
                                                    .isTrue();
                                            throw new IllegalArgumentException("failure");
                                        }))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(DataScopeContext.isIgnored()).isFalse();
    }

    @Data
    @TableName("scope_document")
    public static class Document {
        private Long id;
        private Long tenantId;
        private Long deptId;
        private Long ownerId;
        private String title;
    }

    @org.apache.ibatis.annotations.Mapper
    public interface DocumentMapper extends BaseMapper<Document> {
        @Select(
                "SELECT d.id FROM scope_document d LEFT JOIN (SELECT 1 AS marker) x ON x.marker=1"
                    + " WHERE d.title <> 'missing' ORDER BY d.id")
        List<Long> joinedIds();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @MapperScan(
            basePackageClasses = DocumentMapper.class,
            annotationClass = org.apache.ibatis.annotations.Mapper.class)
    @Import({
        NzDatascopeAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        TenantAutoConfiguration.class
    })
    static class Application {
        @Bean
        AtomicReference<DataScopeResult> scope() {
            return new AtomicReference<>();
        }

        @Bean
        DataScopeResolver resolver(AtomicReference<DataScopeResult> scope) {
            return scope::get;
        }

        @Bean
        DataScopeRuleCustomizer rules() {
            return rules -> rules.add(new DataScopeRule("scope_document", "dept_id", "owner_id"));
        }
    }
}
