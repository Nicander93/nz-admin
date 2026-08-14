import type { BackendModuleStatus, FrontendModuleManifest } from './types'

const manifestModules = import.meta.glob<{ default: FrontendModuleManifest }>('/src/modules/*/manifest.ts', {
  eager: true,
})

const manifests = Object.values(manifestModules)
  .map((module) => module.default)
  .filter(Boolean)
  .sort((left, right) => {
    if (left.code === 'system') return -1
    if (right.code === 'system') return 1
    return left.code.localeCompare(right.code)
  })

export function getFrontendModuleManifests(): FrontendModuleManifest[] {
  return manifests.slice()
}

export function getEnabledFrontendModuleCodes(statuses: BackendModuleStatus[]): Set<string> {
  const enabledBackendModules = new Set(
    statuses.filter((status) => status.state === 'ENABLED').map((status) => status.frontendModule || status.code),
  )
  return new Set(manifests.filter((manifest) => enabledBackendModules.has(manifest.code)).map((manifest) => manifest.code))
}

export function getModuleCodeForComponent(componentPath?: string): string | undefined {
  if (!componentPath) return undefined
  const normalized = componentPath.replace(/^@\/views\//, '').replace(/^\/src\/views\//, '').replace(/^\/+/, '')
  return manifests.find((manifest) => normalized.startsWith(`${manifest.componentPrefix}/`))?.code
}
