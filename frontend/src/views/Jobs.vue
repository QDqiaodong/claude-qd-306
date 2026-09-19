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
        <span class="r">份数</span><span>交期</span><span>状态</span><span>操作</span>
      </div>
      <div v-for="j in items" :key="j.id" class="row">
        <span class="mono">{{ j.jobNo }}</span>
        <span>{{ j.clientName }}</span>
        <span>{{ paperName(j.paperId) }}</span>
        <span>{{ plateCode(j.plateId) }}</span>
        <span class="r">{{ j.copies }}</span>
        <span class="dim">{{ j.dueDate }}</span>
        <span class="state" :class="stateTone(j.jobState)">{{ j.jobState }}</span>
        <span>
          <button v-if="j.jobState !== '已完成'" class="ghost small" @click="advance(j)">推进</button>
        </span>
      </div>
      <div v-if="!items.length" class="empty">没有符合条件的工单</div>
    </div>

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
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { jobApi, paperApi, plateApi } from '../api'
import { emit } from '../utils/bus'

const STATES = ['待印', '印刷中', '已完成']
const FLOW = { 待印: '印刷中', 印刷中: '已完成' }

const items = ref([])
const papers = ref([])
const plates = ref([])
const dialog = ref(false)
const form = ref({})
const query = reactive({ client: '', state: '', paperId: '', dueFrom: '', dueTo: '' })

function params() {
  const p = {}
  Object.keys(query).forEach((k) => { if (query[k] !== '' && query[k] != null) p[k] = query[k] })
  return p
}
async function run() {
  items.value = await jobApi.search(params())
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
.row { display: grid; grid-template-columns: 110px 1.2fr 100px 100px 70px 110px 90px 80px;
  gap: 8px; align-items: center; padding: 11px 14px; border-bottom: 1px solid #f3f4fa; font-size: 13px; }
.head-row { background: #f7f8fc; color: #8d92a8; font-size: 12px; }
.mono { font-family: ui-monospace, Menlo, monospace; color: #8d92a8; }
.r { text-align: right; }
.dim { color: #8d92a8; font-size: 12px; }
.state { font-size: 12px; }
.state.doing { color: var(--el-color-primary-dark-2); font-weight: 600; }
.state.done { color: #2e7d4f; }
.ghost { background: #fff; border: 1px solid var(--el-color-primary-light-7); color: var(--el-color-primary-dark-2);
  border-radius: 7px; padding: 6px 14px; font-size: 12px; cursor: pointer; }
.ghost.small { padding: 4px 10px; }
.empty { padding: 26px; text-align: center; color: #b6bad0; font-size: 13px; }
.fr { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.fr label { width: 64px; text-align: right; font-size: 13px; color: #71758c; }
.note { font-size: 12px; color: #b6bad0; margin: 4px 0 0 74px; }
</style>
