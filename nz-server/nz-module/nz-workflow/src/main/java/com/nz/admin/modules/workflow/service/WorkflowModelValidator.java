package com.nz.admin.modules.workflow.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONException;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.nz.admin.common.core.BusinessException;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 流程模型结构校验器。
 */
@Component
public class WorkflowModelValidator {

    private static final Set<String> NODE_TYPES = Set.of("start", "task", "exclusive", "parallel", "end");

    public String normalizeAndValidate(String modelJson) {
        try {
            JSONObject model = JSONUtil.parseObj(modelJson);
            JSONArray nodes = model.getJSONArray("nodes");
            JSONArray edges = model.getJSONArray("edges");
            if (nodes == null || nodes.isEmpty() || edges == null || edges.isEmpty()) {
                throw new BusinessException("流程模型必须包含节点和连线");
            }

            Set<String> nodeIds = new HashSet<>();
            Map<String, Set<String>> forward = new HashMap<>();
            Map<String, Set<String>> reverse = new HashMap<>();
            String startId = null;
            String endId = null;
            int startCount = 0;
            int endCount = 0;

            for (Object value : nodes) {
                JSONObject node = JSONUtil.parseObj(value);
                String nodeId = StrUtil.trim(node.getStr("id"));
                String nodeType = StrUtil.trim(node.getStr("type"));
                if (StrUtil.isBlank(nodeId) || !NODE_TYPES.contains(nodeType)) {
                    throw new BusinessException("流程节点缺少编号或类型不支持");
                }
                if (!nodeIds.add(nodeId)) {
                    throw new BusinessException("流程节点编号不能重复");
                }
                String assignee = StrUtil.trim(node.getStr("assignee"));
                if ("task".equals(nodeType) && !isValidAssignee(assignee)) {
                    throw new BusinessException("审批节点办理人只支持 initiator、user:<id> 或 role:<roleKey>");
                }
                if ("start".equals(nodeType)) {
                    startId = nodeId;
                    startCount++;
                } else if ("end".equals(nodeType)) {
                    endId = nodeId;
                    endCount++;
                }
                forward.put(nodeId, new HashSet<>());
                reverse.put(nodeId, new HashSet<>());
            }
            if (startCount != 1 || endCount != 1) {
                throw new BusinessException("流程模型必须且只能包含一个开始节点和一个结束节点");
            }

            for (Object value : edges) {
                JSONObject edge = JSONUtil.parseObj(value);
                String source = StrUtil.trim(edge.getStr("source"));
                String target = StrUtil.trim(edge.getStr("target"));
                if (!nodeIds.contains(source) || !nodeIds.contains(target) || source.equals(target)) {
                    throw new BusinessException("流程连线引用了无效节点");
                }
                forward.get(source).add(target);
                reverse.get(target).add(source);
            }
            if (!walk(startId, forward).equals(nodeIds) || !walk(endId, reverse).equals(nodeIds)) {
                throw new BusinessException("流程模型存在不可达节点或无法到达结束节点");
            }
            return JSONUtil.toJsonStr(model);
        } catch (JSONException | IllegalArgumentException exception) {
            if (exception instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException("流程模型不是有效的 JSON");
        }
    }

    private boolean isValidAssignee(String assignee) {
        return "initiator".equals(assignee)
                || assignee != null && assignee.matches("user:[1-9]\\d*")
                || assignee != null && assignee.matches("role:[A-Za-z0-9_-]+");
    }

    private Set<String> walk(String start, Map<String, Set<String>> graph) {
        Set<String> visited = new HashSet<>();
        ArrayDeque<String> queue = new ArrayDeque<>();
        queue.add(start);
        while (!queue.isEmpty()) {
            String node = queue.removeFirst();
            if (visited.add(node)) {
                queue.addAll(graph.getOrDefault(node, Set.of()));
            }
        }
        return visited;
    }
}
