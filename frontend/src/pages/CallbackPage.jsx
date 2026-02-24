import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { setToken } from '../api/client.js'

export default function CallbackPage() {
  const navigate = useNavigate()

  useEffect(() => {
    const params = new URLSearchParams(window.location.search)
    const token = params.get('token')
    if (token) {
      setToken(token)
      navigate('/posts', { replace: true })
    } else {
      navigate('/login', { replace: true })
    }
  }, [navigate])

  return <p style={{ textAlign: 'center', marginTop: '4rem' }}>로그인 처리 중...</p>
}
