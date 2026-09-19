<template>
  <div class="pane">
    <div class="head">
      <h2>纸张</h2>
      <span class="sub">每条一根水位条，中间的虚线是预警线；低于线就标黄、见底标红</span>
      <button class="prime" @click="openNew">登记纸卷</button>
    </div>
    <div class="levels">
      <div v-for="p in items" :key="p.id" class="lv" :class="tone(p)">
        <div class="lv-top">
          <b>{{ p.paperName }}</b>
          <span class="code">{{ p.paperCode }} · {{ p.gramWeight ?? '-' }}g</span>
          <span class="state">{{ p.paperState }}</span>
        </div>
        <div class="bar">
          <div class="fill" :style="{ width: pct(p) + '%' }"></div>
          <div v-if="p.warnLine" class="warnline" :style="{ left: warnPct(p) + '%' }"></div>
        </div>
        <div class="lv-foot">
          <span>现有 {{ p.stock }}</span>
          <span>预警线 {{ p.warnLine ?? '未设' }}</span>
          <button class="ghost" @click="openEdit(p)">改</button>
        </div>
      </div>
    </div>

    <el-dialog v-model="dialog" :title="form.id ? '修改纸卷' : '登记纸卷'" width="420px">
      <div class="fr"><label>纸号</label><el-input v-model="form.paperCode" /></div>
      <div class="fr"><label>纸名</label><el-input v-model="form.paperName" /></div>
      <div class="fr"><label>克重</label><el-input v-model="form.gramWeight" /></div>
      <div class="fr"><label>库存</label><el-input v-model="form.stock" /></div>
      <div class="fr"><label>预警线</label><el-input v-model="form.warnLine" /></div>
      <div class="fr"><label>状态</label><el-input v-model="form.paperState" placeholder="充足 / 紧张 / 缺货" /></div>
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
import { paperApi } from '../api'
import { emit } from '../utils/bus'

const items = ref([])
const dialog = ref(false)
const form = ref({})

const MAX = 60000

async function load() {
  items.value = await paperApi.list()
}
function pct(p) {
  return Math.min(100, Math.round((Number(p.stock || 0) * 100) / MAX))
}
function warnPct(p) {
  return Math.min(100, Math.round((Number(p.warnLine || 0) * 100) / MAX))
}
function tone(p) {
  if (p.paperState === '缺货') return 'out'
  return p.paperState === '紧张' ? 'low' : ''
}
function openNew() {
  form.value = { paperState: '充足' }
  dialog.value = true
}
function openEdit(row) {
  form.value = { ...row }
  dialog.value = true
}
async function submit() {
  try {
    if (form.value.id) await paperApi.save(form.value.id, form.value)
    else await paperApi.add(form.value)
    dialog.value = false
    await load()
    emit('data-changed', 'paper')
    ElMessage.success('保存好了')
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
.levels { background: #fff; border: 1px solid #e9ebf5; border-radius: 12px; padding: 6px 18px; }
.lv { padding: 14px 0; border-bottom: 1px solid #f3f4fa; }
.lv-top { display: flex; align-items: center; gap: 12px; margin-bottom: 10px; }
.lv-top b { font-size: 14px; }
.code { color: #8d92a8; font-size: 12px; }
.state { margin-left: auto; font-size: 12px; color: var(--el-color-primary-dark-2); }
.lv.low .state { color: #b4761f; }
.lv.out .state { color: #c0392b; }
.bar { position: relative; height: 14px; background: #f2f3f9; border-radius: 7px; overflow: hidden; }
.fill { height: 100%; background: var(--el-color-primary); border-radius: 7px; transition: width .3s; }
.lv.low .fill { background: #e6a23c; }
.lv.out .fill { background: #d9534f; }
.warnline { position: absolute; top: 0; bottom: 0; width: 0; border-left: 2px dashed #b9bed6; }
.lv-foot { display: flex; align-items: center; gap: 20px; margin-top: 8px; font-size: 12px; color: #8d92a8; }
.lv-foot .ghost { margin-left: auto; }
.ghost { background: #fff; border: 1px solid var(--el-color-primary-light-7); color: var(--el-color-primary-dark-2);
  border-radius: 7px; padding: 4px 12px; font-size: 12px; cursor: pointer; }
.fr { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.fr label { width: 68px; text-align: right; font-size: 13px; color: #71758c; }
</style>
