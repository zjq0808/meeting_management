import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import store from '@/store'
import LoginView from '@/views/LoginModern.vue'
import AdminShell from '@/views/admin/AdminShellModern.vue'
import MeetingList from '@/views/admin/MeetingListModern.vue'
import MeetingEditor from '@/views/admin/MeetingEditorModern.vue'
import MeetingDashboard from '@/views/admin/MeetingConsoleModern.vue'
import MeetingDetail from '@/views/admin/MeetingDetailModern.vue'
import MeetingConsole from '@/views/admin/MeetingConsoleModern.vue'
import SigninDashboard from '@/views/admin/SigninDashboardModern.vue'
import MobileMeetingList from '@/views/mobile/MobileMeetingListModern.vue'
import MobileMeetingDetail from '@/views/mobile/MobileMeetingDetailModern.vue'
import MobileMeetingConsole from '@/views/mobile/MobileMeetingConsoleModern.vue'
import SelectionView from '@/views/mobile/SelectionView.vue'
import SignInView from '@/views/mobile/SignInView.vue'
import ProgressView from '@/views/mobile/ProgressViewModern.vue'

const routes: Array<RouteRecordRaw> = [
  { path: '/login', name: 'login', component: LoginView },
  {
    path: '/',
    component: AdminShell,
    meta: { requiresAuth: true },
    children: [
      { path: '', redirect: '/meetings' },
      { path: 'meetings', name: 'meetings', component: MeetingList },
      { path: 'meetings/new', name: 'meeting-new', component: MeetingEditor },
      { path: 'meetings/:id/edit', name: 'meeting-edit', component: MeetingEditor, props: true },
      { path: 'meetings/:id/detail', name: 'meeting-detail', component: MeetingDetail, props: true },
      { path: 'meetings/:id/console', name: 'meeting-console', component: MeetingConsole, props: true },
      { path: 'meetings/:id/signin-dashboard', name: 'signin-dashboard', component: SigninDashboard, props: true },
      { path: 'meetings/:id/dashboard', name: 'meeting-dashboard', component: MeetingDashboard, props: true }
    ]
  },
  { path: '/mobile/meetings', name: 'mobile-meetings', component: MobileMeetingList, meta: { requiresAuth: true } },
  { path: '/mobile/meetings/:id', name: 'mobile-meeting-detail', component: MobileMeetingDetail, meta: { requiresAuth: true }, props: true },
  { path: '/mobile/meetings/:id/console', name: 'mobile-meeting-console', component: MobileMeetingConsole, meta: { requiresAuth: true }, props: true },
  { path: '/mobile/selection/:meetingId', name: 'mobile-selection', component: SelectionView, meta: { requiresAuth: true }, props: true },
  { path: '/mobile/sign-in', name: 'mobile-sign-in', component: SignInView, meta: { requiresAuth: true } },
  { path: '/mobile/progress/:meetingId', name: 'mobile-progress', component: ProgressView, meta: { requiresAuth: true }, props: true }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  if (to.meta.requiresAuth && !store.state.token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
