import axios from 'axios'

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

export const api = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN',
  withXSRFToken: false,
})

export async function ensureCsrf() {
  const response = await api.get<{ token: string; headerName: string }>('/api/auth/csrf')
  api.defaults.headers.common[response.data.headerName] = response.data.token
}

export function errorMessage(error: unknown) {
  if (axios.isAxiosError(error)) return error.response?.data?.message || error.message
  return error instanceof Error ? error.message : 'Unexpected error'
}
