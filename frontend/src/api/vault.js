import api from './client'

export const vaultApi = {
  upload: (formData) =>
    api.post('/medical-documents/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }),
  getAll:         (elderlyUserId) => api.get('/medical-documents', { params: { elderlyUserId } }),
  getOne:         (id)            => api.get(`/medical-documents/${id}`),
  download:       (id)            => api.get(`/medical-documents/${id}/download`, { responseType: 'blob' }),
  updateMetadata: (id, data)      => api.put(`/medical-documents/${id}/metadata`, data),
  delete:         (id)            => api.delete(`/medical-documents/${id}`),
  history:        (elderlyUserId) => api.get('/medical-documents/history', { params: { elderlyUserId } }),
  search:         (q, elderlyUserId) => api.get('/medical-documents/search', { params: { q, elderlyUserId } }),
  processOcr:     (id)            => api.post(`/medical-documents/${id}/process-ocr`),
}

export const ocrApi = {
  process: (file) => {
    const fd = new FormData()
    fd.append('file', file)
    const ocrUrl = import.meta.env.VITE_OCR_URL || 'http://localhost:8001'
    return fetch(`${ocrUrl}/ocr/process`, { method: 'POST', body: fd })
      .then(r => r.ok ? r.json() : r.json().then(e => Promise.reject(e)))
  },
}
