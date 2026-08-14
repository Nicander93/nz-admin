import { describe, expect, it } from 'vitest'
import { DEFAULT_WORKFLOW_MODEL, formatModelJson } from '@/views/workflow/definition/hooks'

describe('workflow definition model helpers', () => {
  it('provides a connected start-task-end model', () => {
    const model = JSON.parse(DEFAULT_WORKFLOW_MODEL)

    expect(model.nodes.map((node: { type: string }) => node.type)).toEqual(['start', 'task', 'end'])
    expect(model.nodes[1].assignee).toBe('role:manager')
    expect(model.edges).toHaveLength(2)
  })

  it('formats valid JSON and rejects invalid input', () => {
    expect(formatModelJson('{"nodes":[],"edges":[]}')).toContain('\n  "nodes"')
    expect(() => formatModelJson('{invalid')).toThrow()
  })
})
