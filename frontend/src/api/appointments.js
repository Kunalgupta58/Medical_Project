import api from './client'

export const appointmentsApi = {
  getAll:   (elderlyUserId) => api.get('/appointments', { params: { elderlyUserId } }),
  create:   (data)          => api.post('/appointments', data),
  update:   (id, data)      => api.put(`/appointments/${id}`, data),
  cancel:   (id)            => api.post(`/appointments/${id}/cancel`),
}
