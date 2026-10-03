import type { UserConfig } from 'vite'
import type { PlaywrightTestConfig } from '@playwright/test'

export interface BrowserTestOptions {
  baseURL?: string
  serverCommand?: string
}
export function unitTestConfig(root: string): UserConfig
export function browserTestConfig(
  options?: BrowserTestOptions,
): PlaywrightTestConfig
