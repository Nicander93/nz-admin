package com.nz.admin.framework.protection.core;

import cn.hutool.crypto.digest.DigestUtil;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import org.springframework.validation.BindingResult;

import java.util.Arrays;

/** JSON 对象键排序后计算摘要，字段顺序变化不会被误判成另一个请求。 */
public final class RequestFingerprint {
    private static final JsonMapper JSON =
            JsonMapper.builder()
                    .findAndAddModules()
                    .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                    .build();

    private RequestFingerprint() {}

    public static String of(Object[] arguments) {
        Object[] body =
                Arrays.stream(arguments)
                        .filter(
                                arg ->
                                        !(arg instanceof ServletRequest)
                                                && !(arg instanceof ServletResponse)
                                                && !(arg instanceof BindingResult))
                        .toArray();
        try {
            return DigestUtil.sha256Hex(JSON.writeValueAsBytes(body));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("请求参数无法生成幂等摘要", e);
        }
    }
}
