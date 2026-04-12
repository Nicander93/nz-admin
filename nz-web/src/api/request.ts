import axios, { type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import type { R } from './types'

const instance = axios.create({
  timeout: 10000,
})

instance.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = token
  }
  return config
})

instance.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.msg || '请求失败')
      if (res.code === 401) {
        localStorage.removeItem('token')
        router.push('/login')
      }
      return Promise.reject(res)
    }
    return res
  },
  (error) => {
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  },
)

const request = {
  get<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<R<T>> {
    return instance.get(url, config) as unknown as Promise<R<T>>
  },
  post<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<R<T>> {
    return instance.post(url, data, config) as unknown as Promise<R<T>>
  },
  put<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<R<T>> {
    return instance.put(url, data, config) as unknown as Promise<R<T>>
  },
  delete<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<R<T>> {
    return instance.delete(url, config) as unknown as Promise<R<T>>
  },
}

export default request
