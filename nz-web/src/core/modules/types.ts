export type BackendModuleState = 'ENABLED' | 'DISABLED' | 'UNAVAILABLE'

export interface BackendModuleStatus {
  code: string
  name: string
  version: string
  description?: string
  frontendModule?: string
  state: BackendModuleState
}

export interface FrontendModuleManifest {
  code: string
  title: string
  componentPrefix: string
}
