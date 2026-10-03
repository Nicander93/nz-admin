/** 网络错误重试使用原幂等键；成功或请求内容变更时使用新键。 */
export function withIdempotency<D, T>(
  submit: (data: D, key: string) => Promise<T>,
) {
  let pending: { fingerprint: string; key: string } | undefined
  return async (data: D): Promise<T> => {
    const fingerprint = JSON.stringify(data)
    if (!pending || pending.fingerprint !== fingerprint)
      pending = { fingerprint, key: crypto.randomUUID() }
    const attempt = pending
    const result = await submit(data, attempt.key)
    if (pending === attempt) pending = undefined
    return result
  }
}
