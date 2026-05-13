package com.nz.admin.framework.datascope;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import com.nz.admin.framework.test.core.ut.BaseWebContextUnitTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = DataScopeAspectTest.Application.class
)
@Import(DataScopeAspectTest.TestConfiguration.class)
class DataScopeAspectTest extends BaseWebContextUnitTest {

    @Resource
    private DataScopeTargetService dataScopeTargetService;

    @MockBean
    private DataScopeUserResolver dataScopeUserResolver;

    @Test
    void testAllScope_adminShouldGetNullSql() {
        long userId = 2001L;
        mockLogin(userId);
        when(dataScopeUserResolver.isAdmin(userId)).thenReturn(true);

        TestDataScopeQuery query = new TestDataScopeQuery();
        dataScopeTargetService.queryByAll(query);

        assertNull(query.getDataScopeSql());
    }

    @Test
    void testDeptScope_shouldAppendDeptSql() {
        long userId = 2002L;
        mockLogin(userId);
        when(dataScopeUserResolver.isAdmin(userId)).thenReturn(false);
        when(dataScopeUserResolver.getDeptId(userId)).thenReturn(300L);

        TestDataScopeQuery query = new TestDataScopeQuery();
        dataScopeTargetService.queryByDept(query);

        assertEquals("dept_id = 300", query.getDataScopeSql());
    }

    @Test
    void testSelfScope_shouldAppendSelfSql() {
        long userId = 2003L;
        mockLogin(userId);
        when(dataScopeUserResolver.isAdmin(userId)).thenReturn(false);
        when(dataScopeUserResolver.getDeptId(userId)).thenReturn(301L);

        TestDataScopeQuery query = new TestDataScopeQuery();
        dataScopeTargetService.queryBySelf(query);

        assertEquals("id = 2003", query.getDataScopeSql());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class
    })
    static class Application {
    }

    @Configuration
    static class TestConfiguration {

        @Bean
        DataScopeAspect dataScopeAspect(DataScopeUserResolver dataScopeUserResolver) {
            return new DataScopeAspect(dataScopeUserResolver);
        }

        @Bean
        DataScopeTargetService dataScopeTargetService() {
            return new DataScopeTargetService();
        }
    }

    @Service
    static class DataScopeTargetService {

        @DataScope(value = {DataScopeType.ALL}, deptAlias = "dept_id", userAlias = "id")
        public void queryByAll(TestDataScopeQuery query) {
        }

        @DataScope(value = {DataScopeType.DEPT}, deptAlias = "dept_id", userAlias = "id")
        public void queryByDept(TestDataScopeQuery query) {
        }

        @DataScope(value = {DataScopeType.SELF}, deptAlias = "dept_id", userAlias = "id")
        public void queryBySelf(TestDataScopeQuery query) {
        }
    }

    static class TestDataScopeQuery implements DataScopeParam {

        private String dataScopeSql;

        @Override
        public void setDataScopeSql(String dataScopeSql) {
            this.dataScopeSql = dataScopeSql;
        }

        public String getDataScopeSql() {
            return dataScopeSql;
        }
    }
}
