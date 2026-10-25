import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'

import AuthUnavailable from '@/views/auth-unavailable/index.vue'

const mocks = vi.hoisted(() => ({
  replace: vi.fn(),
  logout: vi.fn(),
}))

vi.mock('vue-router', () => ({ useRouter: () => ({ replace: mocks.replace }) }))
vi.mock('@/stores/user', () => ({ useUserStore: () => ({ logout: mocks.logout }) }))

const ButtonStub = {
  template: `<button @click="$emit('click')"><slot /></button>`,
  emits: ['click'],
}

describe('auth unavailable page', () => {
  it('shows a static message and retries without clearing the token', async () => {
    const wrapper = mount(AuthUnavailable, {
      global: {
        stubs: {
          ElResult: {
            props: ['title', 'subTitle'],
            template: '<section><h1>{{ title }}</h1><p>{{ subTitle }}</p><slot name="extra" /></section>',
          },
          ElButton: ButtonStub,
        },
      },
    })

    expect(wrapper.text()).toContain('暂时无法加载账户信息')
    expect(wrapper.text()).not.toContain('管理员')
    await wrapper.get('button').trigger('click')

    expect(mocks.replace).toHaveBeenCalledWith('/')
    expect(mocks.logout).not.toHaveBeenCalled()
  })

  it('clears identity only after the user chooses to sign in again', async () => {
    const wrapper = mount(AuthUnavailable, {
      global: {
        stubs: {
          ElResult: {
            props: ['title', 'subTitle'],
            template: '<section><h1>{{ title }}</h1><p>{{ subTitle }}</p><slot name="extra" /></section>',
          },
          ElButton: ButtonStub,
        },
      },
    })

    await wrapper.get('button:nth-of-type(2)').trigger('click')

    expect(mocks.logout).toHaveBeenCalledOnce()
    expect(mocks.replace).toHaveBeenCalledWith('/login')
  })
})
