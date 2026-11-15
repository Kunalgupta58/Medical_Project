import api from './client'

export const dashboardApi = {
  overview: (elderlyUserId) => api.get('/dashboard/overview', { params: { elderlyUserId } }),
}
