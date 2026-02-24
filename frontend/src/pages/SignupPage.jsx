import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import client from '../api/client.js'

export default function SignupPage() {
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
      await client.post('/auth/signup', form)
      navigate('/login')
    } catch (err) {
      setError(err.response?.data?.message ?? '회원가입에 실패했습니다.')
    }
  }

  return (
    <div className="auth-page">
      <div className="card">
        <h2>회원가입</h2>
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
          <button type="submit" className="btn btn-primary" style={{ width: '100%' }}>가입하기</button>
        </form>
        <p className="auth-footer">이미 계정이 있으신가요? <Link to="/login">로그인</Link></p>
      </div>
    </div>
  )
}
