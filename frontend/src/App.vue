<template>
  <div class="shell">
    <header class="top">
      <span class="brand">印刷厂</span>
      <div class="seg">
        <router-link v-for="t in tabs" :key="t.path" :to="t.path"
                     class="cell" :class="{ on: $route.path === t.path }">{{ t.label }}</router-link>
      </div>
    </header>
    <main class="main"><router-view /></main>
  </div>
</template>

<script setup>
import { onUnmounted, ref } from 'vue'
import { on } from './utils/bus'
import { tabs } from './router'

const lastEvent = ref('')
const off = on('data-changed', (name) => {
  lastEvent.value = name
})
onUnmounted(off)
</script>

<style>
html, body { margin: 0; }
body { background: #f4f5fa; font-family: -apple-system, 'PingFang SC', sans-serif; }
.shell { min-height: 100vh; }
.top { display: flex; align-items: center; gap: 24px; background: #fff; padding: 14px 26px;
  border-bottom: 1px solid #e9ebf5; }
.brand { font-size: 15px; font-weight: 700; color: var(--el-color-primary-dark-2); }
.seg { display: flex; gap: 3px; background: #f2f3f9; border-radius: 10px; padding: 3px; }
.cell { padding: 7px 18px; border-radius: 8px; text-decoration: none; font-size: 13px; color: #71758c; }
.cell.on { background: #fff; color: var(--el-color-primary-dark-2); font-weight: 600;
  box-shadow: 0 1px 4px rgba(0,0,0,.08); }
.main { padding: 22px 26px; }
</style>
