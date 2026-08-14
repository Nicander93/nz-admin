import axios, { type AxiosRequestConfig, type AxiosResponse } from 'axios'
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
  async (response) => {
    if (response.config.responseType === 'blob') {
      const contentType = String(response.headers['content-type'] ?? '')
      if (response.data instanceof Blob && contentType.includes('application/json')) {
        const res = JSON.parse(await response.data.text()) as R
        if (res.code !== 200) {
          ElMessage.error(res.msg || '请求失败')
          if (res.code === 401) {
            localStorage.removeItem('token')
            router.push('/login')
          }
          return Promise.reject(res)
        }
      }
      return response
    }
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
  download<T = Blob>(url: string, data?: unknown): Promise<AxiosResponse<T>> {
    return instance.post(url, data, { responseType: 'blob' }) as Promise<AxiosResponse<T>>
  },
  getBlob<T = Blob>(url: string, config?: AxiosRequestConfig): Promise<AxiosResponse<T>> {
    return instance.get(url, { ...config, responseType: 'blob' }) as Promise<AxiosResponse<T>>
  },
}

export default request
