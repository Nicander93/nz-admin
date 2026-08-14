export const ACCOUNT_CLIENT_ID = 'nz-web-account'
export const SMS_CLIENT_ID = 'nz-web-sms'
export const SOCIAL_CLIENT_ID = 'nz-web-social'

export type LoginMode = 'account' | 'sms'

export type LoginForm = {
  tenantCode: string
  username: string
  password: string
  phone: string
  code: string
}

export function validateLoginForm(mode: LoginMode, form: LoginForm): string | null {
  if (!form.tenantCode.trim()) {
    return '请输入租户编码'
  }
  if (mode === 'account') {
    if (!form.username.trim()) {
      return '请输入用户名'
    }
    if (!form.password) {
      return '请输入密码'
    }
    return null
  }
  if (!/^\+?[1-9]\d{7,14}$/.test(form.phone.trim())) {
    return '请输入正确的手机号'
  }
  if (!/^\d{4,8}$/.test(form.code)) {
    return '请输入正确的验证码'
  }
  return null
}

export function validateSmsCodeRequest(form: LoginForm): string | null {
  if (!form.tenantCode.trim()) {
    return '请输入租户编码'
  }
  if (!/^\+?[1-9]\d{7,14}$/.test(form.phone.trim())) {
    return '请输入正确的手机号'
  }
  return null
}

export function accountLoginPayload(form: LoginForm) {
  return {
    tenantCode: form.tenantCode.trim(),
    clientId: ACCOUNT_CLIENT_ID,
    username: form.username.trim(),
    password: form.password,
  }
}

export function smsLoginPayload(form: LoginForm) {
  return {
    tenantCode: form.tenantCode.trim(),
    clientId: SMS_CLIENT_ID,
    phone: form.phone.trim(),
    code: form.code,
  }
}
