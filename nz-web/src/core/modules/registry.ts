import systemModule from '@/modules/system/manifest'
import jobModule from '@/modules/job/manifest'
import type { BackendModuleStatus, FrontendModuleManifest } from './types'

const manifests: FrontendModuleManifest[] = [systemModule, jobModule]

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
