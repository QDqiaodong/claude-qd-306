import axios from 'axios'

const http = axios.create({ baseURL: '/api', timeout: 10000 })

http.interceptors.response.use(
  (res) => res.data,
  (err) => Promise.reject(new Error(err?.response?.data?.message || err.message || '请求没成功'))
)

export const pressApi = {
  list: (params) => http.get('/presses', { params }),
  add: (b) => http.post('/presses', b),
  save: (id, b) => http.put(`/presses/${id}`, b)
}
export const plateApi = {
  list: (params) => http.get('/plates', { params }),
  add: (b) => http.post('/plates', b),
  save: (id, b) => http.put(`/plates/${id}`, b)
}
export const paperApi = {
  list: (params) => http.get('/papers', { params }),
  add: (b) => http.post('/papers', b),
  save: (id, b) => http.put(`/papers/${id}`, b)
}
export const jobApi = {
  search: (params) => http.get('/jobs', { params }),
  add: (b) => http.post('/jobs', b),
  save: (id, b) => http.put(`/jobs/${id}`, b)
}
export const proofApi = {
  list: (params) => http.get('/proofs', { params }),
  add: (b) => http.post('/proofs', b)
}

export default http
