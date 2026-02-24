import { Link, useNavigate } from 'react-router-dom'
import { isLoggedIn, removeToken, getCurrentUser } from '../api/client.js'

export default function Navbar() {
  const navigate = useNavigate()
  const loggedIn = isLoggedIn()
  const user = getCurrentUser()

  function logout() {
    removeToken()
    navigate('/login')
  }

  return (
    <nav>
      <Link to="/posts" className="brand">Portfolio Board</Link>
      <div className="nav-links">
        {loggedIn ? (
          <>
            <span style={{ color: '#e2e8f0', fontSize: '0.85rem' }}>{user?.username}</span>
            <button className="link-btn" onClick={logout}>로그아웃</button>
          </>
        ) : (
          <>
            <Link to="/login">로그인</Link>
            <Link to="/signup">회원가입</Link>
          </>
        )}
      </div>
    </nav>
  )
}
