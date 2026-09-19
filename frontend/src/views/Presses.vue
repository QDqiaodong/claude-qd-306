<template>
  <div class="pane">
    <div class="head">
      <h2>印刷机</h2>
      <span class="sub">深色灯墙，一盏灯一台机器；点灯换状态（停机的机器点一下就先跑起来）</span>
      <button class="prime" @click="openNew">新增机器</button>
    </div>
    <div class="wall">
      <div v-for="p in items" :key="p.id" class="lamp">
        <div class="bulb" :class="lampOf(p)" @click="cycle(p)"></div>
        <div class="lamp-name">{{ p.pressName }}</div>
        <div class="lamp-code">{{ p.pressCode }}{{ p.operator ? ' · ' + p.operator : '' }}</div>
        <div class="lamp-state">{{ p.pressState }}</div>
        <button class="ghost" @click="openEdit(p)">改</button>
      </div>
    </div>
    <el-dialog v-model="dialog" :title="form.id ? '修改印刷机' : '新增印刷机'" width="420px">
      <div class="fr"><label>编号</label><el-input v-model="form.pressCode" /></div>
      <div class="fr"><label>名称</label><el-input v-model="form.pressName" /></div>
      <div class="fr"><label>型号</label><el-input v-model="form.modelText" /></div>
      <div class="fr"><label>机长</label><el-input v-model="form.operator" /></div>
      <div class="fr"><label>状态</label><el-input v-model="form.pressState" placeholder="运行 / 停机 / 封存" /></div>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { pressApi } from '../api'
import { emit } from '../utils/bus'

const items = ref([])
const dialog = ref(false)
const form = ref({})
const CYCLE = ['停机', '运行', '封存']

async function load() {
  items.value = await pressApi.list()
}
function lampOf(p) {
  return 'k' + Math.max(0, ['运行', '停机', '封存'].indexOf(p.pressState))
}
function openNew() {
  form.value = { pressState: '停机' }
  dialog.value = true
}
function openEdit(row) {
  form.value = { ...row }
  dialog.value = true
}
async function submit() {
  try {
    if (form.value.id) await pressApi.save(form.value.id, form.value)
    else await pressApi.add(form.value)
    dialog.value = false
    await load()
    emit('data-changed', 'press')
    ElMessage.success('保存好了')
  } catch (e) { ElMessage.error(e.message) }
}
async function cycle(p) {
  const i = CYCLE.indexOf(p.pressState)
  const next = CYCLE[(i + 1) % CYCLE.length]
  try {
    await pressApi.save(p.id, { pressState: next })
    await load()
    emit('data-changed', 'press')
  } catch (e) { ElMessage.error(e.message) }
}
onMounted(load)
</script>

<style scoped>
.head { display: flex; align-items: center; gap: 14px; margin-bottom: 18px; }
.head h2 { margin: 0; font-size: 20px; }
.sub { flex: 1; color: #8d92a8; font-size: 12px; }
.prime { background: var(--el-color-primary); color: #fff; border: none; border-radius: 8px;
  padding: 8px 18px; font-size: 13px; cursor: pointer; }
.wall { display: grid; grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); gap: 16px;
  background: #232a4a; border-radius: 14px; padding: 22px; }
.lamp { text-align: center; color: #dfe2f0; }
.bulb { width: 52px; height: 52px; border-radius: 50%; margin: 0 auto 10px; cursor: pointer;
  transition: .2s; }
.bulb.k0 { background: radial-gradient(circle at 34% 30%, #a5f3c0, #2e7d4f); box-shadow: 0 0 16px #4caf5099; }
.bulb.k1 { background: radial-gradient(circle at 34% 30%, #dfe2f0, #6b7099); box-shadow: 0 0 10px #8f94b866; }
.bulb.k2 { background: radial-gradient(circle at 34% 30%, #ffd8d8, #a33); box-shadow: 0 0 16px #e5393566; }
.lamp-name { font-size: 13px; }
.lamp-code { font-size: 11px; color: #8f94b8; margin: 3px 0; }
.lamp-state { font-size: 12px; color: #b9bed6; margin-bottom: 8px; }
.ghost { background: transparent; border: 1px solid #4a5178; color: #dfe2f0; border-radius: 7px;
  padding: 4px 12px; font-size: 12px; cursor: pointer; }
.fr { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.fr label { width: 68px; text-align: right; font-size: 13px; color: #71758c; }
</style>
