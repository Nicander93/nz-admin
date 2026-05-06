import request from '@/api/request'

export type SysPost = {
  id: number
  postCode: string
  postName: string
  sort: number
  status: number
  remark?: string
  createTime?: string
}

export function listPosts() {
  return request.get<SysPost[]>('/api/system/post/list')
}

export function getPost(id: number) {
  return request.get<SysPost>(`/api/system/post/${id}`)
}

export function addPost(data: Partial<SysPost>) {
  return request.post<void>('/api/system/post', data)
}

export function updatePost(data: Partial<SysPost>) {
  return request.put<void>('/api/system/post', data)
}

export function deletePost(id: number) {
  return request.delete<void>(`/api/system/post/${id}`)
}
