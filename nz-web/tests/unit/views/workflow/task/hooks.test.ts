import { describe, expect, it } from 'vitest'
import { taskActionText, uniqueReceiverIds } from '@/views/workflow/task/hooks'

describe('workflow task helpers', () => {
  it('maps task actions to readable labels', () => {
    expect(taskActionText('APPROVE')).toBe('同意')
    expect(taskActionText('TRANSFER')).toBe('转办')
    expect(taskActionText('DELEGATE')).toBe('委派')
    expect(taskActionText('RESOLVE')).toBe('完成委派')
    expect(taskActionText()).toBe('-')
  })

  it('deduplicates receivers and removes invalid or current users', () => {
    expect(uniqueReceiverIds([2, 3, 2, 0, -1, 7], 7)).toEqual([2, 3])
  })
})
