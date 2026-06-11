<template>
  <div class="home-page">
    <!-- Nav -->
    <nav class="home-nav">
      <div class="home-nav__inner">
        <router-link to="/" class="home-nav__brand">TTMS</router-link>
        <div class="home-nav__links">
          <a href="#now-playing" class="home-nav__link active">正在热映</a>
          <a href="#coming-soon" class="home-nav__link">即将上映</a>
        </div>
        <div class="home-nav__actions">
          <router-link v-if="!authStore.isLoggedIn" to="/login" class="home-nav__link">登录</router-link>
          <router-link v-else to="/profile" class="home-nav__link">{{ authStore.realName || '我的' }}</router-link>
        </div>
      </div>
    </nav>

    <!-- Hero wrapper: 始终占用520px高度，确保SPA导航时布局稳定 -->
    <div class="hero-zone">
      <section v-if="hotMovies.length > 0" class="hero" :key="heroKey">
        <div class="hero__track" :style="{ transform: `translateX(-${heroIdx * 100}%)` }">
          <div v-for="m in hotMovies" :key="m.id" class="hero__slide">
            <img class="hero__bg" :src="m.poster || m.posterUrl" :alt="m.movieName" @error="onHeroImgErr" />
          </div>
        </div>
        <div class="hero__overlay"></div>
        <div class="hero__gradient"></div>
        <div class="hero__content">
          <span class="hero__badge">精选推荐</span>
          <h1 class="hero__title">{{ heroMovie.movieName }}</h1>
          <p class="hero__desc" v-if="heroMovie.description">{{ truncate(heroMovie.description, 100) }}</p>
          <div class="hero__btns">
            <button class="hero-btn hero-btn--book" @click="$router.push(`/movie/${heroMovie.id}`)">
              <span class="material-symbols-outlined">confirmation_number</span> 立即购票
            </button>
          </div>
        </div>
        <button class="hero__arrow hero__arrow--left" @click.stop="prevSlide">
          <span class="material-symbols-outlined">chevron_left</span>
        </button>
        <button class="hero__arrow hero__arrow--right" @click.stop="nextSlide">
          <span class="material-symbols-outlined">chevron_right</span>
        </button>
        <div class="hero__dots">
          <span v-for="(m, i) in hotMovies" :key="i" class="hero__dot" :class="{ active: i === heroIdx }" @click.stop="goToSlide(i)"></span>
        </div>
      </section>
      <!-- 数据未加载时显示占位，避免布局跳变 -->
      <div v-else class="hero-placeholder"></div>
    </div>

    <!-- 正在热映 -->
    <section class="section" id="now-playing">
      <div class="section__header">
        <div>
          <h2 class="section__title">正在热映</h2>
          <p class="section__sub">精选佳作现已登陆各大影厅，立即购票锁定你的专属座位。</p>
        </div>
        <button class="section__link" @click="showAllNowPlaying = !showAllNowPlaying">
          {{ showAllNowPlaying ? '收起' : '查看全部(' + movies.length + '部)' }}
        </button>
      </div>
      <div class="movie-grid" v-if="movies.length > 0">
        <div v-for="m in (showAllNowPlaying ? movies : movies.slice(0, 8))" :key="m.id" class="movie-card" @click="$router.push(`/movie/${m.id}`)">
          <div class="movie-card__poster">
            <img :src="m.poster || m.posterUrl" :alt="m.movieName || m.name" @error="onPosterErr" />
            <div class="movie-card__stroke"></div>
          </div>
          <div class="movie-card__tags">
            <span v-if="m.genre" class="tag">{{ m.genre.split(',')[0] }}</span>
            <span v-if="m.duration" class="tag">{{ m.duration }} 分钟</span>
          </div>
          <h3 class="movie-card__name">{{ m.movieName || m.name }}</h3>
          <p class="movie-card__dir" v-if="m.director">导演: {{ m.director }}</p>
        </div>
      </div>
      <el-skeleton v-else :rows="2" animated />
    </section>

    <!-- 即将上映 -->
    <section class="section" id="coming-soon" v-if="upcomingMovies.length > 0">
      <div class="section__header">
        <div>
          <h2 class="section__title">即将上映</h2>
          <p class="section__sub">即将到来的精彩大片，敬请期待。</p>
        </div>
      </div>
      <div class="coming-grid">
        <div v-for="m in upcomingMovies.slice(0, 3)" :key="m.id" class="coming-card" @click="$router.push(`/movie/${m.id}`)">
          <div class="coming-card__img-wrap">
            <img :src="m.poster || m.posterUrl" :alt="m.movieName" @error="onPosterErr" />
            <div class="coming-card__play"><span class="material-symbols-outlined">play_circle</span></div>
          </div>
          <span class="coming-card__date" v-if="m.releaseDate">{{ formatDate(m.releaseDate) }}</span>
          <h3 class="coming-card__name">{{ m.movieName || m.name }}</h3>
          <p class="coming-card__desc" v-if="m.description">{{ truncate(m.description, 60) }}</p>
        </div>
      </div>
    </section>

    <!-- Newsletter -->
    <section class="section newsletter">
      <div class="newsletter__card">
        <div>
          <h2 class="newsletter__title">影院周刊</h2>
          <p class="newsletter__desc">获取最新影评、影展排期和独家放映邀请，精彩不容错过。</p>
        </div>
        <div class="newsletter__form">
          <input type="email" placeholder="请输入邮箱地址" class="newsletter__input" />
          <button class="hero-btn hero-btn--book">立即订阅</button>
        </div>
      </div>
    </section>

    <!-- Footer -->
    <footer class="home-footer">
      <div class="home-footer__brand">TTMS</div>
      <div class="home-footer__links">
        <a href="#">隐私政策</a><a href="#">服务条款</a><a href="#">联系我们</a>
      </div>
      <p>© 2024 TTMS 智能影院管理系统. 保留所有权利.</p>
    </footer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { getMovieList } from '@/api/movie'

const authStore = useAuthStore()
const movies = ref([])
const hotMovies = ref([])
const upcomingMovies = ref([])
const heroIdx = ref(0)
const heroKey = ref(0)
const heroFallback = ref(false)
const showAllNowPlaying = ref(false)

const heroMovie = computed(() => hotMovies.value[heroIdx.value] || hotMovies.value[0] || movies.value[0] || {})

function truncate(s, n) { return s && s.length > n ? s.slice(0, n) + '...' : (s || '') }
function formatDate(d) { if (!d) return ''; const months = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec']; const t = new Date(d); return `${months[t.getMonth()]} ${t.getDate()}, ${t.getFullYear()}` }
function onHeroImgErr(e) { e.target.style.display = 'none'; heroFallback.value = true }
function onPosterErr(e) { e.target.src = 'data:image/svg+xml,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" width="200" height="300"><rect fill="#e4d8ce" width="200" height="300"/><text fill="#837565" font-size="12" text-anchor="middle" x="100" y="155">暂无海报</text></svg>') }

let heroTimer = null
function nextSlide() { heroIdx.value = (heroIdx.value + 1) % hotMovies.value.length; startHeroTimer() }
function prevSlide() { heroIdx.value = (heroIdx.value - 1 + hotMovies.value.length) % hotMovies.value.length; startHeroTimer() }
function goToSlide(i) { heroIdx.value = i; startHeroTimer() }
function startHeroTimer() { stopHeroTimer(); if (hotMovies.value.length > 1) { heroTimer = setInterval(() => { heroIdx.value = (heroIdx.value + 1) % hotMovies.value.length }, 4000) } }
function stopHeroTimer() { if (heroTimer) { clearInterval(heroTimer); heroTimer = null } }

async function fetchMovies() {
  try {
    const res = await getMovieList({ page: 1, size: 50 })
    const all = res.data?.records || res.data || []
    movies.value = all.filter(m => m.status === 1)
    hotMovies.value = all.filter(m => m.status === 1).slice(0, 5)
    upcomingMovies.value = all.filter(m => m.status === 2).slice(0, 3)
    // If no hot movies found, use all in-theater movies
    if (hotMovies.value.length === 0 && movies.value.length > 0) {
      hotMovies.value = movies.value.slice(0, 5)
    }
    // 数据就绪后，等 DOM 完成布局再 force re-render hero 确保尺寸正确
    if (hotMovies.value.length > 0) {
      await nextTick()
      // 两次 RAF 确保浏览器完成 layout + paint
      requestAnimationFrame(() => {
        requestAnimationFrame(() => {
          // 递增 key 触发 hero 强制重建，此时容器宽度已确定
          heroKey.value++
          if (hotMovies.value.length > 1) startHeroTimer()
        })
      })
    }
  } catch { /* silent */ }
}

onMounted(() => {
  fetchMovies()
})
onUnmounted(stopHeroTimer)
</script>

<style scoped>
.home-page { min-height: 100vh; background: var(--bg-primary); }

/* Nav */
.home-nav { position: fixed; top: 0; left: 0; right: 0; z-index: 50; background: rgba(255,255,255,0.7); backdrop-filter: blur(20px); border-bottom: 1px solid rgba(0,0,0,0.06); }
[data-theme='dark'] .home-nav { background: rgba(20,20,35,0.7); backdrop-filter: blur(20px); border-bottom-color: rgba(255,255,255,0.05); }
.home-nav__inner { max-width: 1280px; margin: 0 auto; padding: 0 32px; height: 56px; display: flex; align-items: center; justify-content: space-between; }
.home-nav__brand { font-family: Georgia,'Noto Serif SC',serif; font-size: 22px; font-weight: 700; color: var(--color-primary); }
.home-nav__links { display: flex; gap: 24px; }
.home-nav__link { font-size: 13px; font-weight: 500; color: var(--text-secondary); padding-bottom: 2px; border-bottom: 2px solid transparent; transition: all 0.2s; }
.home-nav__link:hover, .home-nav__link.active { color: var(--color-primary); border-bottom-color: var(--color-primary); }
.home-nav__actions { display: flex; gap: 12px; align-items: center; }

/* Hero Zone — 始终占位，消除 SPA 导航时的布局跳变 */
.hero-zone {
  min-height: 520px;
}
.hero-placeholder {
  height: 520px;
  background: var(--bg-secondary);
}

/* Hero */
.hero { position: relative; width: 100%; height: 520px; overflow: hidden; margin-top: 0; }
.hero__track { display: flex; height: 100%; width: 100%; transition: transform 0.7s cubic-bezier(0.25, 0.1, 0.25, 1); }
.hero__slide { min-width: 100%; width: 100%; height: 100%; position: relative; }
.hero__bg { width: 100%; height: 100%; object-fit: cover; image-rendering: auto; filter: contrast(1.12) saturate(1.25) brightness(0.92); }
.hero__overlay { position: absolute; inset: 0; background: rgba(0,0,0,0.15); z-index: 1; pointer-events: none; }
.hero__gradient { position: absolute; inset: 0; background: linear-gradient(to top, var(--bg-primary) 0%, transparent 20%); z-index: 1; pointer-events: none; }
.hero__content { position: absolute; bottom: 60px; left: 48px; max-width: 640px; z-index: 2; text-shadow: 0 2px 12px rgba(0,0,0,0.5); }
.hero__badge { display: inline-block; padding: 4px 14px; background: var(--color-primary); color: #fff; border-radius: var(--radius-pill); font-size: 11px; font-weight: 600; margin-bottom: 12px; }
.hero__title { font-family: Georgia,'Noto Serif SC',serif; font-size: 56px; font-weight: 800; color: #fff; line-height: 1.1; margin-bottom: 10px; letter-spacing: -0.02em; }
.hero__desc { font-size: 17px; color: rgba(255,255,255,0.85); margin-bottom: 20px; max-width: 480px; font-style: italic; }
.hero__btns { display: flex; gap: 12px; }
.hero-btn { padding: 12px 28px; border-radius: var(--radius-lg); font-size: 13px; font-weight: 600; cursor: pointer; font-family: inherit; display: flex; align-items: center; gap: 6px; border: none; }
.hero-btn--book { background: var(--color-primary); color: #fff; }
.hero-btn--book:hover { filter: brightness(1.1); }
.hero-btn .material-symbols-outlined { font-size: 18px; }
.hero__arrow { position: absolute; top: 50%; transform: translateY(-50%); z-index: 3; width: 44px; height: 44px; border-radius: 50%; border: 1px solid rgba(255,255,255,0.2); background: rgba(0,0,0,0.3); color: #fff; display: flex; align-items: center; justify-content: center; cursor: pointer; transition: all 0.2s; backdrop-filter: blur(4px); }
.hero__arrow:hover { background: rgba(0,0,0,0.5); border-color: rgba(255,255,255,0.4); }
.hero__arrow--left { left: 20px; }
.hero__arrow--right { right: 20px; }
.hero__dots { position: absolute; bottom: 28px; right: 48px; display: flex; gap: 8px; z-index: 3; }
.hero__dot { width: 8px; height: 8px; border-radius: 50%; background: rgba(255,255,255,0.3); cursor: pointer; transition: all 0.2s; }
.hero__dot.active { background: var(--color-primary); width: 24px; border-radius: 4px; }

/* Section */
.section { max-width: 1280px; margin: 0 auto 64px; padding: 0 32px; }
.section__header { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 32px; padding-bottom: 16px; border-bottom: 1px solid var(--border-light); }
.section__title { font-family: Georgia,'Noto Serif SC',serif; font-size: 28px; font-weight: 600; color: var(--text-primary); margin-bottom: 4px; }
.section__sub { font-size: 14px; color: var(--text-secondary); }
.section__link { background: none; border: none; font-size: 13px; font-weight: 600; color: var(--color-primary); cursor: pointer; font-family: inherit; }
.section__link:hover { text-decoration: underline; }

.movie-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 24px; }
.movie-card { cursor: pointer; }
.movie-card__poster { aspect-ratio: 2/3; border-radius: var(--radius-lg); overflow: hidden; background: var(--bg-card); box-shadow: var(--shadow-light); position: relative; margin-bottom: 12px; }
.movie-card__poster img { width: 100%; height: 100%; object-fit: cover; transition: transform 0.5s; }
.movie-card:hover .movie-card__poster img { transform: scale(1.05); }
.movie-card__stroke { position: absolute; inset: 0; box-shadow: inset 0 0 0 1px rgba(0,0,0,0.05); pointer-events: none; border-radius: var(--radius-lg); }
.movie-card__tags { display: flex; gap: 6px; margin-bottom: 6px; }
.tag { padding: 2px 8px; background: var(--bg-secondary); color: var(--text-secondary); border-radius: var(--radius-pill); font-size: 10px; font-weight: 500; }
.movie-card__name { font-family: Georgia,'Noto Serif SC',serif; font-size: 18px; font-weight: 600; color: var(--text-primary); }
.movie-card__dir { font-size: 11px; color: var(--text-tertiary); margin-top: 2px; }

.coming-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 32px; }
.coming-card { cursor: pointer; }
.coming-card__img-wrap { position: relative; aspect-ratio: 16/9; border-radius: var(--radius-lg); overflow: hidden; background: var(--bg-card); box-shadow: var(--shadow-light); margin-bottom: 16px; }
.coming-card__img-wrap img { width: 100%; height: 100%; object-fit: cover; transition: transform 0.5s; }
.coming-card:hover .coming-card__img-wrap img { transform: scale(1.1); }
.coming-card__play { position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; background: rgba(0,0,0,0.15); opacity: 0; transition: opacity 0.3s; }
.coming-card:hover .coming-card__play { opacity: 1; }
.coming-card__play .material-symbols-outlined { font-size: 48px; color: #fff; }
.coming-card__date { font-size: 12px; color: var(--color-primary); font-weight: 600; letter-spacing: 0.08em; text-transform: uppercase; }
.coming-card__name { font-family: Georgia,'Noto Serif SC',serif; font-size: 20px; font-weight: 600; color: var(--text-primary); margin: 4px 0; }
.coming-card__desc { font-size: 13px; color: var(--text-secondary); }

.newsletter__card { background: var(--bg-card); border: 1px solid var(--border-light); border-radius: var(--radius-xl); padding: 48px 56px; display: flex; align-items: center; gap: 40px; box-shadow: var(--shadow-light); flex-wrap: wrap; }
.newsletter__title { font-family: Georgia,'Noto Serif SC',serif; font-size: 32px; font-weight: 700; color: var(--text-primary); margin-bottom: 8px; }
.newsletter__desc { font-size: 15px; color: var(--text-secondary); max-width: 420px; }
.newsletter__form { display: flex; gap: 12px; flex: 1; justify-content: flex-end; }
.newsletter__input { padding: 12px 20px; background: var(--bg-secondary); border: none; border-bottom: 1px solid var(--border-color); font-size: 14px; color: var(--text-primary); outline: none; width: 260px; font-family: inherit; }
.newsletter__input:focus { border-bottom-color: var(--color-primary); }

.home-footer { text-align: center; padding: 48px 32px; border-top: 1px solid var(--border-light); margin-top: 32px; }
.home-footer__brand { font-family: Georgia,'Noto Serif SC',serif; font-size: 40px; font-weight: 700; color: var(--color-primary); margin-bottom: 16px; }
.home-footer__links { display: flex; justify-content: center; gap: 24px; margin-bottom: 12px; font-size: 12px; }
.home-footer__links a { color: var(--text-secondary); }
.home-footer__links a:hover { color: var(--color-primary); }
.home-footer p { font-size: 12px; color: var(--text-tertiary); }

@media (max-width: 768px) {
  .hero-zone { min-height: 400px; }
  .hero-placeholder { height: 400px; }
  .hero { height: 400px; }
  .hero__content { left: 24px; bottom: 40px; }
  .hero__title { font-size: 32px; }
  .movie-grid { grid-template-columns: repeat(2, 1fr); }
  .coming-grid { grid-template-columns: 1fr; }
  .newsletter__card { padding: 32px; flex-direction: column; }
  .newsletter__form { flex-direction: column; width: 100%; }
  .newsletter__input { width: 100%; }
}
</style>
