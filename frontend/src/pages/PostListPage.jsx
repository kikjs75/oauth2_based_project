import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import client, { isLoggedIn, hasRole } from '../api/client.js'

export default function PostListPage() {
  const navigate = useNavigate()
  const [posts, setPosts] = useState([])
  const [error, setError] = useState('')
  const canWrite = hasRole('ROLE_WRITER') || hasRole('ROLE_ADMIN')

  useEffect(() => {
    if (!isLoggedIn()) { navigate('/login'); return }
    client.get('/posts')
      .then(res => setPosts(res.data))
      .catch(() => setError('게시글을 불러오지 못했습니다.'))
  }, [navigate])

  return (
    <div>
      <div className="page-header">
        <h2>게시판</h2>
        {canWrite && <Link to="/posts/new" className="btn btn-primary">글쓰기</Link>}
      </div>
      {error && <div className="alert alert-error">{error}</div>}
      {posts.length === 0
        ? <p className="empty">등록된 게시글이 없습니다.</p>
        : (
          <div className="post-list">
            {posts.map(post => (
              <Link key={post.id} to={`/posts/${post.id}`} className="post-item">
                <h3>{post.title}</h3>
                <p className="post-meta">{post.authorUsername} · {new Date(post.createdAt).toLocaleDateString('ko-KR')}</p>
              </Link>
            ))}
          </div>
        )
      }
    </div>
  )
}
