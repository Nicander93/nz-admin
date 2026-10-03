package com.nz.admin.modules.workflow.engine;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

/** 只允许声明式节点和条件，身份、审计字段与执行监听器由服务端管理。 */
public record EngineDefinitionRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z][A-Za-z0-9_-]{0,63}") String flowCode,
        @NotBlank @Size(max = 100) String flowName,
        @NotEmpty @Size(max = 100) List<@Valid Node> nodeList) {
    public record Node(
            @NotBlank @Pattern(regexp = "[A-Za-z][A-Za-z0-9_-]{0,63}") String nodeCode,
            @NotBlank @Size(max = 100) String nodeName,
            @NotNull @Min(0) @Max(5) Integer nodeType,
            @Size(max = 2000)
                    @Pattern(
                            regexp =
                                    "(?:(?:user:)?[1-9][0-9]*|role:[A-Za-z][A-Za-z0-9_-]*)(?:,(?:(?:user:)?[1-9][0-9]*|role:[A-Za-z][A-Za-z0-9_-]*))*")
                    String permissionFlag,
            @Pattern(regexp = "[0-9]{1,3}(\\.[0-9]+)?") String nodeRatio,
            @Size(max = 100) List<@Valid Transition> skipList) {}

    public record Transition(
            @NotBlank @Pattern(regexp = "[A-Za-z][A-Za-z0-9_-]{0,63}") String nextNodeCode,
            @Pattern(regexp = "PASS|REJECT") String skipType,
            @Size(max = 256) String skipCondition) {}
}
