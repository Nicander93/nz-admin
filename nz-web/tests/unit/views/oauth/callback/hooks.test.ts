import { describe, expect, it } from 'vitest'
import { parseSocialCallbackQuery } from '@/views/oauth/callback/hooks'

describe('parseSocialCallbackQuery', () => {
  it('returns trusted callback input when all fields exist', () => {
    expect(parseSocialCallbackQuery('github', {
      code: 'code',
      state: 'state',
    })).toEqual({
      ok: true,
      data: { provider: 'github', code: 'code', state: 'state' },
    })
  })

  it('keeps provider error for user feedback', () => {
    expect(parseSocialCallbackQuery('github', {
      error: 'access_denied',
    })).toEqual({ ok: false, error: 'access_denied' })
  })

  it('rejects incomplete callbacks', () => {
    expect(parseSocialCallbackQuery('github', {
      code: 'code',
    })).toEqual({
      ok: false,
      error: '授权回调参数不完整，请重新发起授权。',
    })
  })
})
