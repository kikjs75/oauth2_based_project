import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import client, { setToken } from '../api/client.js'

export default function LoginPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ username: '', password: '' })
  const [error, setError] = useState('')

  function handleChange(e) {
    setForm({ ...form, [e.target.name]: e.target.value })
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    try {
      const res = await client.post('/auth/login', form)
      setToken(res.data.accessToken)
      navigate('/posts')
    } catch (err) {
      setError(err.response?.data?.message ?? '로그인에 실패했습니다.')
    }
  }

  function loginWithGoogle() {
    window.location.href = '/oauth2/authorization/google'
  }

  return (
    <div className="auth-page">
      <div className="card">
        <h2>로그인</h2>
        {error && <div className="alert alert-error">{error}</div>}
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>아이디</label>
            <input name="username" value={form.username} onChange={handleChange} required />
          </div>
          <div className="form-group">
            <label>비밀번호</label>
            <input name="password" type="password" value={form.password} onChange={handleChange} required />
          </div>
          <button type="submit" className="btn btn-primary" style={{ width: '100%' }}>로그인</button>
        </form>

        <div className="auth-divider">또는</div>

        <button className="btn btn-google" onClick={loginWithGoogle}>
          <svg width="18" height="18" viewBox="0 0 48 48">
            <path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"/>
            <path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"/>
            <path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z"/>
            <path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"/>
          </svg>
          Google로 로그인
        </button>

        <p className="auth-footer">계정이 없으신가요? <Link to="/signup">회원가입</Link></p>
      </div>
    </div>
  )
}
