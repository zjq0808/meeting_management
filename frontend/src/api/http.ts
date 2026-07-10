import axios from 'axios'
import store from '@/store'
import { appendSignature, decryptResponseData, encryptRequestData, isWeComEnabled } from './wecom'

const http = axios.create({
  baseURL: process.env.VUE_APP_API_BASE || '/api',
  timeout: 60000,
  transformResponse: [
    (respData) => {
      if (isWeComEnabled()) {
        return decryptResponseData(respData)
      }
      if (typeof respData === 'string' && respData.length > 0) {
        try {
          return JSON.parse(respData)
        } catch (error) {
          return respData
        }
      }
      return respData
    }
  ]
})

http.interceptors.request.use((config) => {
  const token = store.state.token || localStorage.getItem('token')
  if (token) {
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${token}`
  }
  if (!isWeComEnabled()) {
    return config
  }
  return encryptRequestData(appendSignature(config))
})

http.interceptors.response.use((response) => {
  const body = response.data
  if (body && body.success === false) {
    return Promise.reject(new Error(body.message || '璇锋眰澶辫触'))
  }
  return body?.data ?? body
}, (error) => {
  const responseText = typeof error?.response?.data === 'string' ? error.response.data : ''
  let message = error?.response?.data?.message || error.message || '网络请求失败'
  if (responseText.includes('Proxy error') || responseText.includes('ECONNREFUSED') || String(message).includes('Network Error')) {
    message = '后端服务未连接，请确认 Spring Boot 已在 http://localhost:9090 启动'
  }
  return Promise.reject(new Error(message))
})

export default http

