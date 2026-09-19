<template>
  <div class="pane">
    <div class="head">
      <h2>印版</h2>
      <span class="sub">点表格里任意一格，就地弹出小面板改这一格</span>
      <button class="prime" @click="openNew">登记印版</button>
    </div>
    <div class="grid-table">
      <div class="row header">
        <span>编号</span><span>版面</span><span>尺寸</span><span>装机</span><span>制版日期</span><span>状态</span>
      </div>
      <div v-for="p in items" :key="p.id" class="row">
        <span class="mono">{{ p.plateCode }}</span>
        <span>{{ p.plateName }}</span>
        <span class="cellv" @click="openCell(p, 'plateSize', '尺寸', $event)">{{ p.plateSize || '—' }}</span>
        <span class="cellv" @click="openCell(p, 'pressId', '装机 id', $event)">{{ pressName(p.pressId) }}</span>
        <span class="cellv" @click="openCell(p, 'plateDate', '制版日期', $event)">{{ p.plateDate || '—' }}</span>
        <span class="cellv" :class="tone(p.plateState)" @click="openCell(p, 'plateState', '状态', $event)">
          {{ p.plateState }}
        </span>
      </div>
    </div>

    <div v-if="cell.show" class="pop" :style="{ left: cell.x + 'px', top: cell.y + 'px' }">
      <div class="pop-title">{{ cell.label }}</div>
      <input v-model="cell.value" @keyup.enter="commitCell" />
      <div class="pop-acts">
        <button class="ghost" @click="cell.show = false">取消</button>
        <button class="prime small" @click="commitCell">确定</button>
      </div>
    </div>

    <el-dialog v-model="dialog" title="登记印版" width="420px">
      <div class="fr"><label>编号</label><el-input v-model="form.plateCode" /></div>
      <div class="fr"><label>版面</label><el-input v-model="form.plateName" /></div>
      <div class="fr"><label>尺寸</label><el-input v-model="form.plateSize" placeholder="四开 / 八开 / 对开" /></div>
      <div class="fr"><label>装机 id</label><el-input v-model="form.pressId" /></div>
      <div class="fr"><label>制版日期</label><el-input v-model="form.plateDate" placeholder="2026-09-19" /></div>
      <div class="fr"><label>状态</label><el-input v-model="form.plateState" placeholder="在用 / 已磨损 / 已作废" /></div>
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
import { plateApi, pressApi } from '../api'
import { emit } from '../utils/bus'

const items = ref([])
const presses = ref([])
const dialog = ref(false)
const form = ref({})
const cell = ref({ show: false, id: null, key: '', label: '', value: '', x: 0, y: 0 })

async function load() {
  items.value = await plateApi.list()
  presses.value = await pressApi.list()
}
function pressName(id) {
  const p = presses.value.find((x) => x.id === id)
  return p ? p.pressCode : '未装机'
}
function tone(state) {
  return state === '已作废' ? 'bad' : state === '已磨损' ? 'warn' : ''
}
function openCell(row, key, label, e) {
  cell.value = {
    show: true, id: row.id, key, label,
    value: row[key] ?? '',
    x: Math.min(e.clientX, window.innerWidth - 230), y: e.clientY + 10
  }
}
async function commitCell() {
  const { id, key, value } = cell.value
  cell.value.show = false
  try {
    await plateApi.save(id, { [key]: key === 'pressId' ? Number(value) : value })
    await load()
    emit('data-changed', 'plate')
    ElMessage.success('已更新')
  } catch (e) { ElMessage.error(e.message); await load() }
}
function openNew() {
  form.value = { plateState: '在用' }
  dialog.value = true
}
async function submit() {
  try {
    await plateApi.add(form.value)
    dialog.value = false
    await load()
    emit('data-changed', 'plate')
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
.prime.small { padding: 5px 14px; font-size: 12px; }
.grid-table { background: #fff; border: 1px solid #e9ebf5; border-radius: 12px; overflow: hidden; }
.row { display: grid; grid-template-columns: 110px 1.4fr 90px 110px 120px 110px; gap: 8px;
  padding: 11px 14px; border-bottom: 1px solid #f3f4fa; font-size: 13px; align-items: center; }
.row.header { background: #f7f8fc; color: #8d92a8; font-size: 12px; }
.mono { font-family: ui-monospace, Menlo, monospace; color: #8d92a8; }
.cellv { cursor: pointer; border-radius: 5px; padding: 3px 6px; border-left: 2px solid transparent; }
.cellv:hover { background: var(--el-color-primary-light-9); border-left-color: var(--el-color-primary); }
.cellv.warn { color: #b4761f; }
.cellv.bad { color: #c0392b; }
.pop { position: fixed; z-index: 40; width: 210px; background: #fff; border: 1px solid #e0e3ef;
  border-radius: 9px; padding: 10px; box-shadow: 0 10px 26px rgba(0,0,0,.14); }
.pop-title { font-size: 12px; color: #8d92a8; margin-bottom: 6px; }
.pop input { width: 100%; box-sizing: border-box; border: 1px solid #e0e3ef; border-radius: 6px;
  padding: 6px 8px; font-size: 13px; }
.pop-acts { display: flex; justify-content: flex-end; gap: 8px; margin-top: 8px; }
.ghost { background: #fff; border: 1px solid var(--el-color-primary-light-7); color: var(--el-color-primary-dark-2);
  border-radius: 7px; padding: 5px 14px; font-size: 12px; cursor: pointer; }
.fr { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.fr label { width: 76px; text-align: right; font-size: 13px; color: #71758c; }
</style>
