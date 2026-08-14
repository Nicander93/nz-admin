package com.nz.admin.modules.workflow.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.nz.admin.common.core.BusinessException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 根据定义快照解析实例的下一运行节点。
 */
@Component
public class WorkflowRuntimeResolver {

    public Transition start(String modelJson, Map<String, Object> variables) {
        RuntimeModel model = parse(modelJson);
        RuntimeNode start = model.nodes().values().stream()
                .filter(node -> "start".equals(node.type()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("流程模型缺少开始节点"));
        return advance(model, start, variables);
    }

    public Transition advance(String modelJson, String currentNodeId, Map<String, Object> variables) {
        RuntimeModel model = parse(modelJson);
        RuntimeNode current = model.nodes().get(currentNodeId);
        if (current == null) {
            throw new BusinessException("当前流程节点不存在于定义快照");
        }
        return advance(model, current, variables);
    }

    private Transition advance(RuntimeModel model, RuntimeNode from, Map<String, Object> variables) {
        RuntimeNode target = from;
        int traversed = 0;
        while (true) {
            Edge edge = selectEdge(model.edges(), target.id(), variables);
            target = model.nodes().get(edge.target());
            if (target == null) {
                throw new BusinessException("流程连线目标节点不存在");
            }
            if ("task".equals(target.type())) {
                return new Transition(from, target, false);
            }
            if ("end".equals(target.type())) {
                return new Transition(from, target, true);
            }
            if ("parallel".equals(target.type())) {
                throw new BusinessException("并行网关将在多任务切片启用，当前定义暂不能运行");
            }
            if (!"exclusive".equals(target.type())) {
                throw new BusinessException("流程运行中遇到不支持的自动节点类型");
            }
            if (++traversed > model.nodes().size()) {
                throw new BusinessException("流程自动节点形成循环，无法继续运行");
            }
        }
    }

    private Edge selectEdge(List<Edge> edges, String source, Map<String, Object> variables) {
        List<Edge> outgoing = edges.stream().filter(edge -> source.equals(edge.source())).toList();
        if (outgoing.isEmpty()) {
            throw new BusinessException("当前节点没有可用的后续连线");
        }
        List<Edge> matched = outgoing.stream()
                .filter(edge -> edge.condition() != null && matches(edge.condition(), variables))
                .toList();
        if (matched.size() == 1) {
            return matched.get(0);
        }
        if (matched.size() > 1) {
            throw new BusinessException("多个条件分支同时命中，无法确定后续节点");
        }
        List<Edge> defaults = outgoing.stream().filter(edge -> edge.condition() == null).toList();
        if (defaults.size() == 1) {
            return defaults.get(0);
        }
        throw new BusinessException("没有命中的条件分支或默认分支不唯一");
    }

    private boolean matches(JSONObject condition, Map<String, Object> variables) {
        String variable = StrUtil.trim(condition.getStr("variable"));
        String operator = StrUtil.blankToDefault(StrUtil.trim(condition.getStr("operator")), "EQ").toUpperCase();
        Object actual = variables.get(variable);
        Object expected = condition.get("value");
        return switch (operator) {
            case "EQ" -> Objects.equals(normalize(actual), normalize(expected));
            case "NE" -> !Objects.equals(normalize(actual), normalize(expected));
            case "GT" -> compare(actual, expected) > 0;
            case "GE" -> compare(actual, expected) >= 0;
            case "LT" -> compare(actual, expected) < 0;
            case "LE" -> compare(actual, expected) <= 0;
            case "IN" -> expected instanceof Collection<?> collection
                    && collection.stream().map(this::normalize).anyMatch(normalize(actual)::equals);
            default -> throw new BusinessException("流程分支使用了不支持的条件操作符：" + operator);
        };
    }

    private int compare(Object actual, Object expected) {
        if (actual == null || expected == null) {
            throw new BusinessException("流程分支比较变量不能为空");
        }
        try {
            return new BigDecimal(String.valueOf(actual)).compareTo(new BigDecimal(String.valueOf(expected)));
        } catch (NumberFormatException exception) {
            return String.valueOf(actual).compareTo(String.valueOf(expected));
        }
    }

    private String normalize(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private RuntimeModel parse(String modelJson) {
        JSONObject model = JSONUtil.parseObj(modelJson);
        Map<String, RuntimeNode> nodes = new LinkedHashMap<>();
        for (Object value : model.getJSONArray("nodes")) {
            JSONObject node = JSONUtil.parseObj(value);
            String id = node.getStr("id");
            nodes.put(id, new RuntimeNode(id, StrUtil.blankToDefault(node.getStr("name"), id),
                    node.getStr("type"), node.getStr("assignee")));
        }
        List<Edge> edges = new ArrayList<>();
        JSONArray edgeArray = model.getJSONArray("edges");
        for (Object value : edgeArray) {
            JSONObject edge = JSONUtil.parseObj(value);
            edges.add(new Edge(edge.getStr("source"), edge.getStr("target"), edge.getJSONObject("condition")));
        }
        return new RuntimeModel(nodes, edges);
    }

    public record RuntimeNode(String id, String name, String type, String assignee) {
    }

    public record Transition(RuntimeNode from, RuntimeNode target, boolean completed) {
    }

    private record Edge(String source, String target, JSONObject condition) {
    }

    private record RuntimeModel(Map<String, RuntimeNode> nodes, List<Edge> edges) {
    }
}
