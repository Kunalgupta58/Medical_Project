import api from './client'

export const notificationsApi = {
  getAll:      (page = 0, size = 20) => api.get('/notifications', { params: { page, size } }),
  unreadCount: ()                    => api.get('/notifications/unread-count'),
  markRead:    (id)                  => api.post(`/notifications/${id}/read`),
  markAllRead: ()                    => api.post('/notifications/read-all'),
}
