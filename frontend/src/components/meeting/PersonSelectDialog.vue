<template>
  <el-dialog v-model="visible" :title="title" width="780px" destroy-on-close class="person-dialog">
    <div class="person-selector">
      <aside>
        <el-input v-model="departmentKeyword" clearable placeholder="搜索部门" />
        <button
          v-for="department in filteredDepartments"
          :key="department.id"
          :class="{ active: selectedDepartment === department.id }"
          @click="selectedDepartment = department.id"
        >
          {{ department.name }}
        </button>
      </aside>
      <section>
        <el-alert v-if="hint" :title="hint" type="warning" :closable="false" show-icon class="person-selector__hint" />
        <el-input v-model="keyword" clearable placeholder="搜索姓名或工号" />
        <el-checkbox-group v-model="checked" class="person-selector__grid">
          <label v-for="user in filteredUsers" :key="user.id" class="person-selector__item">
            <el-checkbox :label="String(user.id)" :disabled="(disabledIds || []).includes(String(user.id))" />
            <span class="person-avatar">{{ firstChar(user.realName || user.username) }}</span>
            <span>
              <strong>{{ user.realName || user.username }}</strong>
              <em>{{ user.departmentName || '-' }} / {{ user.employeeNo || user.id }}</em>
            </span>
          </label>
        </el-checkbox-group>
        <el-empty v-if="filteredUsers.length === 0" description="暂无可选人员" />
      </section>
    </div>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="loading" @click="$emit('confirm', checked)">{{ confirmText || '保存' }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { DepartmentItem, UserItem } from '@/api/meeting'

const props = defineProps<{
  modelValue: boolean
  title: string
  departments: DepartmentItem[]
  users: UserItem[]
  selectedIds: string[]
  disabledIds?: string[]
  defaultDepartment?: string
  hint?: string
  confirmText?: string
  loading?: boolean
}>()
const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'confirm', ids: string[]): void
}>()

const keyword = ref('')
const departmentKeyword = ref('')
const selectedDepartment = ref('')
const checked = ref<string[]>([])

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

watch(() => props.modelValue, (open) => {
  if (open) {
    checked.value = [...props.selectedIds]
    selectedDepartment.value = props.defaultDepartment || ''
    keyword.value = ''
    departmentKeyword.value = ''
  }
})

const filteredDepartments = computed(() => props.departments.filter((item) => !departmentKeyword.value || item.name.includes(departmentKeyword.value)))
const filteredUsers = computed(() => {
  const key = keyword.value.trim()
  return props.users.filter((user) => {
    if (selectedDepartment.value && user.departmentId !== selectedDepartment.value) return false
    if (key && !`${user.username || ''}${user.realName || ''}${user.employeeNo || ''}${user.id}`.includes(key)) return false
    return true
  })
})

function firstChar(value?: string) {
  return value ? value.slice(0, 1) : '-'
}
</script>
