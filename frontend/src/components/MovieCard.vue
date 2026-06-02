<template>
  <div class="movie-card" @click="$router.push(`/movie/${movie.id}`)">
    <div class="poster-wrapper">
      <img
        :src="movie.poster || movie.posterUrl || defaultPoster"
        :alt="movie.name"
        class="poster-img"
        @error="onImgError"
      />
      <div class="poster-overlay">
        <el-button type="primary" size="small" round>查看详情</el-button>
      </div>
      <span v-if="movie.isHot" class="hot-badge">热映</span>
      <span v-if="movie.rating" class="rating-badge">
        <el-icon><StarFilled /></el-icon>
        {{ movie.rating }}
      </span>
    </div>
    <div class="card-info">
      <h3 class="movie-name">{{ movie.name }}</h3>
      <div class="movie-meta">
        <el-tag v-if="movie.genre" size="small" type="info">{{ movie.genre }}</el-tag>
        <span v-if="movie.duration" class="duration">{{ movie.duration }}分钟</span>
      </div>
      <div class="movie-price" v-if="movie.price">
        <span class="price-symbol">$</span>
        <span class="price-value">{{ movie.price }}</span>
        <span class="price-unit">起</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { StarFilled } from '@element-plus/icons-vue'

const props = defineProps({
  movie: {
    type: Object,
    required: true
  }
})

const defaultPoster = ref('data:image/svg+xml,' + encodeURIComponent(`
<svg xmlns="http://www.w3.org/2000/svg" width="300" height="400" viewBox="0 0 300 400">
  <rect fill="#1a1a2e" width="300" height="400"/>
  <text fill="#7a8096" font-family="Arial" font-size="18" text-anchor="middle" x="150" y="200">暂无海报</text>
</svg>
`))

function onImgError(e) {
  e.target.src = defaultPoster.value
}
</script>

<style scoped>
.movie-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: var(--shadow-light);
}

.movie-card:hover {
  transform: translateY(-6px);
  box-shadow: var(--shadow-heavy);
}

.poster-wrapper {
  position: relative;
  width: 100%;
  padding-top: 140%;
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
  transition: transform 0.4s ease;
}

.movie-card:hover .poster-img {
  transform: scale(1.08);
}

.poster-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.movie-card:hover .poster-overlay {
  opacity: 1;
}

.hot-badge {
  position: absolute;
  top: 8px;
  left: 8px;
  background: linear-gradient(135deg, #ff6b35, #f56c6c);
  color: #fff;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 1px;
}

.rating-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  background: rgba(0, 0, 0, 0.7);
  color: #f5a623;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 2px;
}

.card-info {
  padding: 12px;
}

.movie-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.movie-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.duration {
  font-size: 13px;
  color: var(--text-muted);
}

.movie-price {
  color: var(--color-accent);
  font-weight: 700;
}

.price-symbol {
  font-size: 14px;
}

.price-value {
  font-size: 20px;
}

.price-unit {
  font-size: 13px;
  color: var(--text-muted);
  font-weight: 400;
}
</style>
