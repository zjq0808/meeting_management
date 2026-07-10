<template>
  <el-drawer v-model="visible" :title="title" size="560px">
    <el-table :data="people" border>
      <el-table-column prop="departmentName" label="部门" min-width="160" />
      <el-table-column prop="employeeNo" label="工号" width="130" />
      <el-table-column label="姓名" width="150">
        <template #default="{ row }">{{ row.realName || row.username }}</template>
      </el-table-column>
    </el-table>
    <el-empty v-if="people.length === 0" description="暂无人员" />
  </el-drawer>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { UserItem } from '@/api/meeting'

const props = defineProps<{ modelValue: boolean; people: UserItem[]; title?: string }>()
const emit = defineEmits<{ (e: 'update:modelValue', value: boolean): void }>()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})
</script>
