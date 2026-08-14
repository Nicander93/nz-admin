import { describe, expect, it } from 'vitest'
import {
  ACCOUNT_CLIENT_ID,
  SMS_CLIENT_ID,
  accountLoginPayload,
  smsLoginPayload,
  validateLoginForm,
  validateSmsCodeRequest,
  type LoginForm,
} from '@/views/login/hooks'

const form = (overrides: Partial<LoginForm> = {}): LoginForm => ({
  tenantCode: ' default ',
  username: ' admin ',
  password: 'secret',
  phone: '13800138000',
  code: '123456',
  ...overrides,
})

describe('login hooks', () => {
  it('builds client-bound account and sms payloads', () => {
    expect(accountLoginPayload(form())).toEqual({
      tenantCode: 'default',
      clientId: ACCOUNT_CLIENT_ID,
      username: 'admin',
      password: 'secret',
    })
    expect(smsLoginPayload(form())).toEqual({
      tenantCode: 'default',
      clientId: SMS_CLIENT_ID,
      phone: '13800138000',
      code: '123456',
    })
  })

  it('validates each login mode independently', () => {
    expect(validateLoginForm('account', form({ username: '' }))).toBe('请输入用户名')
    expect(validateLoginForm('sms', form({ phone: '123' }))).toBe('请输入正确的手机号')
    expect(validateLoginForm('sms', form({ code: '12' }))).toBe('请输入正确的验证码')
    expect(validateLoginForm('account', form())).toBeNull()
    expect(validateLoginForm('sms', form())).toBeNull()
  })

  it('allows requesting a code before entering the code itself', () => {
    expect(validateSmsCodeRequest(form({ code: '' }))).toBeNull()
    expect(validateSmsCodeRequest(form({ tenantCode: '' }))).toBe('请输入租户编码')
  })
})
