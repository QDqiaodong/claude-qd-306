<template>
  <div class="pane">
    <div class="head">
      <h2>校色试印台账</h2>
      <span class="sub">工单 + 印版 + 印刷机三样齐了才入账；「通过」只认眼下还算数的那一条，旧的留档备查</span>
      <button class="prime" @click="openNew">落一条试印</button>
    </div>

    <div class="table">
      <div class="row head-row">
        <span>时间</span><span>工单</span><span>印版</span><span>印刷机</span>
        <span>结果</span><span>眼下效力</span><span>记录人</span><span>备注</span>
      </div>
      <div v-for="t in items" :key="t.id" class="row">
        <span class="dim">{{ fmt(t.createdAt) }}</span>
        <span class="mono">{{ t.jobNo || '#' + t.jobId }}</span>
        <span class="mono">{{ t.plateCode || '#' + t.plateId }}</span>
        <span class="mono">{{ t.pressCode || '#' + t.pressId }}</span>
        <span class="result" :class="t.result === '通过' ? 'ok' : 'ng'">{{ t.result }}</span>
        <span>
          <i v-if="t.result !== '通过'" class="tag dim-tag">—</i>
          <i v-else-if="t.validNow" class="tag ok-tag">有效</i>
          <i v-else class="tag stale-tag">已失效</i>
        </span>
        <span>{{ t.createdBy || '—' }}</span>
        <span class="dim">{{ t.note || '—' }}</span>
      </div>
      <div v-if="!items.length" class="empty">台账还是空的，先落一条试印</div>
    </div>

    <el-dialog v-model="dialog" title="落一条试印" width="460px">
      <div class="fr">
        <label>工单</label>
        <el-select v-model="form.jobId" style="flex:1" placeholder="只列还停在待印的单子">
          <el-option v-for="j in pendingJobs" :key="j.id"
                     :label="j.jobNo + ' ' + j.clientName" :value="j.id" />
        </el-select>
      </div>
      <div class="fr">
        <label>印版</label>
        <el-select v-model="form.plateId" style="flex:1">
          <el-option v-for="p in plates" :key="p.id"
                     :label="p.plateCode + ' ' + p.plateName + '（' + p.plateState + '）'" :value="p.id" />
        </el-select>
      </div>
      <div class="fr">
        <label>印刷机</label>
        <el-select v-model="form.pressId" style="flex:1">
          <el-option v-for="p in presses" :key="p.id"
                     :label="p.pressCode + ' ' + p.pressName + '（' + p.pressState + '）'" :value="p.id" />
        </el-select>
      </div>
      <div class="fr">
        <label>结果</label>
        <el-radio-group v-model="form.result">
          <el-radio value="通过">通过</el-radio>
          <el-radio value="不通过">不通过</el-radio>
        </el-radio-group>
      </div>
      <div class="fr"><label>记录人</label><el-input v-model="form.createdBy" placeholder="谁落的这条账" /></div>
      <div class="fr"><label>备注</label><el-input v-model="form.note" placeholder="可空" /></div>
      <p class="note">记「通过」时：版必须正好装在这台机上、机器得在跑、版得在用；一张待印单只认一条有效通过。</p>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="submit">入账</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { testPrintApi, jobApi, plateApi, pressApi } from '../api'
import { emit } from '../utils/bus'

const route = useRoute()
const items = ref([])
const jobs = ref([])
const plates = ref([])
const presses = ref([])
const dialog = ref(false)
const form = ref({})

const pendingJobs = computed(() => jobs.value.filter((j) => j.jobState === '待印'))

async function load() {
  const [rows, jobRows, plateRows, pressRows] = await Promise.all([
    testPrintApi.list(), jobApi.search({}), plateApi.list(), pressApi.list()
  ])
  items.value = rows
  jobs.value = jobRows
  plates.value = plateRows
  presses.value = pressRows
}
function fmt(s) {
  return s ? String(s).replace('T', ' ').slice(0, 16) : '—'
}
function openNew() {
  form.value = { result: '通过' }
  const pre = Number(route.query.jobId)
  if (pre && pendingJobs.value.some((j) => j.id === pre)) form.value.jobId = pre
  dialog.value = true
}
async function submit() {
  try {
    await testPrintApi.add(form.value)
    dialog.value = false
    await load()
    emit('data-changed', 'test-print')
    ElMessage.success('已入账')
  } catch (e) { ElMessage.error(e.message) }
}
onMounted(load)
</script>

<style scoped>
.head { display: flex; align-items: center; gap: 14px; margin-bottom: 16px; }
.head h2 { margin: 0; font-size: 20px; }
.sub { flex: 1; color: #8d92a8; font-size: 12px; }
.prime { background: var(--el-color-primary); color: #fff; border: none; border-radius: 8px;
  padding: 8px 18px; font-size: 13px; cursor: pointer; }
.table { background: #fff; border: 1px solid #e9ebf5; border-radius: 12px; overflow: hidden; }
.row { display: grid; grid-template-columns: 140px 90px 100px 100px 70px 90px 80px 1fr;
  gap: 8px; align-items: center; padding: 11px 14px; border-bottom: 1px solid #f3f4fa; font-size: 13px; }
.head-row { background: #f7f8fc; color: #8d92a8; font-size: 12px; }
.mono { font-family: ui-monospace, Menlo, monospace; color: #8d92a8; }
.dim { color: #8d92a8; font-size: 12px; }
.result.ok { color: #2e7d4f; font-weight: 600; }
.result.ng { color: #c0392b; }
.tag { font-style: normal; font-size: 12px; border-radius: 10px; padding: 2px 10px; }
.ok-tag { background: #e6f4ec; color: #2e7d4f; }
.stale-tag { background: #fdf0e3; color: #b4761f; }
.dim-tag { color: #b6bad0; }
.empty { padding: 26px; text-align: center; color: #b6bad0; font-size: 13px; }
.fr { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.fr label { width: 64px; text-align: right; font-size: 13px; color: #71758c; }
.note { font-size: 12px; color: #b6bad0; margin: 4px 0 0 74px; }
</style>
