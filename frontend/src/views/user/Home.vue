<template>
  <div class="home-page">
    <NavBar />

    <!-- Hero Banner -->
    <section class="hero-banner">
      <div class="hero-content">
        <h1 class="hero-title">光影世界，由此开启</h1>
        <p class="hero-subtitle">最新大片、经典佳作，尽在TTMS</p>
        <div class="hero-search">
          <el-input
            v-model="searchKeyword"
            size="large"
            placeholder="搜索您想看的电影..."
            :prefix-icon="Search"
            class="hero-search-input"
            @keyup.enter="handleSearch"
          >
            <template #append>
              <el-button :icon="Search" @click="handleSearch">搜索</el-button>
            </template>
          </el-input>
        </div>
      </div>
      <div class="hero-overlay"></div>
    </section>

    <div class="main-content">
      <!-- Section header -->
      <div class="section-header">
        <div class="section-title-group">
          <h2 class="section-title">
            <el-icon :size="24" color="var(--color-primary)"><VideoCameraFilled /></el-icon>
            正在热映
          </h2>
          <span class="section-count">共 {{ filteredMovies.length }} 部影片</span>
        </div>
        <div class="section-actions">
          <el-radio-group v-model="sortBy" size="small">
            <el-radio-button value="default">默认</el-radio-button>
            <el-radio-button value="rating">评分最高</el-radio-button>
            <el-radio-button value="price">价格最低</el-radio-button>
          </el-radio-group>
        </div>
      </div>

      <!-- Loading state -->
      <div v-if="loading" class="loading-container">
        <el-skeleton :rows="3" animated />
        <div class="skeleton-grid">
          <el-skeleton v-for="i in 8" :key="i" animated>
            <template #template>
              <el-skeleton-item variant="image" style="height: 240px" />
              <el-skeleton-item variant="text" style="margin-top: 8px" />
              <el-skeleton-item variant="text" style="width: 60%" />
            </template>
          </el-skeleton>
        </div>
      </div>

      <!-- Empty state -->
      <el-empty
        v-else-if="filteredMovies.length === 0"
        description="暂无影片数据"
        :image-size="200"
      />

      <!-- Movie grid -->
      <div v-else class="movie-grid">
        <MovieCard
          v-for="movie in filteredMovies"
          :key="movie.id"
          :movie="movie"
          @click="$router.push(`/movie/${movie.id}`)"
        />
      </div>
    </div>

    <!-- Footer -->
    <footer class="home-footer">
      <p>TTMS Cinema Management System &copy; 2024</p>
    </footer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getMovieList, searchMovies } from '@/api/movie'
import { ElMessage } from 'element-plus'
import { Search, VideoCameraFilled } from '@element-plus/icons-vue'
import NavBar from '@/components/NavBar.vue'
import MovieCard from '@/components/MovieCard.vue'

const route = useRoute()
const router = useRouter()

const movies = ref([])
const loading = ref(true)
const searchKeyword = ref('')
const sortBy = ref('default')

const filteredMovies = computed(() => {
  let list = [...movies.value]

  // Sort
  if (sortBy.value === 'rating') {
    list.sort((a, b) => (b.rating || 0) - (a.rating || 0))
  } else if (sortBy.value === 'price') {
    list.sort((a, b) => (a.price || 0) - (b.price || 0))
  }

  // Filter by search keyword
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase()
    list = list.filter(m =>
      m.name?.toLowerCase().includes(kw) ||
      m.genre?.toLowerCase().includes(kw) ||
      m.director?.toLowerCase().includes(kw)
    )
  }

  return list
})

async function fetchMovies(keyword) {
  loading.value = true
  try {
    if (keyword) {
      const res = await searchMovies(keyword)
      movies.value = res.data || []
    } else {
      const res = await getMovieList()
      movies.value = res.data?.records || res.data || []
    }
  } catch (err) {
    movies.value = []
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  if (searchKeyword.value.trim()) {
    fetchMovies(searchKeyword.value.trim())
  }
}

watch(route, (to) => {
  if (to.query.keyword) {
    searchKeyword.value = to.query.keyword
    fetchMovies(to.query.keyword)
  }
})

onMounted(() => {
  if (route.query.keyword) {
    searchKeyword.value = route.query.keyword
    fetchMovies(route.query.keyword)
  } else {
    fetchMovies()
  }
})
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  background: var(--bg-secondary);
}

/* Hero Banner */
.hero-banner {
  position: relative;
  height: 380px;
  background: linear-gradient(135deg, #1a1a2e, #16213e, #0f3460);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.hero-overlay {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 20% 50%, rgba(233, 69, 96, 0.15) 0%, transparent 50%),
    radial-gradient(circle at 80% 50%, rgba(64, 158, 255, 0.1) 0%, transparent 50%);
  pointer-events: none;
}

.hero-content {
  position: relative;
  z-index: 1;
  text-align: center;
  padding: 0 24px;
}

.hero-title {
  font-size: 42px;
  font-weight: 800;
  color: #fff;
  margin-bottom: 12px;
  letter-spacing: 2px;
}

.hero-subtitle {
  font-size: 18px;
  color: rgba(255, 255, 255, 0.75);
  margin-bottom: 36px;
}

.hero-search {
  max-width: 560px;
  margin: 0 auto;
}

.hero-search-input :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.12) !important;
  border: 1px solid rgba(255, 255, 255, 0.2) !important;
  border-radius: 24px !important;
  box-shadow: none !important;
  padding: 4px 16px;
}

.hero-search-input :deep(.el-input__inner) {
  color: #fff !important;
}

.hero-search-input :deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.6) !important;
}

.hero-search-input :deep(.el-input-group__append) {
  background: var(--color-primary);
  border: none;
  border-radius: 0 24px 24px 0;
  color: #fff;
}

/* Main content */
.main-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 32px 24px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 12px;
}

.section-title-group {
  display: flex;
  align-items: center;
  gap: 12px;
}

.section-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-count {
  font-size: 13px;
  color: var(--text-muted);
}

/* Movie grid */
.movie-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 20px;
}

/* Loading skeleton */
.loading-container {
  padding: 16px 0;
}

.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 20px;
  margin-top: 24px;
}

/* Footer */
.home-footer {
  text-align: center;
  padding: 32px 24px;
  color: var(--text-muted);
  font-size: 13px;
  border-top: 1px solid var(--border-light);
  margin-top: 24px;
}

@media (max-width: 768px) {
  .hero-banner {
    height: 260px;
  }
  .hero-title {
    font-size: 28px;
  }
  .section-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
