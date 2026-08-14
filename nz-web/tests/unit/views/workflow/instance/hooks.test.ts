import { describe, expect, it } from 'vitest'
import {
  buildStartPayload,
  formatVariables,
  instanceEventText,
  instanceStatusText,
} from '@/views/workflow/instance/hooks'

describe('workflow instance helpers', () => {
  it('maps runtime statuses and events to readable labels', () => {
    expect(instanceStatusText('RUNNING')).toBe('运行中')
    expect(instanceStatusText('COMPLETED')).toBe('已完成')
    expect(instanceEventText('APPROVE')).toBe('同意')
    expect(instanceEventText('CUSTOM')).toBe('CUSTOM')
  })

  it('builds a typed start payload from JSON variables', () => {
    expect(buildStartPayload({
      flowCode: ' leave_apply ',
      businessKey: ' LEAVE-001 ',
      title: ' 请假审批 ',
      variablesJson: '{"days":2,"urgent":true}',
    })).toEqual({
      flowCode: 'leave_apply',
      businessKey: 'LEAVE-001',
      title: '请假审批',
      variables: { days: 2, urgent: true },
    })
  })

  it('rejects invalid variables and formats persisted JSON', () => {
    expect(() => buildStartPayload({
      flowCode: 'leave_apply',
      businessKey: 'LEAVE-001',
      title: '请假审批',
      variablesJson: '{invalid',
    })).toThrow()
    expect(formatVariables('{"days":2}')).toContain('\n  "days": 2')
    expect(formatVariables('{invalid')).toBe('{invalid')
  })
})
