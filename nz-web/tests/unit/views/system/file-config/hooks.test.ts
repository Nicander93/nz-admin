import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useFileConfig } from '@/views/system/file-config/hooks'
import type { SysFileConfig } from '@/api/system/fileConfig'

vi.mock('element-plus', () => ({ ElMessage: { success: vi.fn() } }))

const row: SysFileConfig = {
  id: 1,
  configName: '生产 OSS',
  storageType: 'oss',
  endpoint: 'https://oss.example.com',
  accessKeyIdMasked: 'abc***xyz',
  accessKeySecretConfigured: true,
  bucketName: 'bucket',
  maxFileSizeBytes: 104857600,
  status: 1,
}

describe('useFileConfig', () => {
  const page = vi.fn()
  const add = vi.fn()
  const update = vi.fn()
  const remove = vi.fn()
  const activate = vi.fn()
  const test = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()
    page.mockResolvedValue({
      data: { records: [row], total: 1, size: 10, current: 1, pages: 1 },
    })
    add.mockResolvedValue({})
    update.mockResolvedValue({})
    remove.mockResolvedValue({})
    activate.mockResolvedValue({})
    test.mockResolvedValue({})
  })

  it('加载配置列表并保持密钥不可回填', async () => {
    const state = useFileConfig({
      page,
      add,
      update,
      remove,
      activate,
      test,
    } as never)
    await state.load()
    expect(state.data.value).toEqual([row])

    state.openEdit(row)
    expect(state.form.accessKeyId).toBe('')
    expect(state.form.accessKeySecret).toBe('')
  })

  it('新增配置后刷新列表', async () => {
    const state = useFileConfig({
      page,
      add,
      update,
      remove,
      activate,
      test,
    } as never)
    state.openAdd()
    state.form.configName = '本地配置'
    await state.submit()

    expect(add).toHaveBeenCalledWith(
      expect.objectContaining({
        configName: '本地配置',
        storageType: 'local',
      }),
    )
    expect(page).toHaveBeenCalled()
  })

  it('启用配置后刷新状态', async () => {
    const state = useFileConfig({
      page,
      add,
      update,
      remove,
      activate,
      test,
    } as never)
    await state.activate(1)
    expect(activate).toHaveBeenCalledWith(1)
    expect(page).toHaveBeenCalled()
  })

it('测试当前存储连接', async () => {
  const state = useFileConfig({
    page,
    add,
    update,
    remove,
    activate,
    test,
  } as never)
  await state.testConnection(1)
  expect(test).toHaveBeenCalledWith(1)
  expect(page).not.toHaveBeenCalled()
})
})
