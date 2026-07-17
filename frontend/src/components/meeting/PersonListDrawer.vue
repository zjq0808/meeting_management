<template>
  <el-drawer v-model="visible" :title="title" size="560px">
    <el-input v-model="keyword" class="people-search" clearable placeholder="搜索议题、部门、工号或姓名" />
    <el-table :data="filteredPeople" border>
      <el-table-column v-if="showTopic" prop="topicSortNo" label="议题序号" width="90" />
      <el-table-column v-if="showTopic" prop="topicTitle" label="议题名称" min-width="180" />
      <el-table-column prop="departmentName" label="部门" min-width="160" />
      <el-table-column prop="employeeNo" label="工号" width="130" />
      <el-table-column label="姓名" width="150">
        <template #default="{ row }">{{ row.realName || row.username }}</template>
      </el-table-column>
    </el-table>
    <el-empty v-if="filteredPeople.length === 0" description="暂无人员" />
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { UserItem } from '@/api/meeting'

const props = defineProps<{ modelValue: boolean; people: UserItem[]; title?: string; showTopic?: boolean }>()
const emit = defineEmits<{ (e: 'update:modelValue', value: boolean): void }>()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const keyword = ref('')
const filteredPeople = computed(() => {
  const key = keyword.value.trim().toLowerCase()
  if (!key) return props.people
  return props.people.filter((person) => `${person.topicSortNo || ''}${person.topicTitle || ''}${person.departmentName || ''}${person.employeeNo || person.id || ''}${person.realName || person.username || ''}`.toLowerCase().includes(key))
})
</script>

<style scoped>
.people-search {
  margin-bottom: 12px;
}
</style>
