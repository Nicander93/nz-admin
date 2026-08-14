import request from '@/api/request'

export interface GeneratorTable {
  schemaName: string
  tableName: string
  tableComment: string
  columnCount: number
}

export interface GeneratorColumn {
  ordinalPosition: number
  columnName: string
  columnComment: string
  dataType: string
  udtName: string
  nullable: boolean
  defaultValue?: string
  primaryKey: boolean
  identity: boolean
}

export interface GeneratorRequest {
  schemaName: string
  tableName: string
  moduleName: string
  businessName: string
  className: string
  packageName: string
  featureName: string
  author: string
  parentMenuId: number
}

export interface GeneratorPreview {
  columns: GeneratorColumn[]
  files: Record<string, string>
}

export function listGeneratorTables(params: { schemaName: string; keyword?: string }) {
  return request.get<GeneratorTable[]>('/api/generator/tables', { params })
}

export function listGeneratorColumns(params: { schemaName: string; tableName: string }) {
  return request.get<GeneratorColumn[]>('/api/generator/columns', { params })
}

export function previewGenerator(data: GeneratorRequest) {
  return request.post<GeneratorPreview>('/api/generator/preview', data)
}

export function downloadGenerator(data: GeneratorRequest) {
  return request.download<Blob>('/api/generator/download', data)
}
