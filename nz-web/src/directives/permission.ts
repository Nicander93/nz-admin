import type { Directive, DirectiveBinding } from 'vue'
import { useUserStore } from '@/stores/user'

type PermissionValue = string | string[]

function hasPermission(binding: DirectiveBinding<PermissionValue>): boolean {
  const value = binding.value
  if (!value) return true
  const userStore = useUserStore()
  const required = Array.isArray(value) ? value : [value]
  return required.some((perm) => userStore.hasPermission(perm))
}

function removeDom(el: HTMLElement) {
  const parent = el.parentNode
  if (parent) {
    parent.removeChild(el)
  }
}

export const permissionDirective: Directive<HTMLElement, PermissionValue> = {
  mounted(el, binding) {
    if (!hasPermission(binding)) {
      removeDom(el)
    }
  },
  updated(el, binding) {
    if (!hasPermission(binding)) {
      removeDom(el)
    }
  },
}

