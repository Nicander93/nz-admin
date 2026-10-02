package com.nz.admin.framework.datascope.support;

import com.baomidou.mybatisplus.extension.plugins.handler.MultiDataPermissionHandler;
import com.nz.admin.common.core.BusinessException;
import com.nz.admin.framework.datascope.core.DataScopeContext;
import com.nz.admin.framework.datascope.core.DataScopeResolver;
import com.nz.admin.framework.datascope.core.DataScopeResult;
import com.nz.admin.framework.datascope.core.DataScopeRule;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** 为受保护表构造 SQL 表达式，避免向请求对象写入 SQL。 */
public class DataScopeHandler implements MultiDataPermissionHandler {
    private final Map<String, DataScopeRule> rules = new LinkedHashMap<>();
    private final DataScopeResolver resolver;

    public DataScopeHandler(List<DataScopeRule> rules, DataScopeResolver resolver) {
        this.resolver = resolver;
        for (DataScopeRule rule : rules) {
            if (this.rules.putIfAbsent(rule.table().toLowerCase(Locale.ROOT), rule) != null) {
                throw new IllegalArgumentException("重复的数据权限表：" + rule.table());
            }
        }
    }

    @Override
    public Expression getSqlSegment(Table table, Expression where, String statementId) {
        if (DataScopeContext.isIgnored()) {
            return null;
        }
        DataScopeRule rule = rules.get(table.getName().toLowerCase(Locale.ROOT));
        if (rule == null) {
            return null;
        }
        DataScopeResult scope = DataScopeContext.withoutFilter(resolver::resolve);
        if (scope == null) {
            throw new BusinessException("无法解析数据权限");
        }
        if (scope.all()) {
            return null;
        }

        String qualifier = table.getAlias() == null ? table.getName() : table.getAlias().getName();
        Expression condition = null;
        if (rule.deptColumn() != null && !scope.deptIds().isEmpty()) {
            InExpression departments = new InExpression();
            departments.setLeftExpression(new Column(qualifier + "." + rule.deptColumn()));
            departments.setRightExpression(
                    new ParenthesedExpressionList<>(
                            scope.deptIds().stream().sorted().map(LongValue::new).toList()));
            condition = departments;
        }
        if (rule.userColumn() != null && scope.self()) {
            Expression self =
                    new EqualsTo(
                            new Column(qualifier + "." + rule.userColumn()),
                            new LongValue(scope.userId()));
            condition = condition == null ? self : new OrExpression(condition, self);
        }
        if (condition == null) {
            return new EqualsTo(new LongValue(1), new LongValue(0));
        }
        return new ParenthesedExpressionList<>(condition);
    }
}
