export interface R<T = unknown> {
  code: number
  msg: string
  data: T
}

export interface PageQuery {
  [key: string]: unknown
  pageNum?: number
  pageSize?: number
}

export interface PageResult<T = unknown> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}
