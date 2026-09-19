<template>
  <div class="pg">
    <div class="hd">
      <h2>印刷工单</h2>
      <span class="hint">上半是待处理的，下半是处理过的，中间可以互相搬。</span>
      <input class="search" v-model="kw" placeholder="搜索编号或名称" />
      <button class="btn solid" @click="openNew">新增</button>
    </div>
    <div class="ss">
      <div class="half top">
        <div class="hh">待处理（{{ doing.length }}）</div>
        <div class="hlist">
          <div class="hi" v-for="it in doing" :key="it.id" :class="{ sel: sel.includes(it.id) }"
               @click="togglePick(it.id)">
            <span class="box">{{ sel.includes(it.id) ? '☑' : '☐' }}</span>
            <span class="hc">{{ it.code }}</span>
            <span class="hn">{{ it.name }}</span>
            <span class="hs">{{ it[ST] }}</span>
          </div>
        </div>
      </div>
      <div class="mid">
        <button class="btn solid" :disabled="!sel.length" @click="move(true)">↓ 标记完成</button>
        <button class="btn" :disabled="!sel.length" @click="move(false)">↑ 退回</button>
      </div>
      <div class="half bottom">
        <div class="hh">已处理（{{ done.length }}）</div>
        <div class="hlist">
          <div class="hi" v-for="it in done" :key="it.id">
            <span class="hc">{{ it.code }}</span>
            <span class="hn">{{ it.name }}</span>
            <span class="hs">{{ it[ST] }}</span>
            <button class="btn sm" @click="openEdit(it)">改</button>
          </div>
        </div>
      </div>
    </div>
  </div>

    <el-dialog v-model="show" :title="form.id ? '修改' : '新增'" width="440px">
      <div class="frm">
        <div class="fr" v-for="fd in FORM_FIELDS" :key="fd.k">
          <label>{{ fd.l }}</label>
          <el-input v-model="form[fd.k]" :placeholder="'请填写' + fd.l" />
        </div>
      </div>
      <template #footer>
        <el-button @click="show = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { jobApi } from '../api'

const rows = ref([])
const show = ref(false)
const form = ref({})
const kw = ref('')
const FORM_FIELDS = [{"k":"code","l":"编号"},{"k":"client","l":"客户"},{"k":"paperId","l":"用纸"},{"k":"copies","l":"印刷份数"},{"k":"printDate","l":"交期"},{"k":"status","l":"状态"}]
const BODY_FIELDS = [{"k":"client","l":"客户"},{"k":"paperId","l":"用纸"},{"k":"copies","l":"印刷份数"},{"k":"printDate","l":"交期"}]
const ST = 'status'
const OPTS = ["印刷中","待印","已完成"]

const picked = ref([])
const filtered = computed(() => {
  if (!kw.value) return rows.value
  const k = kw.value.toLowerCase()
  return rows.value.filter(r => (r.code || '').toLowerCase().includes(k) || (r.name || '').toLowerCase().includes(k))
})

async function load() { rows.value = await jobApi.list() }
function openNew() { form.value = {}; show.value = true }
function openEdit(it) { form.value = { ...it }; show.value = true }
async function save() {
  try {
    if (form.value.id) await jobApi.update(form.value.id, form.value)
    else await jobApi.create(form.value)
    show.value = false
    await load()
    ElMessage.success('已保存')
  } catch (e) { ElMessage.error(e.message) }
}
async function patch(it, key, value) {
  try {
    await jobApi.update(it.id, { [key]: value })
    await load()
    ElMessage.success('已更新')
  } catch (e) { ElMessage.error(e.message); await load() }
}
function togglePick(id) {
  const i = picked.value.indexOf(id)
  if (i >= 0) picked.value.splice(i, 1)
  else picked.value.push(id)
}
onMounted(load)
const sel = picked
const LAST = OPTS[OPTS.length - 1]
const doing = computed(() => rows.value.filter(r => r[ST] !== LAST))
const done = computed(() => rows.value.filter(r => r[ST] === LAST))
async function move(toDone) {
  for (const id of sel.value) {
    const it = rows.value.find(r => r.id === id)
    if (!it) continue
    try { await jobApi.update(id, { [ST]: toDone ? LAST : OPTS[0] }) }
    catch (e) { ElMessage.error(e.message) }
  }
  sel.value = []
  await load()
}
</script>
<style scoped>
.pg { padding: 4px 2px 40px; color: #303133; }
.pg h2 { margin: 0; font-size: 19px; }
.hd { display: flex; align-items: center; gap: 14px; margin-bottom: 16px; flex-wrap: wrap; }
.hd .hint { color: #888; font-size: 13px; flex: 1; }
.btn { border: 1px solid var(--el-color-primary); background: #fff; color: var(--el-color-primary);
  border-radius: 6px; padding: 6px 14px; cursor: pointer; font-size: 13px; }
.btn:hover { background: var(--el-color-primary-light-9); }
.btn.solid { background: var(--el-color-primary); color: #fff; }
.btn.sm { padding: 3px 10px; font-size: 12px; }
.frm .fr { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.frm .fr label { width: 88px; text-align: right; color: #666; font-size: 13px; }
.blank { color: #bbb; padding: 30px; text-align: center; }
.search { display: none; }
.ss { display: flex; flex-direction: column; gap: 10px; }
.half { background: #fff; border: 1px solid #eee; border-radius: 10px; padding: 12px; min-height: 180px; }
.hh { font-size: 13px; color: #888; margin-bottom: 8px; }
.hlist { max-height: 240px; overflow: auto; }
.hi { display: grid; grid-template-columns: 26px 110px 1fr 100px 60px; gap: 8px; align-items: center;
  padding: 8px 6px; border-bottom: 1px solid #f7f7f7; font-size: 13px; cursor: pointer; border-radius: 5px; }
.hi.sel { background: var(--el-color-primary-light-9); }
.box { color: var(--el-color-primary); }
.hc { font-family: ui-monospace, monospace; color: #aaa; }
.hs { color: var(--el-color-primary-dark-2); }
.mid { display: flex; gap: 10px; justify-content: center; }
.mid .btn:disabled { opacity: .45; cursor: not-allowed; }

</style>
