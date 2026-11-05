/**
 * 共享 API 请求模块
 * - 自动注入 Authorization: Bearer {token} 请求头
 * - 统一基础路径 /api/v1
 * - 统一 Content-Type: application/json
 */

const API_BASE = '/api/v1'

/**
 * 从 localStorage 获取认证 token
 */
export function getToken() {
  return localStorage.getItem('user_token') || ''
}

/**
 * 构建含认证信息的请求头
 */
export function getAuthHeaders(extraHeaders = {}) {
  const token = getToken()
  const headers = { 'Content-Type': 'application/json', ...extraHeaders }
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }
  return headers
}

/**
 * 通用 API 请求封装 — 自动注入 auth header
 * @param {string} url — 完整请求路径（如 '/api/v1/admin/suggestions'）
 * @param {object} options — fetch 选项，headers 会自动合并
 * @returns {Promise<Response>}
 */
export async function apiFetch(url, options = {}) {
  const headers = getAuthHeaders(options.headers || {})
  return fetch(url, { ...options, headers })
}

export { API_BASE }
