package com.nz.admin.framework.realtime.core;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.IOException;
import java.util.UUID;

/** Redis 跨节点消息与退出通知；连接本身仍由各节点管理。 */
public class RedisRealtimePublisher
        implements RealtimePublisher, RealtimeConnectionManager, MessageListener {
    private final RealtimeConnectionRegistry local;
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final String channel;
    private final String node = UUID.randomUUID().toString();

    public RedisRealtimePublisher(
            RealtimeConnectionRegistry local,
            StringRedisTemplate redis,
            ObjectMapper mapper,
            String channel) {
        this.local = local;
        this.redis = redis;
        this.mapper = mapper;
        this.channel = channel;
    }

    private void publish(String kind, Long tenant, Long user, RealtimeMessage message) {
        try {
            redis.convertAndSend(
                    channel,
                    mapper.writeValueAsString(new Envelope(node, kind, tenant, user, message)));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("实时消息无法序列化", e);
        }
    }

    public int publishToUser(Long tenant, Long user, RealtimeMessage message) {
        publish("user", tenant, user, message);
        return local.publishToUser(tenant, user, message);
    }

    public int publishToTenant(Long tenant, RealtimeMessage message) {
        publish("tenant", tenant, null, message);
        return local.publishToTenant(tenant, message);
    }

    public int broadcast(RealtimeMessage message) {
        publish("broadcast", null, null, message);
        return local.broadcast(message);
    }

    public int disconnectUser(Long tenant, Long user) {
        publish("disconnect", tenant, user, null);
        return local.disconnectUser(tenant, user);
    }

    public RealtimeConnectionStats stats() {
        return local.stats();
    }

    public void onMessage(Message message, byte[] pattern) {
        try {
            var envelope = mapper.readValue(message.getBody(), Envelope.class);
            if (node.equals(envelope.node())) return;
            switch (envelope.kind()) {
                case "user" ->
                        local.publishToUser(envelope.tenant(), envelope.user(), envelope.message());
                case "tenant" -> local.publishToTenant(envelope.tenant(), envelope.message());
                case "broadcast" -> local.broadcast(envelope.message());
                case "disconnect" -> local.disconnectUser(envelope.tenant(), envelope.user());
                default -> throw new IllegalArgumentException("未知实时消息类型");
            }
        } catch (IOException e) {
            throw new IllegalStateException("实时消息无法读取", e);
        }
    }

    public record Envelope(
            String node, String kind, Long tenant, Long user, RealtimeMessage message) {}
}
