<template>
  <div class="mobile-page">
    <van-nav-bar title="参会遴选" left-text="返回" left-arrow @click-left="$router.back()" />
    <van-cell-group inset>
      <van-cell v-for="topic in tasks" :key="topic.id" :title="topic.title" :label="topic.reportDepartmentName">
        <template #value><van-button size="small" type="primary" @click="openPicker(topic)">选择</van-button></template>
      </van-cell>
      <van-empty v-if="tasks.length === 0" description="暂无待处理议题" />
    </van-cell-group>

    <van-popup v-model:show="pickerVisible" position="bottom" round class="selection-popup">
      <div class="picker">
        <h3>{{ activeTopic?.title }}</h3>
        <van-tabs v-model:active="attendeeType">
          <van-tab title="汇报人" name="REPORT" />
          <van-tab title="参会人" name="PARTAKE" />
        </van-tabs>
        <div class="picker-scroll">
          <van-checkbox-group v-model="selectedIds">
            <van-cell-group>
              <van-cell v-for="user in candidates" :key="user.id" clickable :title="user.realName || user.username" :label="`${user.departmentName || '-'} / ${user.employeeNo || '-'}`" @click="toggle(user.id)">
                <template #right-icon><van-checkbox :name="user.id" /></template>
              </van-cell>
            </van-cell-group>
          </van-checkbox-group>
        </div>
        <div class="picker-footer">
          <van-button block type="primary" :loading="submitting" @click="submit">提交参会名单</van-button>
        </div>
      </div>
    </van-popup>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useStore } from 'vuex'
import { showFailToast, showSuccessToast } from 'vant'
import { api } from '@/api/meeting'

const route = useRoute()
const store = useStore()
const meetingId = Number(route.params.meetingId)
const tasks = ref<any[]>([])
const candidates = ref<any[]>([])
const selectedIds = ref<string[]>([])
const attendeeType = ref<'REPORT' | 'PARTAKE'>('REPORT')
const activeTopic = ref<any>(null)
const pickerVisible = ref(false)
const submitting = ref(false)
const user = computed(() => store.state.user)

async function load() {
  tasks.value = await api.selectionTasks(meetingId) as any[]
  candidates.value = await api.users({ departmentId: user.value?.departmentId }) as any[]
}

function openPicker(topic: any) {
  activeTopic.value = topic
  selectedIds.value = []
  pickerVisible.value = true
}

function toggle(id: string) {
  const index = selectedIds.value.indexOf(id)
  if (index >= 0) selectedIds.value.splice(index, 1)
  else selectedIds.value.push(id)
}

async function submit() {
  if (!activeTopic.value) return
  submitting.value = true
  try {
    await api.submitAttendees(activeTopic.value.id, { userIds: selectedIds.value, attendeeType: attendeeType.value })
    showSuccessToast('提交成功')
    pickerVisible.value = false
    await load()
  } catch (error: any) {
    showFailToast(error.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => load().catch((error: any) => showFailToast(error.message || '加载失败')))
</script>

<style scoped>
.picker {
  display: flex;
  flex-direction: column;
  max-height: 82vh;
  padding: 16px 16px calc(16px + env(safe-area-inset-bottom));
  overflow: hidden;
}

h3 {
  margin: 0 0 12px;
  font-size: 18px;
  line-height: 1.35;
  word-break: break-word;
}

.selection-popup {
  max-height: 82vh;
  overflow: hidden;
}

.picker-scroll {
  min-height: 0;
  max-height: 48vh;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

.picker-footer {
  flex: none;
  padding-top: 12px;
  background: #fff;
}
</style>
