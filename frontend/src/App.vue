<template>
  <!-- Admin Layout: Sidebar + Header + Content Area -->
  <template v-if="isAdminRoute">
    <AdminSidebar />
    <AdminHeader
      :show-search="showAdminSearch"
      :search-placeholder="adminSearchPlaceholder"
      @search="onAdminSearch"
    />
    <div class="admin-main">
      <router-view />
    </div>
  </template>

  <!-- Public Layout: NavBar + Content -->
  <template v-else>
    <div class="public-layout">
      <NavBar v-if="!isMinimalRoute" />
      <main :class="{ 'minimal-content': isMinimalRoute }">
        <router-view />
      </main>
      <footer v-if="showFooter" class="cinema-footer">
        <div class="footer-inner">
          <span class="footer-logo font-display">TTMS</span>
          <p class="footer-text">智能影院综合管理系统 &copy; {{ currentYear }}</p>
        </div>
      </footer>
    </div>
  </template>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import NavBar from '@/components/NavBar.vue'
import AdminSidebar from '@/components/AdminSidebar.vue'
import AdminHeader from '@/components/AdminHeader.vue'

const route = useRoute()

const isAdminRoute = computed(() => route.path.startsWith('/admin'))
const isMinimalRoute = computed(() => {
  const name = route.name
  return name === 'Login' || name === 'Register' || name === 'NotFound' || name === 'Landing'
})
const showFooter = computed(() => isMinimalRoute.value === false)

const showAdminSearch = computed(() => {
  const name = route.name
  return ['MovieManage', 'OrderManage', 'MemberManage'].includes(name)
})
const adminSearchPlaceholder = computed(() => {
  const name = route.name
  if (name === 'MovieManage') return '搜索影片名称、导演、主演...'
  if (name === 'OrderManage') return '搜索订单号、用户名...'
  if (name === 'MemberManage') return '搜索会员...'
  return '搜索...'
})

function onAdminSearch(keyword) {
  // Search handling per page
}
const currentYear = new Date().getFullYear()
</script>

<style scoped>
/* Admin: Sidebar offset only — pages handle own padding */
.admin-main {
  margin-left: var(--sidebar-width);
  padding-top: var(--header-height);
  min-height: 100vh;
  background: var(--bg-primary);
}

/* Public Layout */
.public-layout {
  min-height: 100vh;
  background: var(--bg-primary);
  display: flex;
  flex-direction: column;
}
.public-layout main {
  flex: 1;
}
.public-layout main.minimal-content {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
}

/* Cinema Footer */
.cinema-footer {
  margin-top: 80px;
  padding: 32px 24px;
  border-top: 1px solid var(--border-light);
  text-align: center;
}
.footer-inner {
  max-width: 1400px;
  margin: 0 auto;
}
.footer-logo {
  font-size: 20px;
  font-weight: 700;
  background: linear-gradient(135deg, #E8A850, #F0C070);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
.footer-text {
  font-size: 12px;
  color: var(--text-tertiary);
  margin-top: 8px;
}
</style>
