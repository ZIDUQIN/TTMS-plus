<template>
  <div class="home-page">
    <NavBar />

    <!-- ===== 电影轮播 Hero Carousel ===== -->
    <section class="carousel" v-if="hotMovies.length > 0">
      <div class="carousel-track" :style="{ transform: `translateX(-${currentIndex * 100}%)` }">
        <div
          v-for="movie in hotMovies"
          :key="movie.id"
          class="carousel-slide"
          @click="$router.push(`/movie/${movie.id}`)"
        >
          <img
            class="carousel-bg"
            :src="movie.poster || movie.posterUrl"
            :alt="movie.name || movie.movieName"
            @error="(e) => e.target.style.display='none'"
          />
          <div class="carousel-gradient"></div>
          <div class="carousel-content">
            <span class="carousel-badge">热映推荐</span>
            <h1 class="carousel-title">{{ movie.name || movie.movieName }}</h1>
            <div class="carousel-meta">
              <span class="carousel-rating" v-if="movie.rating">
                <el-icon :size="16"><StarFilled /></el-icon>
                {{ movie.rating }}
              </span>
              <span class="carousel-genre" v-if="movie.genre">{{ movie.genre }}</span>
              <span class="carousel-duration" v-if="movie.duration">{{ movie.duration }}分钟</span>
            </div>
            <p class="carousel-desc" v-if="movie.description">{{ truncateText(movie.description, 120) }}</p>
            <el-button type="primary" size="large" round class="carousel-btn" @click.stop="$router.push(`/movie/${movie.id}`)">
              立即购票
            </el-button>
          </div>
        </div>
      </div>

      <!-- 箭头 -->
      <button class="carousel-arrow carousel-prev" @click.stop="prevSlide">
        <el-icon :size="24"><ArrowLeft /></el-icon>
      </button>
      <button class="carousel-arrow carousel-next" @click.stop="nextSlide">
        <el-icon :size="24"><ArrowRight /></el-icon>
      </button>

      <!-- 指示点 -->
      <div class="carousel-dots">
        <span
          v-for="(movie, idx) in hotMovies"
          :key="movie.id"
          class="carousel-dot"
          :class="{ active: idx === currentIndex }"
          @click.stop="goToSlide(idx)"
        ></span>
      </div>

      <!-- 搜索栏 -->
      <div class="carousel-search">
        <el-input
          v-model="searchKeyword"
          size="large"
          placeholder="搜索您想看的电影..."
          :prefix-icon="Search"
          class="search-input"
          @keyup.enter="handleSearch"
        >
          <template #append>
            <el-button :icon="Search" @click="handleSearch">搜索</el-button>
          </template>
        </el-input>
      </div>
    </section>

    <!-- 无热门影片时的默认Hero -->
    <section class="hero-banner" v-else>
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

    <!-- ===== 正在热映 ===== -->
    <div class="main-content">
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

      <!-- Loading -->
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

      <!-- Empty -->
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
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getMovieList, searchMovies, getHotMovies } from '@/api/movie'
import { Search, VideoCameraFilled, StarFilled, ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import NavBar from '@/components/NavBar.vue'
import MovieCard from '@/components/MovieCard.vue'

const route = useRoute()
const router = useRouter()

const movies = ref([])
const hotMovies = ref([])
const loading = ref(true)
const searchKeyword = ref('')
const sortBy = ref('default')
const currentIndex = ref(0)
let autoTimer = null

const filteredMovies = computed(() => {
  let list = [...movies.value]
  if (sortBy.value === 'rating') {
    list.sort((a, b) => (b.rating || 0) - (a.rating || 0))
  } else if (sortBy.value === 'price') {
    list.sort((a, b) => (a.price || 0) - (b.price || 0))
  }
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

function truncateText(text, max) {
  if (!text) return ''
  return text.length > max ? text.slice(0, max) + '...' : text
}

function nextSlide() {
  if (hotMovies.value.length === 0) return
  currentIndex.value = (currentIndex.value + 1) % hotMovies.value.length
}

function prevSlide() {
  if (hotMovies.value.length === 0) return
  currentIndex.value = (currentIndex.value - 1 + hotMovies.value.length) % hotMovies.value.length
}

function goToSlide(idx) {
  currentIndex.value = idx
  resetAutoPlay()
}

function startAutoPlay() {
  stopAutoPlay()
  autoTimer = setInterval(nextSlide, 5000)
}

function stopAutoPlay() {
  if (autoTimer) { clearInterval(autoTimer); autoTimer = null }
}

function resetAutoPlay() {
  stopAutoPlay()
  startAutoPlay()
}

async function fetchHotMovies() {
  try {
    const res = await getHotMovies()
    hotMovies.value = res.data || []
    if (hotMovies.value.length > 0) {
      startAutoPlay()
    }
  } catch (err) {
    hotMovies.value = []
  }
}

async function fetchMovies(keyword) {
  loading.value = true
  try {
    if (keyword) {
      const res = await searchMovies(keyword)
      movies.value = res.data || []
    } else {
      const res = await getMovieList({ page: 1, size: 100 })
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
  fetchHotMovies()
  if (route.query.keyword) {
    searchKeyword.value = route.query.keyword
    fetchMovies(route.query.keyword)
  } else {
    fetchMovies()
  }
})

onUnmounted(() => {
  stopAutoPlay()
})
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  background: var(--bg-secondary);
}

/* ===== 电影轮播 Carousel ===== */
.carousel {
  position: relative;
  width: 100%;
  height: 520px;
  overflow: hidden;
}

.carousel-track {
  display: flex;
  height: 100%;
  transition: transform 0.6s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}

.carousel-slide {
  position: relative;
  min-width: 100%;
  height: 100%;
  cursor: pointer;
}

.carousel-bg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center 20%;
}

.carousel-gradient {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    to right,
    rgba(0, 0, 0, 0.75) 0%,
    rgba(0, 0, 0, 0.4) 50%,
    rgba(0, 0, 0, 0.2) 100%
  );
  background: linear-gradient(
    to top,
    rgba(0, 0, 0, 0.85) 0%,
    rgba(0, 0, 0, 0.2) 50%,
    rgba(0, 0, 0, 0.5) 100%
  );
}

.carousel-content {
  position: absolute;
  bottom: 80px;
  left: 80px;
  max-width: 560px;
  z-index: 2;
  animation: fadeInUp 0.6s ease;
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(20px); }
  to   { opacity: 1; transform: translateY(0); }
}

.carousel-badge {
  display: inline-block;
  background: var(--color-accent, #ff6b35);
  color: #fff;
  padding: 4px 14px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 1px;
  margin-bottom: 12px;
}

.carousel-title {
  font-size: 44px;
  font-weight: 800;
  color: #fff;
  margin-bottom: 12px;
  text-shadow: 0 2px 12px rgba(0,0,0,0.5);
  line-height: 1.25;
}

.carousel-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
}

.carousel-rating {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #f5a623;
  font-size: 16px;
  font-weight: 700;
}

.carousel-genre,
.carousel-duration {
  color: rgba(255,255,255,0.8);
  font-size: 14px;
}

.carousel-desc {
  color: rgba(255,255,255,0.7);
  font-size: 14px;
  line-height: 1.6;
  margin-bottom: 20px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.carousel-btn {
  font-size: 16px;
  padding: 12px 36px;
}

/* 箭头 */
.carousel-arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 48px;
  height: 48px;
  border-radius: 50%;
  border: none;
  background: rgba(255,255,255,0.12);
  backdrop-filter: blur(6px);
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 3;
  transition: background 0.3s;
}

.carousel-arrow:hover {
  background: rgba(255,255,255,0.25);
}

.carousel-prev { left: 20px; }
.carousel-next { right: 20px; }

/* 指示点 */
.carousel-dots {
  position: absolute;
  bottom: 28px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 10px;
  z-index: 3;
}

.carousel-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: rgba(255,255,255,0.4);
  cursor: pointer;
  transition: all 0.3s;
}

.carousel-dot.active {
  background: #fff;
  width: 28px;
  border-radius: 5px;
}

/* 搜索栏覆盖 */
.carousel-search {
  position: absolute;
  top: 24px;
  right: 24px;
  width: 300px;
  z-index: 3;
}

.search-input :deep(.el-input__wrapper) {
  background: rgba(255,255,255,0.12) !important;
  border: 1px solid rgba(255,255,255,0.2) !important;
  border-radius: 24px !important;
  box-shadow: none !important;
}

.search-input :deep(.el-input__inner) {
  color: #fff !important;
}

.search-input :deep(.el-input__inner::placeholder) {
  color: rgba(255,255,255,0.55) !important;
}

.search-input :deep(.el-input-group__append) {
  background: var(--color-primary);
  border: none;
  border-radius: 0 24px 24px 0;
  color: #fff;
}

/* ===== 默认Hero（无热门影片时） ===== */
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
    radial-gradient(circle at 20% 50%, rgba(233,69,96,0.15) 0%, transparent 50%),
    radial-gradient(circle at 80% 50%, rgba(64,158,255,0.1) 0%, transparent 50%);
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
  color: rgba(255,255,255,0.75);
  margin-bottom: 36px;
}

.hero-search {
  max-width: 560px;
  margin: 0 auto;
}

.hero-search-input :deep(.el-input__wrapper) {
  background: rgba(255,255,255,0.12) !important;
  border: 1px solid rgba(255,255,255,0.2) !important;
  border-radius: 24px !important;
  box-shadow: none !important;
  padding: 4px 16px;
}

.hero-search-input :deep(.el-input__inner) {
  color: #fff !important;
}

.hero-search-input :deep(.el-input__inner::placeholder) {
  color: rgba(255,255,255,0.6) !important;
}

.hero-search-input :deep(.el-input-group__append) {
  background: var(--color-primary);
  border: none;
  border-radius: 0 24px 24px 0;
  color: #fff;
}

/* ===== Main Content ===== */
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

.loading-container { padding: 16px 0; }

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
  .carousel { height: 360px; }
  .carousel-content { left: 24px; right: 24px; bottom: 60px; max-width: none; }
  .carousel-title { font-size: 28px; }
  .carousel-search { display: none; }
  .carousel-arrow { display: none; }
  .hero-banner { height: 260px; }
  .hero-title { font-size: 28px; }
  .section-header { flex-direction: column; align-items: flex-start; }
}
</style>
