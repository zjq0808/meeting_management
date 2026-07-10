import { createStore } from 'vuex'
import { api } from '@/api/meeting'

export interface UserState {
  id?: string
  userId?: string
  username?: string
  employeeNo?: string
  realName?: string
  role?: string
  departmentId?: string
  departmentName?: string
  mobile?: string
}

export interface RootState {
  token: string
  user: UserState | null
}

export default createStore<RootState>({
  state: {
    token: localStorage.getItem('token') || '',
    user: JSON.parse(localStorage.getItem('user') || 'null')
  },
  mutations: {
    setSession(state, payload: { token: string; user: UserState }) {
      state.token = payload.token
      state.user = payload.user
      localStorage.setItem('token', payload.token)
      localStorage.setItem('user', JSON.stringify(payload.user))
    },
    clearSession(state) {
      state.token = ''
      state.user = null
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    }
  },
  actions: {
    async login({ commit }, payload: { userId?: string; username?: string; password?: string }) {
      const session = await api.login(payload) as unknown as { token: string; user: UserState }
      commit('setSession', session)
      return session
    }
  },
  getters: {
    isAdmin: (state) => state.user?.role === 'ADMIN',
    isMobileRole: (state) => ['SECRETARY', 'LEADER', 'PARTICIPANT'].includes(state.user?.role || '')
  }
})
