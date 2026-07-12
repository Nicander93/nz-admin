import { describe, expect, it } from 'vitest'
import {
  getEnabledFrontendModuleCodes,
  getFrontendModuleManifests,
  getModuleCodeForComponent,
} from '@/core/modules/registry'

describe('module registry', () => {
  it('only enables manifests reported as enabled by the backend', () => {
    const codes = getEnabledFrontendModuleCodes([
      { code: 'system', name: 'System', version: '1', frontendModule: 'system', state: 'ENABLED' },
      { code: 'job', name: 'Job', version: '1', frontendModule: 'job', state: 'DISABLED' },
    ])

    expect(getFrontendModuleManifests().map((item) => item.code)).toEqual(['system', 'job'])
    expect(codes).toEqual(new Set(['system']))
  })

  it('maps modular view paths while keeping legacy paths compatible', () => {
    expect(getModuleCodeForComponent('@/views/system/user/index.vue')).toBe('system')
    expect(getModuleCodeForComponent('@/views/job/index.vue')).toBe('job')
    expect(getModuleCodeForComponent('@/views/notice/index.vue')).toBeUndefined()
  })
})
