import api from './client'

export const caregiverApi = {
  getMyTasks:       ()              => api.get('/caregiver/tasks'),
  getTasksFor:      (elderlyId)     => api.get(`/caregiver/tasks/elderly/${elderlyId}`),
  createTask:       (data)          => api.post('/caregiver/tasks', data),
  updateTask:       (id, data)      => api.put(`/caregiver/tasks/${id}`, data),
  getAlerts:        (elderlyId)     => api.get(`/caregiver/alerts/${elderlyId}`),
  acknowledgeAlert: (scheduleId)    => api.post(`/caregiver/alerts/${scheduleId}/acknowledge`),
}

export const relationshipsApi = {
  invite:       (data)  => api.post('/relationships/invite', data),
  accept:       (token) => api.post('/relationships/accept', { token }),
  getAll:       ()      => api.get('/relationships'),
  getInvites:   ()      => api.get('/relationships/invitations'),
  getPending:   ()      => api.get('/relationships/pending'),
  revoke:       (id)    => api.delete(`/relationships/${id}`),
}

export const emergencyApi = {
  triggerSOS:   (data) => api.post('/emergency/sos', data),
  getContacts:  (elderlyUserId) => api.get('/emergency/contacts', { params: { elderlyUserId } }),
  addContact:   (data) => api.post('/emergency/contacts', data),
}
