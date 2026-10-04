# 请求、下载与错误处理

业务 API 使用 `src/api/request.ts`，统一添加令牌并处理 `R<T>`。新页面不再创建独立 Axios 实例，以免漏掉认证和错误行为。

## 普通请求保留响应外壳

```ts
import request from '@/api/request'
import type { PageResult } from '@/api/types'

interface Item {
  id: number
  name: string
}

export function pageItems(pageNum = 1) {
  return request.get<PageResult<Item>>('/api/demo/item/page', {
    params: { pageNum, pageSize: 10 },
  })
}
```

调用方取 `response.data.records`。当前封装返回 `R<T>`，不是直接返回 data，也不是原始 AxiosResponse。GET 查询条件通过 params 传入，不拼接未经编码的业务文本。

## 统一业务错误

业务码不是 200 时，封装显示消息并 reject；业务码 401 时清理令牌和设计器认证副本，转向登录页。网络/HTTP 错误会走 Axios 错误分支，不能假定与 JSON 业务错误完全相同。

hook 用 finally 恢复 loading。失败保留表单供用户修正，不把失败响应当成功记录。全局已提示的普通错误不必再次重复弹窗；需要更具体处理时保持错误含义。

## 下载的返回值不同

`download` 发起 POST，`getBlob` 发起 GET，返回含 Blob 的 AxiosResponse。文件接口返回 JSON 错误时，封装按内容类型解析；普通成功下载不能按 `R<T>` 取 data.records。

页面负责 Blob URL 的创建、下载文件名和使用后的释放。文件访问地址仍受服务端配置与授权约束，不将对象存储密钥下发浏览器。

## 重试关键提交

创建和审批提交可通过请求 config 传入 `Idempotency-Key`。一次业务尝试的网络重试复用同一键；成功后新操作换键。不要给所有 GET 或任意写请求自动无限重试，尤其是不能保证幂等的外部操作。

详见[幂等机制](/guide/backend/protection)。源文件：[request.ts](https://github.com/Nicander93/nz-admin/blob/master/nz-web/src/api/request.ts)。
