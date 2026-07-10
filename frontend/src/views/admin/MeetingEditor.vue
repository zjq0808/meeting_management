<template>
  <div class="page editor-page">
    <div class="toolbar editor-topbar">
      <div>
        <h2>{{ meetingId ? '编辑会议 / 排会' : '创建会议 / 排会' }}</h2>
        <p class="muted">导入 Word 后先解析为议题清单，确认无误后保存。</p>
      </div>
      <div class="top-actions">
        <el-button :icon="Back" @click="$router.push('/meetings')">返回</el-button>
        <el-button type="primary" class="action-button" :icon="DocumentChecked" :loading="saving" @click="saveMeeting(true)">保存</el-button>
        <el-button type="primary" class="action-button" :icon="Promotion" :loading="publishing" @click="publishMeeting">发布</el-button>
      </div>
    </div>

    <main class="editor-layout">
      <section class="panel base-panel">
        <div class="panel-header">
          <h3><el-icon><Document /></el-icon>会议基本信息</h3>
        </div>
        <el-form :model="form" label-position="top" class="base-form">
          <div class="two-cols">
            <el-form-item label="会议日期"><el-date-picker v-model="form.meetingDate" value-format="YYYY-MM-DD" type="date" placeholder="选择日期" /></el-form-item>
            <el-form-item label="会议时间"><el-time-picker v-model="form.meetingTime" value-format="HH:mm" format="HH:mm" placeholder="选择时间" /></el-form-item>
          </div>
          <el-form-item label="会议期数"><el-input v-model="form.periodNo" placeholder="如：2026年第1期" /></el-form-item>
          <el-form-item label="会议地点"><el-input v-model="form.location" placeholder="输入会议室" /></el-form-item>
          <el-form-item label="参会领导"><el-input v-model="form.leaders" placeholder="兼容字段，领导名单可后续通过参会人员维护" /></el-form-item>
          <el-form-item label="参会人员">
            <div class="attendee-entry" @click="attendeeVisible = true">
              <el-icon><View /></el-icon>
              <span>查看本次会议参会人员</span>
              <strong>{{ attendees.length }} 人</strong>
            </div>
          </el-form-item>
          <el-form-item label="会议内容"><el-input v-model="form.content" type="textarea" :rows="5" resize="none" placeholder="输入会议背景或主要内容" /></el-form-item>
        </el-form>
      </section>

      <section class="panel topic-panel">
        <div class="panel-header">
          <h3><el-icon><List /></el-icon>参会议题信息</h3>
          <div class="topic-tools">
            <el-upload :auto-upload="false" :show-file-list="false" :on-change="uploadTopicFile" accept=".doc,.docx">
              <el-button type="primary" class="action-button" :icon="Upload" :loading="uploading">导入议题</el-button>
            </el-upload>
            <el-button :icon="Plus" @click="addTopic">手动新增</el-button>
          </div>
        </div>

        <el-alert v-if="importMessage" class="import-tip" type="success" :closable="true" :title="importMessage" @close="importMessage = ''" />

        <el-table :data="sortedTopics" border row-key="sortNo" empty-text="暂无参会议题，请导入 Word 或手动新增">
          <el-table-column label="序号" width="76" align="center">
            <template #default="{ $index }"><span class="serial">{{ pad($index + 1) }}</span></template>
          </el-table-column>
          <el-table-column label="议题名称" min-width="240">
            <template #default="{ row }"><el-input v-model="row.title" placeholder="议题名称" /></template>
          </el-table-column>
          <el-table-column label="类型" width="150">
            <template #default="{ row }"><el-input v-model="row.topicType" placeholder="三重一大" /></template>
          </el-table-column>
          <el-table-column label="汇报部门" width="190">
            <template #default="{ row }">
              <el-select v-model="row.reportDepartmentId" clearable filterable placeholder="选择部门">
                <el-option v-for="dept in departments" :key="dept.id" :label="dept.name" :value="dept.id" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="参会部门" width="240">
            <template #default="{ row }">
              <el-select v-model="row.participantDepartmentIds" multiple collapse-tags filterable placeholder="选择部门">
                <el-option v-for="dept in departments" :key="dept.id" :label="dept.name" :value="dept.id" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="议题简述" min-width="260">
            <template #default="{ row }"><el-input v-model="row.summary" type="textarea" :rows="2" resize="none" /></template>
          </el-table-column>
          <el-table-column label="操作" width="128" align="center" fixed="right">
            <template #default="{ $index }">
              <el-button link :icon="Top" :disabled="$index === 0" @click="moveTopic($index, -1)" />
              <el-button link :icon="Bottom" :disabled="$index === form.topics.length - 1" @click="moveTopic($index, 1)" />
              <el-button link type="danger" :icon="Delete" @click="removeTopic($index)" />
            </template>
          </el-table-column>
        </el-table>
      </section>
    </main>

    <el-drawer v-model="attendeeVisible" title="本次会议参会人员" size="560px">
      <el-table :data="attendees" border>
        <el-table-column prop="departmentName" label="部门" />
        <el-table-column prop="employeeNo" label="工号" width="140" />
        <el-table-column label="姓名" width="140"><template #default="{ row }">{{ row.realName || row.username }}</template></el-table-column>
      </el-table>
      <el-empty v-if="attendees.length === 0" description="尚未选择参会人员" />
    </el-drawer>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, UploadFile } from 'element-plus'
import { Back, Bottom, Delete, Document, DocumentChecked, List, Plus, Promotion, Top, Upload, View } from '@element-plus/icons-vue'
import { api, DepartmentItem, ImportResult, MeetingItem, MeetingPayload, TopicPayload } from '@/api/meeting'

const route = useRoute()
const router = useRouter()
const meetingId = computed(() => route.params.id ? Number(route.params.id) : 0)
const departments = ref<DepartmentItem[]>([])
const detail = ref<MeetingItem | null>(null)
const attendeeVisible = ref(false)
const saving = ref(false)
const publishing = ref(false)
const uploading = ref(false)
const importMessage = ref('')
const form = reactive<MeetingPayload>({ meetingDate: '', meetingTime: '', periodNo: '', location: '', leaders: '', content: '', topics: [] })

const sortedTopics = computed(() => form.topics)
const attendees = computed(() => {
  const map = new Map<string, any>()
  ;(detail.value?.attendees || []).forEach((user: any) => map.set(String(user.id || user.userId), user))
  form.topics.forEach((topic: any) => (topic.attendees || []).forEach((user: any) => map.set(String(user.id || user.userId), user)))
  return Array.from(map.values())
})

async function load() {
  departments.value = await api.departments() as DepartmentItem[]
  if (!meetingId.value) return
  const data = await api.meetingDetail(meetingId.value) as MeetingItem
  detail.value = data
  Object.assign(form, {
    meetingDate: data.meetingDate || '',
    meetingTime: data.meetingTime || '',
    periodNo: data.periodNo || '',
    location: data.location || '',
    leaders: data.leaders || '',
    content: data.content || '',
    topics: (data.topics || []).map(normalizeTopic)
  })
}

function addTopic() {
  form.topics.push({ topicType: '三重一大', title: '', sortNo: form.topics.length + 1, participantDepartmentIds: [] })
}

function removeTopic(index: number) {
  form.topics.splice(index, 1)
  refreshSort()
}

function moveTopic(index: number, step: number) {
  const target = index + step
  if (target < 0 || target >= form.topics.length) return
  const current = form.topics[index]
  form.topics.splice(index, 1)
  form.topics.splice(target, 0, current)
  refreshSort()
}

async function saveMeeting(showMessage: boolean) {
  saving.value = true
  try {
    validateBase()
    const payload = buildPayload()
    const saved = meetingId.value ? await api.updateMeeting(meetingId.value, payload) : await api.createMeeting(payload)
    detail.value = saved as MeetingItem
    if (showMessage) ElMessage.success('保存成功')
    if (!meetingId.value) await router.replace(`/meetings/${(saved as MeetingItem).id}/edit`)
    return saved as MeetingItem
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
    throw error
  } finally {
    saving.value = false
  }
}

async function publishMeeting() {
  publishing.value = true
  try {
    const saved = meetingId.value ? await saveMeeting(false) : await saveMeeting(false)
    await ElMessageBox.confirm('发布后将给议题涉及部门秘书生成通知。', '确认发布')
    detail.value = await api.publish(saved.id) as MeetingItem
    ElMessage.success('发布成功')
    await router.push(`/meetings/${saved.id}/detail`)
  } catch (error: any) {
    if (error !== 'cancel') ElMessage.error(error.message || '发布失败')
  } finally {
    publishing.value = false
  }
}

async function uploadTopicFile(file: UploadFile) {
  if (!file.raw) return
  uploading.value = true
  try {
    const currentId = meetingId.value || (await saveMeeting(false)).id
    const data = await api.importTopics(currentId, file.raw) as ImportResult
    form.topics = data.topics.map((topic, index) => normalizeTopic({ ...topic, sortNo: index + 1 }))
    importMessage.value = `${data.parserMessage}，请核对后点击保存。`
    ElMessage.success('议题解析成功')
  } catch (error: any) {
    ElMessage.error(error.message || '议题导入失败')
  } finally {
    uploading.value = false
  }
}

function buildPayload(): MeetingPayload {
  refreshSort()
  return {
    meetingDate: form.meetingDate,
    meetingTime: form.meetingTime,
    periodNo: form.periodNo,
    location: form.location,
    leaders: form.leaders,
    content: form.content,
    topics: form.topics.map((topic) => ({
      ...topic,
      reportDepartmentName: departmentName(topic.reportDepartmentId),
      participantDepartments: departmentNames(topic.participantDepartmentIds).join(',')
    }))
  }
}

function validateBase() {
  if (!form.meetingDate || !form.meetingTime || !form.periodNo || !form.location) {
    throw new Error('请完整填写会议日期、时间、期数和地点')
  }
}

function normalizeTopic(topic: any): TopicPayload {
  return {
    id: topic.id,
    topicType: topic.topicType || '三重一大',
    title: topic.title || '',
    reportDepartmentId: topic.reportDepartmentId ? String(topic.reportDepartmentId) : undefined,
    reportDepartmentName: topic.reportDepartmentName,
    participantDepartmentIds: normalizeDepartmentIds(topic.participantDepartmentIds || topic.participantDeptId),
    participantDepartments: topic.participantDepartments || topic.participantDeptName,
    summary: topic.summary || '',
    sortNo: Number(topic.sortNo || form.topics.length + 1)
  }
}

function normalizeDepartmentIds(value: unknown): string[] {
  if (Array.isArray(value)) return value.map((item) => String(item)).filter(Boolean)
  if (typeof value === 'string') return value.split(',').map((item) => item.trim()).filter(Boolean)
  return []
}

function refreshSort() {
  form.topics.forEach((topic, index) => { topic.sortNo = index + 1 })
}

function departmentName(id?: string) {
  return departments.value.find((dept) => dept.id === id)?.name || ''
}

function departmentNames(ids?: string[]) {
  const selected = new Set(ids || [])
  return departments.value.filter((item) => selected.has(item.id)).map((item) => item.name)
}

function pad(value: number) {
  return String(value).padStart(2, '0')
}

onMounted(() => load().catch((error: any) => ElMessage.error(error.message || '加载失败')))
</script>

<style scoped>
.editor-page {
  max-width: 1600px;
}

.editor-topbar {
  position: sticky;
  top: 0;
  z-index: 20;
  padding: 12px 0;
  background: #f5f7fa;
}

.top-actions,
.topic-tools {
  display: flex;
  align-items: center;
  gap: 10px;
}

.editor-layout {
  display: grid;
  grid-template-columns: minmax(320px, 0.38fr) minmax(0, 0.62fr);
  gap: 22px;
  align-items: start;
}

.base-panel,
.topic-panel {
  min-height: calc(100vh - 150px);
}

.base-panel {
  position: sticky;
  top: 84px;
}

.two-cols {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

:deep(.el-date-editor.el-input),
:deep(.el-date-editor.el-input__wrapper) {
  width: 100%;
}

.attendee-entry {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  min-height: 40px;
  padding: 0 12px;
  color: #1a73e8;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.15s ease, border-color 0.15s ease;
}

.attendee-entry:hover {
  background: #dbeafe;
  border-color: #93c5fd;
}

.attendee-entry strong {
  margin-left: auto;
  color: #1f2937;
}

.import-tip {
  margin-bottom: 12px;
}

.serial {
  color: #6b7280;
  font-weight: 700;
}

@media (max-width: 1100px) {
  .editor-layout {
    grid-template-columns: 1fr;
  }

  .base-panel {
    position: static;
    min-height: auto;
  }
}

@media (max-width: 720px) {
  .top-actions,
  .topic-tools {
    flex-wrap: wrap;
  }

  .two-cols {
    grid-template-columns: 1fr;
  }
}
</style>
