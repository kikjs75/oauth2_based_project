import axios from 'axios'

const client = axios.create({ baseURL: '/api' })

client.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

export default client

// Auth helpers
export const getToken = () => localStorage.getItem('token')
export const setToken = (token) => localStorage.setItem('token', token)
export const removeToken = () => localStorage.removeItem('token')

export function getCurrentUser() {
  const token = getToken()
  if (!token) return null
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    return {
      id: Number(payload.sub),
      username: payload.username,
      roles: payload.roles ?? [],
    }
  } catch {
    return null
  }
}

export function isLoggedIn() {
  return getCurrentUser() !== null
}

export function hasRole(role) {
  return getCurrentUser()?.roles.includes(role) ?? false
}
