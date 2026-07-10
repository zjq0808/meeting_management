<template>
  <div class="mobile-page">
    <van-nav-bar title="参会遴选" left-text="返回" left-arrow @click-left="$router.back()" />
    <van-cell-group inset>
      <van-cell v-for="topic in tasks" :key="topic.id" :title="topic.title" :label="topic.reportDepartmentName">
        <template #value><van-button size="small" type="primary" @click="openPicker(topic)">选择</van-button></template>
      </van-cell>
      <van-empty v-if="tasks.length === 0" description="暂无待处理议题" />
    </van-cell-group>

    <van-popup v-model:show="pickerVisible" position="bottom" round>
      <div class="picker">
        <h3>{{ activeTopic?.title }}</h3>
        <van-checkbox-group v-model="selectedIds">
          <van-cell-group>
            <van-cell v-for="user in candidates" :key="user.id" clickable :title="user.realName || user.username" :label="`${user.departmentName || '-'} / ${user.employeeNo || '-'}`" @click="toggle(user.id)">
              <template #right-icon><van-checkbox :name="user.id" /></template>
            </van-cell>
          </van-cell-group>
        </van-checkbox-group>
        <van-button block type="primary" :loading="submitting" @click="submit">提交参会名单</van-button>
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
const selectedIds = ref<number[]>([])
const activeTopic = ref<any>(null)
const pickerVisible = ref(false)
const submitting = ref(false)
const user = computed(() => store.state.user)

async function load() {
  tasks.value = await api.selectionTasks(meetingId) as any[]
  const role = user.value?.role === 'LEADER' ? 'PARTICIPANT' : 'LEADER'
  candidates.value = await api.users({ departmentId: user.value?.departmentId, role }) as any[]
}

function openPicker(topic: any) {
  activeTopic.value = topic
  selectedIds.value = []
  pickerVisible.value = true
}

function toggle(id: number) {
  const index = selectedIds.value.indexOf(id)
  if (index >= 0) selectedIds.value.splice(index, 1)
  else selectedIds.value.push(id)
}

async function submit() {
  if (!activeTopic.value) return
  submitting.value = true
  try {
    await api.submitAttendees(activeTopic.value.id, { userIds: selectedIds.value, attendeeType: user.value?.role === 'LEADER' ? 'PARTICIPANT' : 'LEADER' })
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
  padding: 16px 16px 24px;
}

h3 {
  margin: 0 0 12px;
  font-size: 18px;
}
</style>
