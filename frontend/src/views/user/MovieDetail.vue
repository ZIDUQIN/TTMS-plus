<template>
  <div class="movie-detail-page">
    <NavBar />

    <div class="detail-container" v-loading="loading">
      <template v-if="movie">
        <!-- Backdrop + Info -->
        <div class="detail-hero">
          <div class="detail-hero-bg" :style="{ backgroundImage: `url(${movie.poster || ''})` }"></div>
          <div class="detail-hero-inner">
            <div class="poster-col">
              <img
                :src="movie.poster || movie.posterUrl || defaultPoster"
                :alt="movie.name"
                class="detail-poster"
                @error="onImgError"
              />
            </div>
            <div class="info-col">
              <h1 class="movie-title">{{ movie.name }}</h1>
              <div class="movie-tags">
                <el-tag v-if="movie.genre" type="danger" size="small">{{ movie.genre }}</el-tag>
                <el-tag v-if="movie.duration" size="small">{{ movie.duration }}分钟</el-tag>
                <el-tag v-if="movie.language" size="small">{{ movie.language }}</el-tag>
                <el-tag v-if="movie.country" size="small">{{ movie.country }}</el-tag>
              </div>
              <div v-if="movie.rating" class="rating-display">
                <el-rate v-model="movie.rating" disabled show-score text-color="#f5a623" />
              </div>
              <div class="info-list">
                <div class="info-item" v-if="movie.director">
                  <span class="info-label">导演：</span>
                  <span>{{ movie.director }}</span>
                </div>
                <div class="info-item" v-if="movie.actors">
                  <span class="info-label">演员：</span>
                  <span>{{ movie.actors }}</span>
                </div>
                <div class="info-item" v-if="movie.releaseDate">
                  <span class="info-label">上映日期：</span>
                  <span>{{ movie.releaseDate }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">票价：</span>
                  <span class="price-text">${{ movie.price || '--' }}</span>
                </div>
              </div>
              <div v-if="movie.description" class="description">
                <p>{{ movie.description }}</p>
              </div>
            </div>
          </div>
        </div>

        <!-- Schedules -->
        <div class="schedules-section">
          <h2 class="section-title">
            <el-icon :size="20" color="var(--color-primary)"><Clock /></el-icon>
            近期场次
          </h2>

          <div v-if="scheduleLoading" style="padding: 24px;">
            <el-skeleton :rows="3" animated />
          </div>

          <el-empty v-else-if="schedules.length === 0" description="暂无排片信息" :image-size="120" />

          <div v-else class="schedule-list">
            <div
              v-for="sch in schedules"
              :key="sch.id"
              class="schedule-card"
              :class="{ disabled: (sch.availableSeats || sch.availableCount) <= 0 }"
            >
              <div class="sch-date">
                <div class="sch-day">{{ formatDay(sch.startTime) }}</div>
                <div class="sch-weekday">{{ formatWeekday(sch.startTime) }}</div>
              </div>
              <div class="sch-info">
                <div class="sch-time">{{ formatTime(sch.startTime) }} - {{ formatTime(sch.endTime) }}</div>
                <div class="sch-meta">
                  <el-tag size="small" type="info">{{ sch.hallName || sch.hall?.name || '--' }}</el-tag>
                  <span>余座：<b :class="{ 'seats-low': (sch.availableSeats || sch.availableCount) < 20 }">
                    {{ sch.availableSeats ?? sch.availableCount ?? '--' }}
                  </b></span>
                </div>
              </div>
              <div class="sch-price">
                <span class="price">{{ sch.price || movie.price }}</span>
                <span class="price-unit">元</span>
              </div>
              <div class="sch-action">
                <el-button
                  type="primary"
                  :disabled="(sch.availableSeats || sch.availableCount) <= 0"
                  @click="goBooking(sch.id)"
                >
                  {{ (sch.availableSeats || sch.availableCount) <= 0 ? '售罄' : '选座购票' }}
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </template>

      <el-empty v-else-if="!loading" description="电影不存在" :image-size="200" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getMovieDetail } from '@/api/movie'
import { getSchedulesByMovie } from '@/api/order'
import { ElMessage } from 'element-plus'
import { Clock } from '@element-plus/icons-vue'
import NavBar from '@/components/NavBar.vue'

const route = useRoute()
const router = useRouter()

const movie = ref(null)
const schedules = ref([])
const loading = ref(true)
const scheduleLoading = ref(true)

const defaultPoster = 'data:image/svg+xml,' + encodeURIComponent(`
<svg xmlns="http://www.w3.org/2000/svg" width="300" height="400" viewBox="0 0 300 400">
  <rect fill="#1a1a2e" width="300" height="400"/>
  <text fill="#7a8096" font-family="Arial" font-size="18" text-anchor="middle" x="150" y="200">暂无海报</text>
</svg>
`)

function onImgError(e) {
  e.target.src = defaultPoster
}

function formatDay(dateStr) {
  if (!dateStr) return '--'
  const d = new Date(dateStr)
  return `${d.getMonth() + 1}月${d.getDate()}日`
}

function formatWeekday(dateStr) {
  if (!dateStr) return '--'
  const days = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return days[new Date(dateStr).getDay()]
}

function formatTime(dateStr) {
  if (!dateStr) return '--'
  const d = new Date(dateStr)
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function goBooking(scheduleId) {
  const token = localStorage.getItem('token')
  if (!token) {
    ElMessage.warning('请先登录后再选座购票')
    router.push('/login')
    return
  }
  router.push(`/booking/${scheduleId}`)
}

async function fetchData() {
  const movieId = route.params.id
  if (!movieId) return

  loading.value = true
  scheduleLoading.value = true

  try {
    const res = await getMovieDetail(movieId)
    movie.value = res.data
  } catch (err) {
    movie.value = null
  } finally {
    loading.value = false
  }

  try {
    const res = await getSchedulesByMovie(movieId)
    schedules.value = res.data || []
  } catch (err) {
    schedules.value = []
  } finally {
    scheduleLoading.value = false
  }
}

onMounted(fetchData)
</script>

<style scoped>
.movie-detail-page {
  min-height: 100vh;
  background: var(--bg-secondary);
}

.detail-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px 48px;
}

/* Hero section */
.detail-hero {
  position: relative;
  margin: 0 -24px;
  padding: 40px 24px;
  overflow: hidden;
}

.detail-hero-bg {
  position: absolute;
  inset: 0;
  background-size: cover;
  background-position: center;
  filter: blur(60px) brightness(0.3);
  transform: scale(1.2);
}

.detail-hero-inner {
  position: relative;
  z-index: 1;
  display: flex;
  gap: 32px;
  max-width: 1200px;
  margin: 0 auto;
}

.poster-col {
  flex-shrink: 0;
}

.detail-poster {
  width: 240px;
  height: 340px;
  object-fit: cover;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-heavy);
}

.info-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 0;
}

.movie-title {
  font-size: 28px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 12px;
}

.movie-tags {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.rating-display {
  margin-bottom: 16px;
}

.info-list {
  margin-bottom: 16px;
}

.info-item {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.9);
  margin-bottom: 6px;
  line-height: 1.6;
}

.info-label {
  color: rgba(255, 255, 255, 0.75);
}

.price-text {
  color: var(--color-accent-light);
  font-weight: 700;
  font-size: 18px;
}

.description {
  font-size: 15px;
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.88);
}

/* Schedules */
.schedules-section {
  margin-top: 32px;
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 24px;
  box-shadow: var(--shadow-light);
}

.section-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
}

.schedule-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.schedule-card {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 16px 20px;
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-light);
  transition: all 0.2s;
}

.schedule-card:hover {
  border-color: var(--color-primary);
  box-shadow: var(--shadow-light);
}

.schedule-card.disabled {
  opacity: 0.6;
}

.sch-date {
  text-align: center;
  min-width: 60px;
  flex-shrink: 0;
}

.sch-day {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
}

.sch-weekday {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 2px;
}

.sch-info {
  flex: 1;
  min-width: 0;
}

.sch-time {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.sch-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  color: var(--text-muted);
}

.seats-low {
  color: var(--color-danger);
}

.sch-price {
  text-align: right;
  flex-shrink: 0;
}

.sch-price .price {
  font-size: 24px;
  font-weight: 700;
  color: var(--color-accent);
}

.sch-price .price-unit {
  font-size: 13px;
  color: var(--text-muted);
}

.sch-action {
  flex-shrink: 0;
}

@media (max-width: 768px) {
  .detail-hero-inner {
    flex-direction: column;
    align-items: center;
  }
  .detail-poster {
    width: 180px;
    height: 260px;
  }
  .schedule-card {
    flex-wrap: wrap;
    gap: 12px;
  }
}
</style>
