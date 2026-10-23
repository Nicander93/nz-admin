import { ElMessage } from 'element-plus'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import {
  downloadDemoItemTemplate,
  importDemoItems,
  previewDemoItemImport,
} from '@/api/demo/item'
import {
  getFrontendModuleManifests,
  getModuleCodeForComponent,
} from '@/core/modules/registry'
import { useDemoItemCrud, useDemoItemExcel } from '@/views/demo/item/hooks'

vi.mock('element-plus', () => ({
  ElMessage: { error: vi.fn(), warning: vi.fn(), success: vi.fn() },
}))

vi.mock('@/api/demo/item', () => ({
  pageDemoItems: vi.fn().mockResolvedValue({
    code: 200,
    data: {
      records: [
        {
          id: 1,
          name: '模块化示例',
          category: 'architecture',
          status: 0,
          sort: 10,
        },
      ],
      total: 1,
    },
  }),
  addDemoItem: vi.fn().mockResolvedValue({ code: 200 }),
  updateDemoItem: vi.fn().mockResolvedValue({ code: 200 }),
  deleteDemoItem: vi.fn().mockResolvedValue({ code: 200 }),
  downloadDemoItemTemplate: vi.fn().mockResolvedValue({ data: new Blob() }),
  exportDemoItems: vi.fn().mockResolvedValue({ data: new Blob() }),
  importDemoItems: vi.fn().mockResolvedValue({ code: 200, data: 1 }),
  previewDemoItemImport: vi.fn(),
}))

describe('demo module', () => {
  it('is discovered by the frontend module registry', () => {
    expect(getFrontendModuleManifests().map((item) => item.code)).toContain(
      'demo',
    )
    expect(getModuleCodeForComponent('@/views/demo/item/index.vue')).toBe(
      'demo',
    )
  })

  it('loads demo item page data', async () => {
    const { table } = useDemoItemCrud()
    await table.refresh()

    expect(table.data).toHaveLength(1)
    expect(table.data[0].name).toBe('模块化示例')
    expect(table.pagination.total).toBe(1)
  })

  it('opens a complete default form', () => {
    const { form } = useDemoItemCrud()
    form.openAdd()

    expect(form.visible).toBe(true)
    expect(form.model.category).toBe('general')
    expect(form.model.status).toBe(0)
    expect(form.model.sort).toBe(10)
  })
})

type Deferred<T> = { promise: Promise<T>; resolve: (value: T) => void }
function deferred<T>(): Deferred<T> {
  let resolve!: (value: T) => void
  const promise = new Promise<T>((done) => {
    resolve = done
  })
  return { promise, resolve }
}

const validPreview = (name: string) => ({
  code: 200,
  data: {
    total: 1,
    validCount: 1,
    rows: [{ name, category: 'general', status: 0, sort: 10, remark: '' }],
    errors: [],
  },
})

describe('demo item Excel', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })
  it('ignores out-of-order and closed-dialog preview responses', async () => {
    const first = deferred<ReturnType<typeof validPreview>>()
    const second = deferred<ReturnType<typeof validPreview>>()
    const stale = deferred<ReturnType<typeof validPreview>>()
    vi.mocked(previewDemoItemImport)
      .mockReturnValueOnce(first.promise)
      .mockReturnValueOnce(second.promise)
      .mockReturnValueOnce(stale.promise)
    const excel = useDemoItemExcel(
      { name: '', category: '', status: undefined },
      vi.fn(),
    )
    excel.openImport()
    const requestA = excel.selectFile({ raw: new File(['a'], 'a.xlsx') })
    const requestB = excel.selectFile({ raw: new File(['b'], 'b.xlsx') })
    first.resolve(validPreview('stale A'))
    await requestA
    expect(excel.preview.value).toBeUndefined()
    expect(excel.selectedFile.value?.name).toBe('b.xlsx')
    second.resolve(validPreview('current B'))
    await requestB
    expect(excel.preview.value?.rows[0].name).toBe('current B')

    const lateRequest = excel.selectFile({ raw: new File(['c'], 'c.xlsx') })
    excel.closeImport()
    excel.openImport()
    stale.resolve(validPreview('closed dialog'))
    await lateRequest
    expect(excel.preview.value).toBeUndefined()
    expect(excel.selectedFile.value).toBeUndefined()
  })

  it('reuses a failed request key, rotates it after success and blocks duplicate submits', async () => {
    const keys = vi
      .spyOn(crypto, 'randomUUID')
      .mockReturnValueOnce('00000000-0000-4000-8000-000000000001')
      .mockReturnValueOnce('00000000-0000-4000-8000-000000000002')
    const imports = vi.mocked(importDemoItems)
    imports.mockRejectedValueOnce(new Error('temporary failure'))
    const pending = deferred<{ code: number; data: number }>()
    imports.mockReturnValueOnce(pending.promise)
    const refresh = vi.fn().mockResolvedValue(undefined)
    const excel = useDemoItemExcel(
      { name: '', category: '', status: undefined },
      refresh,
    )
    const batch = validPreview('batch').data
    excel.openImport()
    excel.preview.value = batch

    await excel.confirmImport()
    expect(imports).toHaveBeenNthCalledWith(
      1,
      batch.rows,
      '00000000-0000-4000-8000-000000000001',
    )

    const firstSubmit = excel.confirmImport()
    const duplicateSubmit = excel.confirmImport()
    expect(imports).toHaveBeenCalledTimes(2)
    let closeCalled = false
    excel.beforeClose(() => {
      closeCalled = true
    })
    expect(closeCalled).toBe(false)
    pending.resolve({ code: 200, data: 1 })
    await Promise.all([firstSubmit, duplicateSubmit])
    expect(imports).toHaveBeenNthCalledWith(
      2,
      batch.rows,
      '00000000-0000-4000-8000-000000000001',
    )
    expect(excel.importVisible.value).toBe(false)

    excel.openImport()
    excel.preview.value = batch
    await excel.confirmImport()
    expect(imports).toHaveBeenNthCalledWith(
      3,
      batch.rows,
      '00000000-0000-4000-8000-000000000002',
    )
    expect(refresh).toHaveBeenCalledTimes(2)
    keys.mockRestore()
  })

  it('invalid file selection clears the previous preview immediately', async () => {
    const validRequest = deferred<ReturnType<typeof validPreview>>()
    vi.mocked(previewDemoItemImport).mockReturnValueOnce(validRequest.promise)
    const excel = useDemoItemExcel(
      { name: '', category: '', status: undefined },
      vi.fn(),
    )
    excel.openImport()
    const pending = excel.selectFile({ raw: new File(['valid'], 'valid.xlsx') })
    expect(excel.selectedFile.value?.name).toBe('valid.xlsx')
    await excel.selectFile({ raw: new File(['invalid'], 'invalid.csv') })
    expect(excel.selectedFile.value).toBeUndefined()
    expect(excel.preview.value).toBeUndefined()
    validRequest.resolve(validPreview('stale'))
    await pending
    expect(excel.preview.value).toBeUndefined()
  })

  it('keeps a successful import successful if list refresh fails', async () => {
    const excel = useDemoItemExcel(
      {},
      vi.fn().mockRejectedValue(new Error('list unavailable')),
    )
    excel.openImport()
    excel.preview.value = validPreview('committed').data
    await excel.confirmImport()
    expect(excel.importVisible.value).toBe(false)
    expect(ElMessage.success).toHaveBeenCalled()
    expect(ElMessage.error).not.toHaveBeenCalled()
    expect(ElMessage.warning).toHaveBeenCalledWith(
      '导入已成功，列表刷新失败，请重新查询',
    )
  })

  it('releases the temporary URL after downloading a template', async () => {
    vi.useFakeTimers()
    const create = vi
      .spyOn(URL, 'createObjectURL')
      .mockReturnValue('blob:template')
    const release = vi
      .spyOn(URL, 'revokeObjectURL')
      .mockImplementation(() => {})
    const click = vi
      .spyOn(HTMLAnchorElement.prototype, 'click')
      .mockImplementation(() => {})
    try {
      await useDemoItemExcel({}, vi.fn()).downloadTemplate()
      expect(downloadDemoItemTemplate).toHaveBeenCalledOnce()
      expect(click).toHaveBeenCalledOnce()
      expect(create).toHaveBeenCalledOnce()
      await vi.runAllTimersAsync()
      expect(release).toHaveBeenCalledWith('blob:template')
    } finally {
      create.mockRestore()
      release.mockRestore()
      click.mockRestore()
      vi.useRealTimers()
    }
  })
})
