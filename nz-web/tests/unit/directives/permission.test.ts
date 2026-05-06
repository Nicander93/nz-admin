import type { DirectiveBinding } from 'vue'
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'

const mockHasPermission = vi.fn<(perm: string) => boolean>()

vi.mock('@/stores/user', () => ({
  useUserStore: () => ({
    hasPermission: mockHasPermission,
  }),
}))

import { permissionDirective } from '@/directives/permission'

type PermissionValue = string | string[]

function createBinding(value: PermissionValue): DirectiveBinding<PermissionValue> {
  return {
    instance: null,
    value,
    oldValue: undefined,
    arg: undefined,
    modifiers: {},
    dir: permissionDirective,
  } as DirectiveBinding<PermissionValue>
}

describe('permissionDirective', () => {
  beforeEach(() => {
    document.body.innerHTML = ''
    mockHasPermission.mockReset()
  })

  afterEach(() => {
    document.body.innerHTML = ''
  })

  it('绑定字符串权限：有权限则显示', () => {
    mockHasPermission.mockReturnValue(true)
    const parent = document.createElement('div')
    const el = document.createElement('button')
    parent.appendChild(el)

    permissionDirective.mounted?.(el, createBinding('system:user:list'))

    expect(parent.contains(el)).toBe(true)
  })

  it('绑定字符串权限：无权限则移除元素', () => {
    mockHasPermission.mockReturnValue(false)
    const parent = document.createElement('div')
    const el = document.createElement('button')
    parent.appendChild(el)

    permissionDirective.mounted?.(el, createBinding('system:user:list'))

    expect(parent.contains(el)).toBe(false)
  })

  it('绑定数组权限：满足任一权限则显示', () => {
    mockHasPermission.mockImplementation((perm) => perm === 'system:user:list')
    const parent = document.createElement('div')
    const el = document.createElement('button')
    parent.appendChild(el)

    permissionDirective.mounted?.(el, createBinding(['system:user:create', 'system:user:list']))

    expect(parent.contains(el)).toBe(true)
  })

  it('更新权限时：从有权限变无权限会隐藏元素', () => {
    mockHasPermission.mockReturnValue(true)
    const parent = document.createElement('div')
    const el = document.createElement('button')
    parent.appendChild(el)

    permissionDirective.mounted?.(el, createBinding('system:user:list'))
    expect(parent.contains(el)).toBe(true)

    mockHasPermission.mockReturnValue(false)
    permissionDirective.updated?.(el, createBinding('system:user:list'))

    expect(parent.contains(el)).toBe(false)
  })

  it('更新权限时：有权限仍保持显示', () => {
    mockHasPermission.mockReturnValue(true)
    const parent = document.createElement('div')
    const el = document.createElement('button')
    parent.appendChild(el)

    permissionDirective.mounted?.(el, createBinding('system:user:list'))
    permissionDirective.updated?.(el, createBinding('system:user:list'))

    expect(parent.contains(el)).toBe(true)
  })
})
