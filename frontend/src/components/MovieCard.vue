<template>
  <div class="movie-card" @click="$router.push(`/movie/${movie.id}`)">
    <div class="poster-wrapper">
      <img
        :src="movie.poster || movie.posterUrl || defaultPoster"
        :alt="movie.name"
        class="poster-img"
        loading="lazy"
        @error="onImgError"
      />
      <div class="poster-overlay">
        <span class="overlay-text">查看详情</span>
      </div>
    </div>
    <div class="card-info">
      <h3 class="movie-name">{{ movie.name }}</h3>
      <div class="movie-meta">
        <span v-if="movie.genre" class="genre-tag">{{ movie.genre }}</span>
        <span v-if="movie.duration" class="duration">{{ movie.duration }}分钟</span>
        <span v-if="movie.rating" class="rating-sm">
          <el-icon :size="14"><StarFilled /></el-icon>
          {{ movie.rating }}
        </span>
      </div>
      <div class="movie-price" v-if="movie.price">
        <span class="price-currency">¥</span>
        <span class="price-value">{{ movie.price }}</span>
        <span class="price-label">起</span>
      </div>
    </div>
    <!-- Hot badge -->
    <span v-if="movie.isHot" class="hot-badge">热映</span>
  </div>
</template>

<script setup>
import { StarFilled } from '@element-plus/icons-vue'

const props = defineProps({
  movie: {
    type: Object,
    required: true
  }
})

const defaultPoster = 'data:image/svg+xml,' + encodeURIComponent(`
<svg xmlns="http://www.w3.org/2000/svg" width="300" height="400" viewBox="0 0 300 400">
  <rect fill="#e8e8ed" width="300" height="400"/>
  <text fill="#aeaeb2" font-family="SF Pro Text,PingFang SC,sans-serif" font-size="15" font-weight="500" text-anchor="middle" x="150" y="200">暂无海报</text>
</svg>
`)

function onImgError(e) {
  e.target.src = defaultPoster
}
</script>

<style scoped>
/* ============================================================
   MovieCard — Apple-style minimal card
   ============================================================ */
.movie-card {
  position: relative;
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  overflow: hidden;
  cursor: pointer;
  box-shadow: var(--shadow-light);
  transition:
    transform 0.22s cubic-bezier(0.25, 0.1, 0.25, 1),
    box-shadow 0.22s cubic-bezier(0.25, 0.1, 0.25, 1);
  /* Subtle border for definition in light mode */
  border: 1px solid transparent;
}

.movie-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-card-hover);
  border-color: var(--border-light);
}

/* ---- Poster ---- */
.poster-wrapper {
  position: relative;
  width: 100%;
  padding-top: 133.33%; /* 3:4 ratio */
  overflow: hidden;
  background: var(--bg-hover);
}

.poster-img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s cubic-bezier(0.25, 0.1, 0.25, 1);
}

.movie-card:hover .poster-img {
  transform: scale(1.05);
}

/* ---- Poster Overlay ---- */
.poster-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
}

.movie-card:hover .poster-overlay {
  opacity: 1;
}

.overlay-text {
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.02em;
  padding: 8px 20px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: var(--radius-pill);
}

/* ---- Badges ---- */
.hot-badge {
  position: absolute;
  top: 10px;
  left: 10px;
  background: var(--color-primary);
  color: #fff;
  padding: 3px 10px;
  border-radius: var(--radius-pill);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.04em;
}

/* ---- Card Info ---- */
.card-info {
  padding: 14px 16px 16px;
}

.movie-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  letter-spacing: -0.01em;
  line-height: 1.3;
}

.movie-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  flex-wrap: wrap;
  font-size: 12px;
  color: var(--text-secondary);
}

.genre-tag {
  font-size: 12px;
  color: var(--text-secondary);
}

.duration {
  font-size: 12px;
  color: var(--text-tertiary);
}

.rating-sm {
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 13px;
  font-weight: 600;
  color: #f5a623;
}

.movie-price {
  color: var(--color-accent);
  font-weight: 700;
  display: flex;
  align-items: baseline;
  gap: 1px;
}

.price-currency {
  font-size: 13px;
  font-weight: 600;
}

.price-value {
  font-size: 20px;
  letter-spacing: -0.02em;
}

.price-label {
  font-size: 12px;
  color: var(--text-tertiary);
  font-weight: 400;
  margin-left: 2px;
}
</style>
