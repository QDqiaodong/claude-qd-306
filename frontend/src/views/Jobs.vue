<template>
  <div class="pane">
    <div class="head">
      <h2>印刷工单</h2>
      <span class="sub">条件可以自由叠加（客户 / 状态 / 用纸 / 交期区间），条件给了才拼进查询</span>
      <button class="prime" @click="openNew">开一张工单</button>
    </div>

    <div class="filters">
      <input v-model="query.client" class="inp" placeholder="客户名" @keyup.enter="run" />
      <select v-model="query.state" class="inp">
        <option value="">全部状态</option>
        <option v-for="s in STATES" :key="s" :value="s">{{ s }}</option>
      </select>
      <select v-model="query.paperId" class="inp">
        <option value="">全部用纸</option>
        <option v-for="p in papers" :key="p.id" :value="p.id">{{ p.paperName }}</option>
      </select>
      <input v-model="query.dueFrom" class="inp date" placeholder="交期起 2026-09-01" />
      <input v-model="query.dueTo" class="inp date" placeholder="交期止 2026-09-30" />
      <button class="prime" @click="run">筛选</button>
      <button class="ghost" @click="reset">清空</button>
    </div>

    <div class="chips">
      <span class="chip" v-if="query.client">客户含「{{ query.client }}」<i @click="query.client = ''">×</i></span>
      <span class="chip" v-if="query.state">状态：{{ query.state }}<i @click="query.state = ''">×</i></span>
      <span class="chip" v-if="query.paperId">用纸：{{ paperName(query.paperId) }}<i @click="query.paperId = ''">×</i></span>
      <span class="chip" v-if="query.dueFrom">交期 ≥ {{ query.dueFrom }}<i @click="query.dueFrom = ''">×</i></span>
      <span class="chip" v-if="query.dueTo">交期 ≤ {{ query.dueTo }}<i @click="query.dueTo = ''">×</i></span>
      <span class="count">命中 {{ items.length }} 张</span>
    </div>

    <div class="table">
      <div class="row head-row">
        <span>工单号</span><span>客户</span><span>用纸</span><span>印版</span>
        <span class="r">份数</span><span>交期</span><span>已校色</span><span>状态</span><span>操作</span>
      </div>
      <div v-for="j in items" :key="j.id" class="row">
        <span class="mono">{{ j.jobNo }}</span>
        <span>{{ j.clientName }}</span>
        <span>{{ paperName(j.paperId) }}</span>
        <span>{{ plateCode(j.plateId) }}</span>
        <span class="r">{{ j.copies }}</span>
        <span class="dim">{{ j.dueDate }}</span>
        <ProofTag :p="proofOf(j.id)" />
        <span class="state" :class="stateTone(j.jobState)">{{ j.jobState }}</span>
        <span>
          <el-tooltip v-if="j.jobState === '待印' && blockReason(j)" :content="blockReason(j)" placement="top">
            <span class="blocked-wrap"><button class="ghost small blocked" disabled>推进</button></span>
          </el-tooltip>
          <button v-else-if="j.jobState !== '已完成'" class="ghost small" @click="advance(j)">推进</button>
        </span>
      </div>
      <div v-if="!items.length" class="empty">没有符合条件的工单</div>
    </div>
    <p class="tip">待印单没有眼下仍然有效的校色「通过」，推进印刷中会被后台直接挡回；去「校色试印」页落一条试印。</p>

    <el-dialog v-model="dialog" title="开一张工单" width="450px">
      <div class="fr"><label>工单号</label><el-input v-model="form.jobNo" /></div>
      <div class="fr"><label>客户</label><el-input v-model="form.clientName" /></div>
      <div class="fr">
        <label>用纸</label>
        <el-select v-model="form.paperId" style="flex:1">
          <el-option v-for="p in papers" :key="p.id" :label="p.paperName + '（' + p.paperState + '）'" :value="p.id" />
        </el-select>
      </div>
      <div class="fr">
        <label>印版</label>
        <el-select v-model="form.plateId" style="flex:1">
          <el-option v-for="p in plates" :key="p.id" :label="p.plateCode + ' ' + p.plateName" :value="p.id" />
        </el-select>
      </div>
      <div class="fr"><label>份数</label><el-input v-model="form.copies" /></div>
      <div class="fr"><label>交期</label><el-input v-model="form.dueDate" placeholder="2026-09-25" /></div>
      <p class="note">纸不够或者版已作废，开单会被拦住。</p>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { h, onMounted, onUnmounted, reactive, ref } from 'vue'
import { ElMessage, ElTooltip } from 'element-plus'
import { jobApi, paperApi, plateApi, proofApi } from '../api'
import { emit, on } from '../utils/bus'

const STATES = ['待印', '印刷中', '已完成']
const FLOW = { 待印: '印刷中', 印刷中: '已完成' }

const items = ref([])
const papers = ref([])
const plates = ref([])
const proofs = ref([])
const dialog = ref(false)
const form = ref({})
const query = reactive({ client: '', state: '', paperId: '', dueFrom: '', dueTo: '' })

// 已校色标签：有通过记录只是底子，眼下装版/机态对得上才是绿色「有效」
const ProofTag = {
  props: { p: { type: Object, default: null } },
  setup(props) {
    return () => {
      if (!props.p) return h('span', { class: 'proof none' }, '未校色')
      if (props.p.result !== '通过') return h('span', { class: 'proof none' }, '未通过')
      if (props.p.validNow) return h('span', { class: 'proof ok' }, '已通过 · 有效')
      return h(ElTooltip, { content: props.p.invalidReason, placement: 'top' }, {
        default: () => h('span', { class: 'proof bad' }, '通过 · 已失效')
      })
    }
  }
}

function params() {
  const p = {}
  Object.keys(query).forEach((k) => { if (query[k] !== '' && query[k] != null) p[k] = query[k] })
  return p
}
async function run() {
  const [jobRows, proofRows] = await Promise.all([jobApi.search(params()), proofApi.list()])
  items.value = jobRows
  proofs.value = proofRows
}
function proofOf(jobId) {
  return proofs.value.find((p) => p.jobId === jobId && p.result === '通过')
}
// 推进前的闸门（真拦在后台保存上，这里只负责把按钮先变灰并把原因摆出来）
function blockReason(j) {
  if (j.jobState !== '待印') return ''
  const p = proofOf(j.id)
  if (!p) return '还没有「通过」的校色试印，先去校色试印页落一条通过'
  if (!p.validNow) return p.invalidReason
  return ''
}
async function reset() {
  Object.keys(query).forEach((k) => { query[k] = '' })
  await run()
}
function paperName(id) {
  const p = papers.value.find((x) => x.id === id)
  return p ? p.paperName : '未指定'
}
function plateCode(id) {
  const p = plates.value.find((x) => x.id === id)
  return p ? p.plateCode : '未指定'
}
function stateTone(s) {
  return s === '已完成' ? 'done' : s === '印刷中' ? 'doing' : ''
}
function openNew() {
  form.value = {}
  dialog.value = true
}
async function submit() {
  try {
    await jobApi.add(form.value)
    dialog.value = false
    await run()
    emit('data-changed', 'job')
    ElMessage.success('开好了')
  } catch (e) { ElMessage.error(e.message) }
}
async function advance(j) {
  try {
    await jobApi.save(j.id, { jobState: FLOW[j.jobState] })
    await run()
    emit('data-changed', 'job')
    ElMessage.success('已推进')
  } catch (e) { ElMessage.error(e.message); await run() }
}

onMounted(async () => {
  papers.value = await paperApi.list()
  plates.value = await plateApi.list()
  await run()
})
// 印版换机器、机器停机、新落试印，都让「已校色」列和推进按钮立刻按眼下状态重算
const off = on('data-changed', (name) => {
  if (name === 'plate' || name === 'press' || name === 'proof') run()
})
onUnmounted(off)
</script>

<style scoped>
.head { display: flex; align-items: center; gap: 14px; margin-bottom: 16px; }
.head h2 { margin: 0; font-size: 20px; }
.sub { flex: 1; color: #8d92a8; font-size: 12px; }
.prime { background: var(--el-color-primary); color: #fff; border: none; border-radius: 8px;
  padding: 8px 18px; font-size: 13px; cursor: pointer; }
.filters { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 12px; }
.inp { border: 1px solid #e0e3ef; border-radius: 8px; padding: 8px 10px; font-size: 13px; background: #fff; }
.inp.date { width: 168px; }
.chips { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; margin-bottom: 14px; }
.chip { background: var(--el-color-primary-light-9); color: var(--el-color-primary-dark-2);
  border-radius: 14px; padding: 3px 12px; font-size: 12px; }
.chip i { font-style: normal; margin-left: 6px; cursor: pointer; opacity: .6; }
.count { margin-left: auto; font-size: 12px; color: #8d92a8; }
.table { background: #fff; border: 1px solid #e9ebf5; border-radius: 12px; overflow: hidden; }
.row { display: grid; grid-template-columns: 100px 1.1fr 90px 90px 64px 100px 120px 76px 76px;
  gap: 8px; align-items: center; padding: 11px 14px; border-bottom: 1px solid #f3f4fa; font-size: 13px; }
.head-row { background: #f7f8fc; color: #8d92a8; font-size: 12px; }
.mono { font-family: ui-monospace, Menlo, monospace; color: #8d92a8; }
.r { text-align: right; }
.dim { color: #8d92a8; font-size: 12px; }
.proof { font-size: 12px; border-radius: 11px; padding: 2px 9px; white-space: nowrap; }
.proof.ok { background: #e8f6ee; color: #2e7d4f; font-weight: 600; }
.proof.bad { background: #fdecea; color: #c0392b; text-decoration: underline dotted; cursor: help; }
.proof.none { color: #b6bad0; }
.state { font-size: 12px; }
.state.doing { color: var(--el-color-primary-dark-2); font-weight: 600; }
.state.done { color: #2e7d4f; }
.ghost { background: #fff; border: 1px solid var(--el-color-primary-light-7); color: var(--el-color-primary-dark-2);
  border-radius: 7px; padding: 6px 14px; font-size: 12px; cursor: pointer; }
.ghost.small { padding: 4px 10px; }
.ghost.blocked { color: #b6bad0; border-color: #dfe2ee; cursor: not-allowed; }
.tip { font-size: 12px; color: #b6bad0; margin: 12px 4px 0; }
.empty { padding: 26px; text-align: center; color: #b6bad0; font-size: 13px; }
.fr { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.fr label { width: 64px; text-align: right; font-size: 13px; color: #71758c; }
.note { font-size: 12px; color: #b6bad0; margin: 4px 0 0 74px; }
</style>
