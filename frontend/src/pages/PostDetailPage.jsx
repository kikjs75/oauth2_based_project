import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import client, { getCurrentUser, hasRole } from '../api/client.js'

export default function PostDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [post, setPost] = useState(null)
  const [error, setError] = useState('')
  const currentUser = getCurrentUser()

  useEffect(() => {
    client.get(`/posts/${id}`)
      .then(res => setPost(res.data))
      .catch(() => setError('게시글을 불러오지 못했습니다.'))
  }, [id])

  async function handleDelete() {
    if (!window.confirm('정말 삭제하시겠습니까?')) return
    try {
      await client.delete(`/posts/${id}`)
      navigate('/posts')
    } catch {
      setError('삭제에 실패했습니다.')
    }
  }

  if (!post && !error) return <p style={{ textAlign: 'center', marginTop: '3rem' }}>불러오는 중...</p>

  const isAuthor = currentUser?.id === post?.authorId
  const isAdmin = hasRole('ROLE_ADMIN')
  const canModify = isAuthor || isAdmin

  return (
    <div className="card post-detail">
      {error && <div className="alert alert-error">{error}</div>}
      {post && (
        <>
          <h2>{post.title}</h2>
          <p className="meta">{post.authorUsername} · {new Date(post.createdAt).toLocaleDateString('ko-KR')}</p>
          <p className="content">{post.content}</p>
          <div className="post-actions">
            <Link to="/posts" className="btn btn-outline">목록</Link>
            {canModify && (
              <>
                <Link to={`/posts/${id}/edit`} className="btn btn-primary">수정</Link>
                <button className="btn btn-danger" onClick={handleDelete}>삭제</button>
              </>
            )}
          </div>
        </>
      )}
    </div>
  )
}
