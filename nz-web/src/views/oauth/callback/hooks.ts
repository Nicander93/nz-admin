export type SocialCallbackInput = {
  provider: string
  code: string
  state: string
}

export type SocialCallbackQueryResult =
  | { ok: true; data: SocialCallbackInput }
  | { ok: false; error: string }

export function parseSocialCallbackQuery(
  providerValue: unknown,
  query: Record<string, unknown>,
): SocialCallbackQueryResult {
  const provider = String(providerValue || '')
  const providerError = String(query.error_description || query.error || '')
  if (providerError) {
    return { ok: false, error: providerError }
  }
  const code = String(query.code || '')
  const state = String(query.state || '')
  if (!provider || !code || !state) {
    return {
      ok: false,
      error: '授权回调参数不完整，请重新发起授权。',
    }
  }
  return { ok: true, data: { provider, code, state } }
}
