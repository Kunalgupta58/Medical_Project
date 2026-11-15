import api from './client'

export const medicationsApi = {
  getAll:        ()         => api.get('/medications'),
  getOne:        (id)       => api.get(`/medications/${id}`),
  create:        (data)     => api.post('/medications', data),
  update:        (id, data) => api.put(`/medications/${id}`, data),
  deactivate:    (id)       => api.delete(`/medications/${id}`),

  getTodaySchedule: ()      => api.get('/medication-schedules/today'),
  getUpcoming:      ()      => api.get('/medication-schedules/upcoming'),

  markTaken:  (id)          => api.post(`/doses/${id}/taken`),
  snoozeDose: (id)          => api.post(`/doses/${id}/snooze`),
  skipDose:   (id, reason)  => api.post(`/doses/${id}/skip`, { reason }),
}
