import { Outlet } from 'react-router-dom'
import Sidebar from './Sidebar.jsx'

export default function Layout() {
  return (
    <div className="flex h-screen overflow-hidden">
      <Sidebar />
      <main className="flex-1 overflow-auto" style={{ background: 'var(--color-bg-page)' }}>
        <Outlet />
      </main>
    </div>
  )
}
