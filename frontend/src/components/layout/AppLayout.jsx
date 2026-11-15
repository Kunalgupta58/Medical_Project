import { Outlet } from 'react-router-dom'
import Sidebar from './Sidebar'
import { useEffect, useState } from 'react'
import { notificationsApi } from '../../api/notifications'

export default function AppLayout() {
  const [unreadCount, setUnreadCount] = useState(0)

  useEffect(() => {
    const fetchCount = () =>
      notificationsApi.unreadCount()
        .then(r => setUnreadCount(r.data?.count ?? 0))
        .catch(() => {})

    fetchCount()
    const interval = setInterval(fetchCount, 60_000)
    return () => clearInterval(interval)
  }, [])

  return (
    <div className="flex h-screen overflow-hidden bg-gray-50">
      <Sidebar unreadCount={unreadCount} />
      <main className="flex-1 overflow-y-auto">
        <div className="page-enter p-6 max-w-7xl mx-auto">
          <Outlet context={{ setUnreadCount }} />
        </div>
      </main>
    </div>
  )
}
