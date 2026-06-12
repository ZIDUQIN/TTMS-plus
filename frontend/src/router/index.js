import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'

const routes = [
  {
    path: '/',
    name: 'Landing',
    component: () => import('@/views/user/Landing.vue'),
    meta: { title: 'TTMS - 智能影院管理系统', public: true }
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
    meta: { title: '影片管理', requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/halls',
    name: 'HallManage',
    component: () => import('@/views/admin/HallManage.vue'),
    meta: { title: '影厅管理', requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/schedules',
    name: 'ScheduleManage',
    component: () => import('@/views/admin/ScheduleManage.vue'),
    meta: { title: '排片管理', requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/orders',
    name: 'AdminOrderManage',
    component: () => import('@/views/admin/OrderManage.vue'),
    meta: { title: '订单管理', requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/box-office',
    name: 'BoxOffice',
    component: () => import('@/views/admin/BoxOffice.vue'),
    meta: { title: '电影票房', requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/statistics',
    name: 'Statistics',
    component: () => import('@/views/admin/Statistics.vue'),
    meta: { title: '数据统计', requiresAuth: true, requiresAdmin: true }
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
  // === 新功能页面 ===
  {
    path: '/admin/pos',
    name: 'Pos',
    component: () => import('@/views/admin/PosView.vue'),
    meta: { title: '柜台售票', requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/members',
    name: 'MemberManage',
    component: () => import('@/views/admin/MemberManage.vue'),
    meta: { title: '会员管理', requiresAuth: true, requiresAdmin: true, role: 'ROLE_SUPER_ADMIN' }
  },
  {
    path: '/admin/coupons-manage',
    name: 'CouponManage',
    component: () => import('@/views/admin/CouponManage.vue'),
    meta: { title: '优惠券管理', requiresAuth: true, requiresAdmin: true, role: 'ROLE_SUPER_ADMIN' }
  },
  {
    path: '/admin/shifts',
    name: 'ShiftManage',
    component: () => import('@/views/admin/ShiftManage.vue'),
    meta: { title: '交接班', requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/snacks',
    name: 'SnackManage',
    component: () => import('@/views/admin/SnackManage.vue'),
    meta: { title: '卖品管理', requiresAuth: true, requiresAdmin: true, role: 'ROLE_SUPER_ADMIN' }
  },
  {
    path: '/admin/reports',
    name: 'ReportView',
    component: () => import('@/views/admin/ReportView.vue'),
    meta: { title: '报表', requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/ticket/:id',
    name: 'TicketDetail',
    component: () => import('@/views/user/TicketDetail.vue'),
    meta: { title: '电子票', requiresAuth: true, role: 'ROLE_USER' }
  },
  {
    path: '/my-coupons',
    name: 'MyCoupons',
    component: () => import('@/views/user/MyCoupons.vue'),
    meta: { title: '我的优惠券', requiresAuth: true, role: 'ROLE_USER' }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue'),
    meta: { title: '404', public: true }
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

  // 记住最后访问的页面（非登录页），刷新后可恢复
  if (to.name && to.name !== 'Login' && to.name !== 'Register') {
    sessionStorage.setItem('ttms-last-route', to.fullPath)
  }

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

  // Super admin only check - applies regardless of requiresAdmin flag
  // Ensures only ROLE_SUPER_ADMIN can access employee management and system settings
  if (to.meta.role === 'ROLE_SUPER_ADMIN') {
    if (authStore.user?.roleCode !== 'ROLE_SUPER_ADMIN') {
      ElMessage.error('仅超级管理员可访问此页面')
      // Redirect to appropriate page based on user role to avoid redirect chain
      if (authStore.isAdmin) {
        return next({ name: 'AdminDashboard' })
      }
      return next({ name: 'Home' })
    }
  }

  // ROLE_USER routes - any authenticated user can access
  if (to.meta.role === 'ROLE_USER' && !authStore.isLoggedIn) {
    return next({ name: 'Login' })
  }

  next()
})

export default router
