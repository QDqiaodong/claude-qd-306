import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', redirect: '/presses' },
  { path: '/presses', component: () => import('../views/Presses.vue'), meta: { label: '印刷机' } },
  { path: '/plates', component: () => import('../views/Plates.vue'), meta: { label: '印版' } },
  { path: '/papers', component: () => import('../views/Papers.vue'), meta: { label: '纸张' } },
  { path: '/jobs', component: () => import('../views/Jobs.vue'), meta: { label: '印刷工单' } },
  { path: '/proofs', component: () => import('../views/Proofs.vue'), meta: { label: '校色试印' } }
]

export const tabs = routes.filter((r) => r.meta).map((r) => ({ path: r.path, label: r.meta.label }))

export default createRouter({
  history: createWebHistory(),
  routes
})
