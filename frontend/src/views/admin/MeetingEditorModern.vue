<template>
  <div class="page page-wide">
    <PageHeader :title="meetingId ? '编辑会议 / 排会' : '创建会议 / 排会'" subtitle="导入 Word 后先解析为议题清单，确认无误后保存。" back @back="$router.push('/meetings')">
      <template #actions>
        <el-button :icon="Back" @click="$router.push('/meetings')">返回</el-button>
        <el-button type="primary" :loading="saving" :icon="DocumentChecked" @click="saveMeeting(true)">保存</el-button>
        <el-button type="primary" :loading="publishing" :icon="Promotion" @click="publishMeeting">发布</el-button>
      </template>
    </PageHeader>

    <main class="editor-layout">
      <section class="surface base-panel">
        <h2>会议基本信息</h2>
        <el-form :model="form" label-position="top">
          <div class="two-cols">
            <el-form-item label="会议日期"><el-date-picker v-model="form.meetingDate" value-format="YYYY-MM-DD" type="date" placeholder="选择日期" /></el-form-item>
            <el-form-item label="会议时间"><el-input v-model="form.meetingTime" placeholder="例如 15:00" /></el-form-item>
          </div>
          <el-form-item label="会议期数"><el-input v-model="form.periodNo" placeholder="例如 2026年第1期" /></el-form-item>
          <el-form-item label="会议地点"><el-input v-model="form.location" placeholder="输入会议室" /></el-form-item>
          <el-form-item label="参会领导"><el-input v-model="form.leaders" placeholder="兼容字段，领导参会可后续通过人员遴选维护" /></el-form-item>
          <el-form-item label="参会人员">
            <button type="button" class="attendee-link" @click="attendeeVisible = true">
              <el-icon><View /></el-icon>
              <span>查看本次会议参会人员</span>
              <strong>{{ attendees.length }} 人</strong>
            </button>
          </el-form-item>
          <el-form-item label="会议内容"><el-input v-model="form.content" type="textarea" :rows="5" resize="none" placeholder="输入会议背景或主要内容" /></el-form-item>
        </el-form>
      </section>

      <section class="surface topic-panel">
        <div class="topic-head">
          <h2>参会议题信息</h2>
          <div>
            <el-upload :auto-upload="false" :show-file-list="false" :on-change="uploadTopicFile" accept=".doc,.docx">
              <el-button type="primary" :loading="uploading" :icon="Upload">导入议题</el-button>
            </el-upload>
            <el-button :icon="Plus" @click="addTopic">手动新增</el-button>
          </div>
        </div>
        <el-alert v-if="importMessage" class="import-tip" type="success" :closable="true" :title="importMessage" @close="importMessage = ''" />
        <el-table :data="form.topics" border row-key="sortNo" empty-text="暂无参会议题，请导入 Word 或手动新增">
          <el-table-column label="序号" width="70" align="center"><template #default="{ $index }">{{ pad($index + 1) }}</template></el-table-column>
          <el-table-column label="议题名称" min-width="240"><template #default="{ row }"><el-input v-model="row.title" placeholder="议题名称" /></template></el-table-column>
          <el-table-column label="类型" width="150"><template #default="{ row }"><el-input v-model="row.topicType" /></template></el-table-column>
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
          <el-table-column label="议题简述" min-width="240"><template #default="{ row }"><el-input v-model="row.summary" type="textarea" :rows="2" resize="none" /></template></el-table-column>
          <el-table-column label="操作" width="132" align="center" fixed="right">
            <template #default="{ $index }">
              <el-button link :icon="Top" :disabled="$index === 0" @click="moveTopic($index, -1)" />
              <el-button link :icon="Bottom" :disabled="$index === form.topics.length - 1" @click="moveTopic($index, 1)" />
              <el-button link type="danger" :icon="Delete" @click="removeTopic($index)" />
            </template>
          </el-table-column>
        </el-table>
      </section>
    </main>

    <PersonListDrawer v-model="attendeeVisible" title="本次会议参会人员" :people="attendees" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, UploadFile } from 'element-plus'
import { Back, Bottom, Delete, DocumentChecked, Plus, Promotion, Top, Upload, View } from '@element-plus/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import PersonListDrawer from '@/components/meeting/PersonListDrawer.vue'
import { api, DepartmentItem, ImportResult, MeetingItem, MeetingPayload, TopicPayload, UserItem } from '@/api/meeting'

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

const attendees = computed<UserItem[]>(() => {
  const map = new Map<string, UserItem>()
  ;(detail.value?.attendees || []).forEach((user) => map.set(String(user.id || user.userId), user))
  ;(detail.value?.topics || []).forEach((topic: any) => (topic.attendees || []).forEach((user: UserItem) => map.set(String(user.id || user.userId), user)))
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
  const [item] = form.topics.splice(index, 1)
  form.topics.splice(target, 0, item)
  refreshSort()
}

async function saveMeeting(showMessage: boolean) {
  saving.value = true
  try {
    validateBase()
    const saved = meetingId.value ? await api.updateMeeting(meetingId.value, buildPayload()) : await api.createMeeting(buildPayload())
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
    const saved = await saveMeeting(false)
    await ElMessageBox.confirm('发布后将给议题涉及部门秘书生成通知。', '确认发布')
    await api.publish(saved.id)
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
    importMessage.value = `${data.parserMessage || '议题解析成功'}，请核对后点击保存。`
  } catch (error: any) {
    ElMessage.error(error.message || '议题导入失败')
  } finally {
    uploading.value = false
  }
}

function buildPayload(): MeetingPayload {
  refreshSort()
  return {
    ...form,
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
.editor-layout {
  display: grid;
  grid-template-columns: minmax(320px, 0.36fr) minmax(0, 0.64fr);
  gap: 20px;
  align-items: start;
}

.base-panel,
.topic-panel {
  padding: 20px;
}

.base-panel {
  position: sticky;
  top: 84px;
}

h2 {
  margin: 0 0 18px;
  font-size: 17px;
}

.two-cols {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

:deep(.el-date-editor.el-input),
:deep(.el-date-editor.el-input__wrapper) {
  width: 100%;
}

.attendee-link {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  min-height: 42px;
  padding: 0 12px;
  color: #175cd3;
  background: #eff6ff;
  border: 1px solid #b2ddff;
  border-radius: 10px;
  cursor: pointer;
}

.attendee-link strong {
  margin-left: auto;
  color: #111827;
}

.topic-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.topic-head > div {
  display: flex;
  gap: 10px;
}

.import-tip {
  margin-bottom: 12px;
}

@media (max-width: 1100px) {
  .editor-layout {
    grid-template-columns: 1fr;
  }

  .base-panel {
    position: static;
  }
}

@media (max-width: 720px) {
  .two-cols,
  .topic-head {
    grid-template-columns: 1fr;
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
