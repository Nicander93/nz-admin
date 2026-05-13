import { describe, it, expect, vi } from 'vitest'

vi.mock('@/api/system', async (importOriginal) => {
  const mod = await importOriginal<typeof import('@/api/system')>()
  return {
    ...mod,
    noticeApi: {
      ...mod.noticeApi,
      pageNotices: vi.fn().mockResolvedValue({
        code: 200,
        data: { records: [{ id: 1, title: '系统通知', content: 'c', type: 1, status: 0 }], total: 1 },
      }),
      addNotice: vi.fn().mockResolvedValue({ code: 200 }),
      updateNotice: vi.fn().mockResolvedValue({ code: 200 }),
      deleteNotice: vi.fn().mockResolvedValue({ code: 200 }),
    },
  }
})

import { noticeApi } from '@/api/system'
import { useNoticeCrud } from '@/views/system/notice/hooks'

describe('useNoticeCrud', () => {
  it('loadData 后表格有数据', async () => {
    const { table } = useNoticeCrud()
    table.loadData()

    await vi.waitFor(() => {
      expect(table.data.length).toBeGreaterThan(0)
    })
    expect(table.data[0].title).toBe('系统通知')
    expect(noticeApi.pageNotices).toHaveBeenCalled()
  })

  it('handleResetQuery 将分页回到第一页', () => {
    const { table } = useNoticeCrud()
    table.query.title = 'x'
    table.pagination.current = 4

    table.handleResetQuery()

    expect(table.pagination.current).toBe(1)
  })

  it('openAdd 打开新增弹窗', () => {
    const { form } = useNoticeCrud()
    form.openAdd()

    expect(form.visible).toBe(true)
    expect(form.mode).toBe('add')
    expect(form.model.title).toBe('')
  })
})
