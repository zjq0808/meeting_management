<template>
  <el-container class="admin-shell">
    <el-aside width="76px" class="icon-rail">
      <div class="rail-logo">会</div>
      <el-tooltip content="会议列表" placement="right">
        <button :class="{ active: $route.path.startsWith('/meetings') }" @click="$router.push('/meetings')">
          <el-icon><Grid /></el-icon>
        </button>
      </el-tooltip>
      <el-tooltip v-if="isAdmin" content="创建会议" placement="right">
        <button @click="$router.push('/meetings/new')">
          <el-icon><Plus /></el-icon>
        </button>
      </el-tooltip>
      <el-tooltip content="移动端入口" placement="right">
        <button @click="$router.push('/mobile/meetings')">
          <el-icon><Iphone /></el-icon>
        </button>
      </el-tooltip>
    </el-aside>
    <el-container>
      <el-header class="topbar">
        <div>
          <strong>技术局三重一大会议</strong>
          <span>{{ roleLabel(user?.role) }}</span>
        </div>
        <div class="topbar-user">
          <span>{{ user?.realName || user?.username }}</span>
          <el-button text @click="logout">退出</el-button>
        </div>
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
import { Grid, Iphone, Plus } from '@element-plus/icons-vue'

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

.icon-rail {
  display: flex;
  align-items: center;
  flex-direction: column;
  gap: 14px;
  padding: 16px 0;
  background: #fff;
  border-right: 1px solid #e6eaf0;
}

.rail-logo,
.icon-rail button {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 12px;
}

.rail-logo {
  color: #fff;
  font-size: 18px;
  font-weight: 900;
  background: #1a73e8;
}

.icon-rail button {
  color: #667085;
  font-size: 22px;
  background: transparent;
  border: 0;
  cursor: pointer;
  transition: background-color 0.16s ease, color 0.16s ease, transform 0.16s ease;
}

.icon-rail button:hover,
.icon-rail button.active {
  color: #1a73e8;
  background: #eff6ff;
  transform: translateY(-1px);
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
  background: rgba(255, 255, 255, 0.92);
  border-bottom: 1px solid #e6eaf0;
}

.topbar strong {
  display: block;
  color: #111827;
  font-size: 16px;
  line-height: 1.4;
}

.topbar span {
  color: #667085;
  font-size: 13px;
  line-height: 1.4;
}

.topbar-user {
  display: flex;
  align-items: center;
  gap: 12px;
}

.el-main {
  padding: 0;
  background: #f4f7fb;
}

@media (max-width: 720px) {
  .icon-rail {
    display: none;
  }
}
</style>
