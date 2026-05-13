package com.nz.admin.framework.mybatis.core.query;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LambdaQueryWrapperXTest {

    @Test
    void shouldSkipEmptyConditionsAndKeepPresentOnes() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), DemoEntity.class);

        LambdaQueryWrapperX<DemoEntity> wrapper = new LambdaQueryWrapperX<DemoEntity>()
                .eqIfPresent(DemoEntity::getStatus, 1)
                .eqIfPresent(DemoEntity::getCode, null)
                .likeIfPresent(DemoEntity::getName, "admin")
                .likeIfPresent(DemoEntity::getRemark, " ")
                .betweenIfPresent(DemoEntity::getCreateTime, LocalDateTime.parse("2026-05-13T00:00:00"), null)
                .inIfPresent(DemoEntity::getDeptId, List.of(1L, 2L))
                .orderByDescIfPresent(DemoEntity::getCreateTime);

        String sqlSegment = wrapper.getSqlSegment();

        assertThat(sqlSegment).contains("status");
        assertThat(sqlSegment).contains("name");
        assertThat(sqlSegment).contains("create_time");
        assertThat(sqlSegment).contains("dept_id");
        assertThat(sqlSegment).doesNotContain("code");
        assertThat(sqlSegment).doesNotContain("remark");
    }

    static class DemoEntity {

        private Integer status;
        private String code;
        private String name;
        private String remark;
        private Long deptId;
        private LocalDateTime createTime;

        public Integer getStatus() {
            return status;
        }

        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }

        public String getRemark() {
            return remark;
        }

        public Long getDeptId() {
            return deptId;
        }

        public LocalDateTime getCreateTime() {
            return createTime;
        }
    }
}
