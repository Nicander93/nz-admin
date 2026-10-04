# 字典、系统参数与公告

字典用于可管理的选项，系统参数用于代码明确读取的业务设置，公告用于发布通知。它们都不能替代部署环境变量。

## 创建字典选项

先创建字典类型，再添加 label、value、sort、status 等字典数据。类型是稳定的代码键，label 是显示文案，value 是提交值。修改 label 一般不会改变已保存的业务值；修改 value 会影响历史记录解释，应先核对引用。

页面可复用现有 API：

```ts
import { listDictDataByType } from '@/api/system/dict'

const response = await listDictDataByType('your_dict_type')
const options = response.data
```

把 `your_dict_type` 替换为已创建的类型。字典数据在当前租户下读取；读取下拉选项的接口仍受登录保护。业务新增、修改时应在服务端校验 value 是否允许，不能只信任页面下拉框。

## 系统参数要有读取方

新增一个参数键不会自动修改任何页面或服务。业务代码需要明确读取该键、默认值、类型转换和修改后何时生效。数据库参数、YAML 和环境变量是不同来源，不能靠名称相似推断它们互通。

配置密码、对象存储凭据或密钥时使用专门的配置机制，不在普通参数备注中存放原文。部署变量见[运维配置](/operations/configuration)。

## 公告与逐用户消息

公告适合维护通知正文和发布状态；消息中心适合对某个接收人保存未读/已读状态。审批提醒或个人通知应选择消息中心，并明确是否需要在线推送。

当前字典并没有自动把所有历史业务值同步改名或迁移；字典删除前也应由业务使用方检查引用，不能假定所有领域都有外键保护。对业务关键枚举，可以保留代码约束，字典只提供显示文案。

参考：[字典 API](https://github.com/Nicander93/nz-admin/blob/master/nz-web/src/api/system/dict.ts)、[消息中心](/message-center)。
