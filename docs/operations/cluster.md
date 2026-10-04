# 双节点配置与一致性验收

多节点首先要共享状态来源。负载均衡能把请求分发到两个节点，但不能自动让登录态、请求保护和实时票据一致。

## 节点配置

两个应用使用同一个 PostgreSQL、同一个 Redis 数据库和相同 `NZ_CACHE_KEY_PREFIX`，开启：

```dotenv
NZ_CLUSTER_ENABLED=true
NZ_REDIS_ENABLED=true
NZ_WARM_FLOW_ENABLED=true
NZ_CACHE_KEY_PREFIX=nz-admin-cluster
```

同机测试可分别监听 8080 和 8081；生产由网关转发。不同环境使用不同前缀。共享数据库完成 V30；文件使用 OSS/S3 或共享文件系统。

## 已共享的状态

登录会话、接口频控、验证码、OAuth state、实时连接票据与撤销版本使用共享状态。业务幂等、定义版本/发布、审批和事件投递通过共享数据库事务与锁保持一致。

实时消息使用 Redis Pub/Sub 跨节点转发，它不提供离线可靠投递。消息中心数据库保存历史；在线连接数和投递计数仍是节点值，不能将单节点结果当全集群指标。

## 真实两进程验证

启动两个连接同一独立测试库的 Java 进程后，在仓库根目录执行：

```bash
E2E_USERNAME=admin E2E_PASSWORD=admin123 node tools/tests/workflow-cluster.mjs
```

脚本检查：A 登录、B 识别会话；并发建版本和发布只有一个当前发布版；不同键重复提交只启动一次；同键跨节点审批只保留一次成功轨迹；票据在另一节点消费且不能重放；消息跨节点到达；退出关闭连接并撤销会话。

使用独立测试库和账号。后端故障注入另用一个库，避免真实节点提前消费故障事件。脚本默认端口及变量以源码为准，自定义端口时先核对脚本参数。

## 不在该验收中的能力

负载均衡故障转移、离线消息队列、分布式定时任务和全局监控需要单独验证。Redis 故障时共享状态拒绝继续处理，不能回退本地状态或默认租户“维持可用”。

参考：[跨节点脚本](https://github.com/Nicander93/nz-admin/blob/master/tools/tests/workflow-cluster.mjs)、[生产部署](/deployment)、[实时通信](/realtime-communication)。
