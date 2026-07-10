<template>
  <el-container class="admin-shell">
    <el-aside width="224px">
      <div class="brand">三重一大会议</div>
      <el-menu router :default-active="$route.path">
        <el-menu-item index="/meetings">会议列表</el-menu-item>
        <el-menu-item v-if="isAdmin" index="/meetings/new">创建会议</el-menu-item>
        <el-menu-item index="/mobile/meetings">移动端入口</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header>
        <div>
          <strong>{{ user?.realName || user?.username }}</strong>
          <span class="muted">{{ roleLabel(user?.role) }}</span>
        </div>
        <el-button text @click="logout">退出</el-button>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useStore } from 'vuex'

const store = useStore()
const router = useRouter()
const user = computed(() => store.state.user)
const isAdmin = computed(() => user.value?.role === 'ADMIN')

function roleLabel(role?: string) {
  const map: Record<string, string> = {
    ADMIN: '会议管理员',
    SECRETARY: '部门秘书',
    LEADER: '科组长',
    PARTICIPANT: '参会人员'
  }
  return role ? map[role] || role : ''
}

function logout() {
  store.commit('clearSession')
  router.replace('/login')
}
</script>

<style scoped>
.admin-shell {
  min-height: 100vh;
}

.el-aside {
  background: #14213d;
  color: #fff;
}

.brand {
  height: 58px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  font-weight: 700;
  border-bottom: 1px solid rgba(255, 255, 255, 0.12);
}

.el-menu {
  border-right: 0;
  background: transparent;
}

:deep(.el-menu-item) {
  color: rgba(255, 255, 255, 0.82);
}

:deep(.el-menu-item.is-active) {
  color: #fff;
  background: rgba(255, 255, 255, 0.12);
}

.el-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
}

.el-header .muted {
  margin-left: 10px;
}

.el-main {
  padding: 0;
}
</style>
