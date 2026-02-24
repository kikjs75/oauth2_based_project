import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import client, { isLoggedIn, hasRole } from '../api/client.js'

export default function PostFormPage() {
  const { id } = useParams()
  const isEdit = Boolean(id)
  const navigate = useNavigate()
  const [form, setForm] = useState({ title: '', content: '' })
  const [error, setError] = useState('')

  useEffect(() => {
    if (!isLoggedIn()) { navigate('/login'); return }
    if (!hasRole('ROLE_WRITER') && !hasRole('ROLE_ADMIN')) {
      navigate('/posts'); return
    }
    if (isEdit) {
      client.get(`/posts/${id}`)
        .then(res => setForm({ title: res.data.title, content: res.data.content }))
        .catch(() => setError('게시글을 불러오지 못했습니다.'))
    }
  }, [id, isEdit, navigate])

  function handleChange(e) {
    setForm({ ...form, [e.target.name]: e.target.value })
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    try {
      if (isEdit) {
        await client.put(`/posts/${id}`, form)
        navigate(`/posts/${id}`)
      } else {
        const res = await client.post('/posts', form)
        navigate(`/posts/${res.data.id}`)
      }
    } catch (err) {
      setError(err.response?.data?.message ?? '저장에 실패했습니다.')
    }
  }

  return (
    <div className="card">
      <h2 style={{ marginBottom: '1.5rem' }}>{isEdit ? '게시글 수정' : '게시글 작성'}</h2>
      {error && <div className="alert alert-error">{error}</div>}
      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label>제목</label>
          <input name="title" value={form.title} onChange={handleChange} required maxLength={200} />
        </div>
        <div className="form-group">
          <label>내용</label>
          <textarea name="content" value={form.content} onChange={handleChange} required />
        </div>
        <div style={{ display: 'flex', gap: '0.6rem' }}>
          <button type="submit" className="btn btn-primary">{isEdit ? '수정하기' : '등록하기'}</button>
          <button type="button" className="btn btn-outline" onClick={() => navigate(-1)}>취소</button>
        </div>
      </form>
    </div>
  )
}
