import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'

const routes = [
  {
    path: '/',
    redirect: '/home'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', public: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { title: '注册', public: true }
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('@/views/user/Home.vue'),
    meta: { title: '首页', public: true }
  },
  {
    path: '/movie/:id',
    name: 'MovieDetail',
    component: () => import('@/views/user/MovieDetail.vue'),
    meta: { title: '电影详情', public: true }
  },
  {
    path: '/booking/:scheduleId',
    name: 'SeatSelection',
    component: () => import('@/views/user/SeatSelection.vue'),
    meta: { title: '选座', requiresAuth: true, role: 'ROLE_USER' }
  },
  {
    path: '/my-orders',
    name: 'MyOrders',
    component: () => import('@/views/user/MyOrders.vue'),
    meta: { title: '我的订单', requiresAuth: true, role: 'ROLE_USER' }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/user/Profile.vue'),
    meta: { title: '个人中心', requiresAuth: true, role: 'ROLE_USER' }
  },
  // Admin routes
  {
    path: '/admin/dashboard',
    name: 'AdminDashboard',
    component: () => import('@/views/admin/Dashboard.vue'),
    meta: { title: '管理后台', requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/movies',
    name: 'MovieManage',
    component: () => import('@/views/admin/MovieManage.vue'),
    meta: { title: '影片管理', requiresAuth: true, requiresAdmin: true, role: 'ROLE_SUPER_ADMIN' }
  },
  {
    path: '/admin/halls',
    name: 'HallManage',
    component: () => import('@/views/admin/HallManage.vue'),
    meta: { title: '影厅管理', requiresAuth: true, requiresAdmin: true, role: 'ROLE_SUPER_ADMIN' }
  },
  {
    path: '/admin/schedules',
    name: 'ScheduleManage',
    component: () => import('@/views/admin/ScheduleManage.vue'),
    meta: { title: '排片管理', requiresAuth: true, requiresAdmin: true, role: 'ROLE_SUPER_ADMIN' }
  },
  {
    path: '/admin/orders',
    name: 'AdminOrderManage',
    component: () => import('@/views/admin/OrderManage.vue'),
    meta: { title: '订单管理', requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/statistics',
    name: 'Statistics',
    component: () => import('@/views/admin/Statistics.vue'),
    meta: { title: '数据统计', requiresAuth: true, requiresAdmin: true, role: 'ROLE_SUPER_ADMIN' }
  },
  {
    path: '/admin/employees',
    name: 'EmployeeManage',
    component: () => import('@/views/admin/EmployeeManage.vue'),
    meta: { title: '员工管理', requiresAuth: true, role: 'ROLE_SUPER_ADMIN' }
  },
  {
    path: '/admin/settings',
    name: 'SystemSettings',
    component: () => import('@/views/admin/SystemSettings.vue'),
    meta: { title: '系统设置', requiresAuth: true, role: 'ROLE_SUPER_ADMIN' }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    redirect: '/home'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

// Navigation guard
router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - TTMS` : 'TTMS - 电影院综合管理系统'

  const authStore = useAuthStore()

  // Public routes
  if (to.meta.public) {
    return next()
  }

  // Auth required check
  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return next({ name: 'Login', query: { redirect: to.fullPath } })
  }

  // Admin routes check - must run before specific role check
  // Routes with requiresAdmin:true allow both SUPER_ADMIN and STAFF
  if (to.meta.requiresAdmin && !authStore.isAdmin) {
    ElMessage.error('无权访问管理后台')
    return next({ name: 'Home' })
  }

  // Super admin only check - only for routes without requiresAdmin flag
  // (employee management, system settings)
  if (to.meta.role === 'ROLE_SUPER_ADMIN' && !to.meta.requiresAdmin) {
    if (authStore.user?.roleCode !== 'ROLE_SUPER_ADMIN') {
      ElMessage.error('仅超级管理员可访问此页面')
      return next({ name: 'AdminDashboard' })
    }
  }

  // ROLE_USER routes - any authenticated user can access
  if (to.meta.role === 'ROLE_USER' && !authStore.isLoggedIn) {
    return next({ name: 'Login' })
  }

  next()
})

export default router
