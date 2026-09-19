<template>
  <div class="pane">
    <div class="head">
      <h2>校色试印台账</h2>
      <span class="sub">落一条必须同时点上待印工单 + 印版 + 印刷机；通过按推进那一刻的装版与机态重核，不挂终身</span>
      <button class="prime" @click="openNew">落一条试印</button>
    </div>

    <div class="table">
      <div class="row head-row">
        <span>试印时间</span><span>工单</span><span>印版</span><span>印刷机</span>
        <span>结果</span><span>眼下还作数吗</span><span>机长 / 备注</span>
      </div>
      <div v-for="p in items" :key="p.id" class="row">
        <span class="dim">{{ fmt(p.proofTime) }}</span>
        <span>
          <span class="mono">{{ p.jobNo }}</span>
          <span class="dim"> · {{ p.clientName }} · {{ p.jobState }}</span>
        </span>
        <span>
          <span class="mono">{{ p.plateCode }}</span>
          <span class="dim"> · 版态 {{ p.plateState || '?' }} · 现装 {{ mountedPressLabel(p) }}</span>
        </span>
        <span>
          <span class="mono">{{ p.pressCode }}</span>
          <span class="dim"> · {{ p.pressState || '?' }}</span>
        </span>
        <span class="result" :class="p.result === '通过' ? 'pass' : 'fail'">{{ p.result }}</span>
        <span v-if="p.result !== '通过'" class="dim">—</span>
        <span v-else-if="p.validNow" class="valid">有效，可上机</span>
        <el-tooltip v-else :content="p.invalidReason" placement="top">
          <span class="invalid">已失效（悬停看原因）</span>
        </el-tooltip>
        <span class="dim">{{ p.operator || '—' }}{{ p.note ? ' · ' + p.note : '' }}</span>
      </div>
      <div v-if="!items.length" class="empty">台账还是空的，先落一条试印</div>
    </div>

    <el-dialog v-model="dialog" title="落一条校色试印" width="480px">
      <div class="fr">
        <label>工单</label>
        <el-select v-model="form.jobId" style="flex:1" placeholder="只能选还停在待印的单">
          <el-option v-for="j in waitingJobs" :key="j.id"
                     :label="j.jobNo + ' · ' + j.clientName" :value="j.id" />
        </el-select>
      </div>
      <div class="fr">
        <label>印版</label>
        <el-select v-model="form.plateId" style="flex:1">
          <el-option v-for="pl in plates" :key="pl.id"
                     :label="pl.plateCode + ' ' + pl.plateName + '（' + pl.plateState
                       + '，现装 ' + (pressCodeOf(pl.pressId) || '未装机') + '）'"
                     :value="pl.id" />
        </el-select>
      </div>
      <div class="fr">
        <label>印刷机</label>
        <el-select v-model="form.pressId" style="flex:1">
          <el-option v-for="pr in presses" :key="pr.id"
                     :label="pr.pressCode + ' ' + pr.pressName + '（' + pr.pressState + '）'"
                     :value="pr.id" />
        </el-select>
      </div>
      <div class="fr">
        <label>结果</label>
        <el-radio-group v-model="form.result">
          <el-radio label="通过">通过</el-radio>
          <el-radio label="不通过">不通过</el-radio>
        </el-radio-group>
      </div>
      <div class="fr"><label>机长</label><el-input v-model="form.operator" /></div>
      <div class="fr"><label>备注</label><el-input v-model="form.note" type="textarea" :rows="2" /></div>
      <p class="note">
        记「通过」时：印版必须在用、正好装在指定机器上、机器得在运行；
        印刷中或已完成的单子补账会当场被退。
      </p>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="submit">入账</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { jobApi, plateApi, pressApi, proofApi } from '../api'
import { emit, on } from '../utils/bus'

const items = ref([])
const jobs = ref([])
const plates = ref([])
const presses = ref([])
const dialog = ref(false)
const form = ref({})

const waitingJobs = computed(() => jobs.value.filter((j) => j.jobState === '待印'))

async function load() {
  const [jobRows, plateRows, pressRows, proofRows] = await Promise.all([
    jobApi.search({}), plateApi.list(), pressApi.list(), proofApi.list()
  ])
  jobs.value = jobRows
  plates.value = plateRows
  presses.value = pressRows
  items.value = proofRows
}
function pressCodeOf(id) {
  const p = presses.value.find((x) => x.id === id)
  return p ? p.pressCode : ''
}
function mountedPressLabel(p) {
  const pl = plates.value.find((x) => x.id === p.plateId)
  return pl ? (pressCodeOf(pl.pressId) || '未装机') : '—'
}
function fmt(t) {
  return t ? t.replace('T', ' ').slice(0, 16) : '—'
}
function openNew() {
  form.value = { result: '通过' }
  dialog.value = true
}
async function submit() {
  try {
    await proofApi.add({
      jobId: form.value.jobId || null,
      plateId: form.value.plateId || null,
      pressId: form.value.pressId || null,
      result: form.value.result,
      operator: form.value.operator || null,
      note: form.value.note || null
    })
    dialog.value = false
    await load()
    emit('data-changed', 'proof')
    ElMessage.success('已入账')
  } catch (e) { ElMessage.error(e.message) }
}
const off = on('data-changed', (name) => {
  if (name === 'plate' || name === 'press' || name === 'job') load()
})
onMounted(load)
onUnmounted(off)
</script>

<style scoped>
.head { display: flex; align-items: center; gap: 14px; margin-bottom: 16px; }
.head h2 { margin: 0; font-size: 20px; }
.sub { flex: 1; color: #8d92a8; font-size: 12px; }
.prime { background: var(--el-color-primary); color: #fff; border: none; border-radius: 8px;
  padding: 8px 18px; font-size: 13px; cursor: pointer; }
.table { background: #fff; border: 1px solid #e9ebf5; border-radius: 12px; overflow: hidden; }
.row { display: grid; grid-template-columns: 130px 1.3fr 1.5fr 1.2fr 70px 150px 1fr;
  gap: 8px; align-items: center; padding: 11px 14px; border-bottom: 1px solid #f3f4fa; font-size: 13px; }
.head-row { background: #f7f8fc; color: #8d92a8; font-size: 12px; }
.mono { font-family: ui-monospace, Menlo, monospace; color: #5d6280; }
.dim { color: #8d92a8; font-size: 12px; }
.result { font-weight: 600; font-size: 12px; }
.result.pass { color: #2e7d4f; }
.result.fail { color: #b4761f; }
.valid { color: #2e7d4f; font-size: 12px; font-weight: 600; }
.invalid { color: #c0392b; font-size: 12px; text-decoration: underline dotted; cursor: help; }
.empty { padding: 26px; text-align: center; color: #b6bad0; font-size: 13px; }
.fr { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.fr label { width: 56px; text-align: right; font-size: 13px; color: #71758c; }
.note { font-size: 12px; color: #b6bad0; margin: 4px 0 0 66px; line-height: 1.6; }
</style>
