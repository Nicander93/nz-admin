import { describe, expect, it, vi } from 'vitest'
import { getFrontendModuleManifests, getModuleCodeForComponent } from '@/core/modules/registry'
import {
  createGeneratorRequest,
  useGenerator,
  type GeneratorApi,
} from '@/views/generator/hooks'
import type { GeneratorColumn, GeneratorTable } from '@/api/generator'

const table: GeneratorTable = {
  schemaName: 'public',
  tableName: 'demo_item',
  tableComment: '示例条目',
  columnCount: 2,
}

const columns: GeneratorColumn[] = [
  {
    ordinalPosition: 1,
    columnName: 'id',
    columnComment: '主键',
    dataType: 'bigint',
    udtName: 'int8',
    nullable: false,
    primaryKey: true,
    identity: true,
  },
  {
    ordinalPosition: 2,
    columnName: 'name',
    columnComment: '名称',
    dataType: 'character varying',
    udtName: 'varchar',
    nullable: false,
    primaryKey: false,
    identity: false,
  },
]

function createApi() {
  return {
    listTables: vi.fn().mockResolvedValue({ code: 200, msg: '成功', data: [table] }),
    listColumns: vi.fn().mockResolvedValue({ code: 200, msg: '成功', data: columns }),
    preview: vi.fn().mockResolvedValue({
      code: 200,
      msg: '成功',
      data: { columns, files: { 'DemoItemDO.java': 'class DemoItemDO {}' } },
    }),
    download: vi.fn(),
  } as unknown as GeneratorApi
}

describe('generator module', () => {
  it('is discovered by the frontend module registry', () => {
    expect(getFrontendModuleManifests().map((item) => item.code)).toContain('generator')
    expect(getModuleCodeForComponent('@/views/generator/index.vue')).toBe('generator')
  })

  it('infers editable generation defaults from a selected table', () => {
    expect(createGeneratorRequest(table)).toEqual({
      schemaName: 'public',
      tableName: 'demo_item',
      moduleName: 'demo',
      businessName: 'item',
      className: 'DemoItem',
      packageName: 'com.nz.admin.modules.demo',
      featureName: '示例条目',
      author: 'nz-admin',
      parentMenuId: 0,
    })
  })

  it('loads metadata and previews files through the injected API', async () => {
    const api = createApi()
    const generator = useGenerator(api)

    await generator.loadTables()
    await generator.openConfigure(table)
    await generator.generatePreview()

    expect(generator.tables.value).toEqual([table])
    expect(generator.columns.value).toEqual(columns)
    expect(generator.form.className).toBe('DemoItem')
    expect(generator.fileNames.value).toEqual(['DemoItemDO.java'])
    expect(generator.activeContent.value).toContain('class DemoItemDO')
    expect(api.preview).toHaveBeenCalledWith(expect.objectContaining({
      tableName: 'demo_item',
      moduleName: 'demo',
    }))
  })
})
