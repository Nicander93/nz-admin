import axios, { type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'

import router from '@/router'

import type { R } from './types'

export interface RequestOptions extends AxiosRequestConfig {
  silentError?: boolean
}

declare module 'axios' {
  interface AxiosRequestConfig {
    silentError?: boolean
  }
}

const instance = axios.create({
  timeout: 10000,
})

let unauthorizedHandling: { token: string; promise: Promise<void> } | null = null

function getRequestToken(config?: AxiosRequestConfig): string | undefined {
  const headers = config?.headers
  const value =
    typeof headers?.get === 'function'
      ? headers.get('Authorization')
      : headers?.Authorization ?? headers?.authorization
  return typeof value === 'string' ? value : undefined
}

async function expireSession(requestToken?: string) {
  if (!requestToken || requestToken !== localStorage.getItem('token')) return
  if (unauthorizedHandling?.token === requestToken)
    return unauthorizedHandling.promise

  const handling = (async () => {
    const { useUserStore } = await import('@/stores/user')
    if (requestToken !== localStorage.getItem('token')) return

    const userStore = useUserStore()
    userStore.logout()
    await router.replace('/login')
  })()
  const promise = handling.finally(() => {
    if (unauthorizedHandling?.promise === promise) unauthorizedHandling = null
  })
  unauthorizedHandling = { token: requestToken, promise }
  return promise
}

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
      if (
        response.data instanceof Blob &&
        contentType.includes('application/json')
      ) {
        const res = JSON.parse(await response.data.text()) as R
        if (res.code !== 200) {
          if (!response.config.silentError) ElMessage.error(res.msg || '请求失败')
          if (res.code === 401) {
            await expireSession(getRequestToken(response.config))
          }
          return Promise.reject(res)
        }
      }
      return response
    }
    const res = response.data
    if (res.code !== 200) {
      if (!response.config.silentError) ElMessage.error(res.msg || '请求失败')
      if (res.code === 401) {
        await expireSession(getRequestToken(response.config))
      }
      return Promise.reject(res)
    }
    return res
  },
  async (error) => {
    if (error.response?.status === 401) {
      if (!error.config?.silentError) ElMessage.error(error.response.data?.msg || '登录状态已失效')
      await expireSession(getRequestToken(error.config))
    } else {
      if (!error.config?.silentError) ElMessage.error(error.message || '网络错误')
    }
    return Promise.reject(error)
  },
)

const request = {
  get<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<R<T>> {
    return instance.get(url, config) as unknown as Promise<R<T>>
  },
  post<T = unknown>(
    url: string,
    data?: unknown,
    config?: AxiosRequestConfig,
  ): Promise<R<T>> {
    return instance.post(url, data, config) as unknown as Promise<R<T>>
  },
  put<T = unknown>(
    url: string,
    data?: unknown,
    config?: AxiosRequestConfig,
  ): Promise<R<T>> {
    return instance.put(url, data, config) as unknown as Promise<R<T>>
  },
  delete<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<R<T>> {
    return instance.delete(url, config) as unknown as Promise<R<T>>
  },
  download<T = Blob>(url: string, data?: unknown): Promise<AxiosResponse<T>> {
    return instance.post(url, data, { responseType: 'blob' }) as Promise<
      AxiosResponse<T>
    >
  },
  getBlob<T = Blob>(
    url: string,
    config?: AxiosRequestConfig,
  ): Promise<AxiosResponse<T>> {
    return instance.get(url, { ...config, responseType: 'blob' }) as Promise<
      AxiosResponse<T>
    >
  },
}

export default request
