<template>
  <div class="coupons-page">
    <div class="coupons-container">
      <!-- Header -->
      <header class="page-hero">
        <div>
          <nav class="breadcrumb">Member Center <span class="material-symbols-outlined">chevron_right</span> <span class="active">My Coupons</span></nav>
          <h1 class="page-hero__title">我的优惠券</h1>
          <p class="page-hero__desc">为您精选的观影礼遇与专享折扣。在支付时选择可用券码，开启您的沉浸式艺术之旅。</p>
        </div>
        <div class="filter-pills">
          <button v-for="f in filters" :key="f.value" class="filter-pill"
            :class="{ active: activeFilter === f.value }" @click="activeFilter = f.value">{{ f.label }}</button>
        </div>
      </header>

      <!-- Coupon Grid -->
      <div v-if="loading" class="skeleton-grid">
        <div v-for="i in 4" :key="i" class="skeleton-card">
          <div class="sk-left"></div><div class="sk-right"><div class="sk-line w60"></div><div class="sk-line w40"></div></div>
        </div>
      </div>

      <div v-else-if="displayCoupons.length > 0" class="coupon-grid">
        <div
          v-for="c in displayCoupons" :key="c.id"
          class="coupon-card"
          :class="{
            'coupon-card--expired': isExpired(c),
            'coupon-card--used': isUsed(c),
            'coupon-card--discount': c.type === 'PERCENT'
          }"
        >
          <!-- Left: Value -->
          <div class="coupon-left" :class="c.type === 'PERCENT' ? 'velvet' : 'gold'">
            <div class="coupon-left__glow"></div>
            <span class="coupon-left__tag">{{ typeTag(c) }}</span>
            <template v-if="c.type === 'FIXED' || !c.type">
              <div class="coupon-left__val"><span class="sym">¥</span>{{ formatVal(c) }}</div>
            </template>
            <template v-else>
              <div class="coupon-left__val">{{ percentToFold(c.value) }}<span class="sym">折</span></div>
            </template>
          </div>
          <!-- Perforation -->
          <div class="coupon-perf"></div>
          <!-- Cutouts -->
          <div class="coupon-cut-top"></div>
          <div class="coupon-cut-bot"></div>
          <!-- Right: Info -->
          <div class="coupon-right">
            <div class="coupon-right__top">
              <h3 class="coupon-right__name">{{ c.name || '优惠券' }}</h3>
              <p class="coupon-right__desc">{{ conditionText(c) }}</p>
            </div>
            <div class="coupon-right__bot">
              <div>
                <p class="coupon-right__exp-label">Validity Period</p>
                <p class="coupon-right__exp">{{ expireText(c) }}</p>
              </div>
              <button v-if="canClaim(c)" class="claim-btn" :disabled="claimingId === c.id"
                @click="claimCoupon(c)">{{ claimingId === c.id ? '领取中' : '立即领取' }}</button>
              <span v-else-if="isOwned(c)" class="owned-badge" :class="c.status === 1 ? 'used' : c.status === 2 ? 'exp' : 'ok'">
                {{ ownStatus(c) }}
              </span>
              <span class="material-symbols-outlined arrow-icon">arrow_forward</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Empty -->
      <div v-else class="empty-state">
        <span class="material-symbols-outlined">confirmation_number</span>
        <p>还没有任何优惠券</p>
      </div>

      <!-- Available to claim -->
      <div v-if="availableCoupons.length > 0 && activeFilter === ''" class="claim-section">
        <h2 class="section-title">可领取优惠券</h2>
        <div class="coupon-grid">
          <div v-for="c in availableCoupons" :key="'a'+c.id" class="coupon-card coupon-card--claimable">
            <div class="coupon-left" :class="c.type === 'PERCENT' ? 'velvet' : 'gold'">
              <div class="coupon-left__glow"></div>
              <span class="coupon-left__tag">{{ typeTag(c) }}</span>
              <template v-if="c.type === 'FIXED' || !c.type">
                <div class="coupon-left__val"><span class="sym">¥</span>{{ formatVal(c) }}</div>
              </template>
              <template v-else>
                <div class="coupon-left__val">{{ percentToFold(c.value) }}<span class="sym">折</span></div>
              </template>
            </div>
            <div class="coupon-perf"></div>
            <div class="coupon-cut-top"></div>
            <div class="coupon-cut-bot"></div>
            <div class="coupon-right">
              <div class="coupon-right__top">
                <h3 class="coupon-right__name">{{ c.name || '优惠券' }}</h3>
                <p class="coupon-right__desc">{{ conditionText(c) }} <span class="stock">· 剩余{{ c.remainingQty || 0 }}张</span></p>
              </div>
              <div class="coupon-right__bot">
                <div>
                  <p class="coupon-right__exp-label">Validity</p>
                  <p class="coupon-right__exp">{{ c.expireDays || 30 }}天有效</p>
                </div>
                <button class="claim-btn" :disabled="claimingId === c.id" @click="claimCoupon(c)">
                  {{ claimingId === c.id ? '领取中' : '立即领取' }}
                </button>
                <span class="material-symbols-outlined arrow-icon">arrow_forward</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Promo -->
      <section class="promo-section">
        <h2 class="section-title">还没有心仪的券？</h2>
        <p class="promo-sub">参与影评活动或办理会员卡，解锁更多惊喜礼遇</p>
        <div class="promo-grid">
          <div class="promo-card" @click="$router.push('/profile')">
            <div class="promo-card__img vip-bg"></div>
            <div class="promo-card__overlay"></div>
            <div class="promo-card__content">
              <span class="promo-badge gold">会员专享</span>
              <h4>加入 VIP 会员</h4>
              <p>每月领 2 张 5 折券</p>
            </div>
          </div>
          <div class="promo-card">
            <div class="promo-card__img review-bg"></div>
            <div class="promo-card__overlay"></div>
            <div class="promo-card__content">
              <span class="promo-badge green">限时活动</span>
              <h4>撰写影评赢代金券</h4>
              <p>优质评论可得 ¥20 通用券</p>
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getMyCoupons, getAvailableCoupons, obtainCoupon } from '@/api/coupon'
import { ElMessage } from 'element-plus'

const coupons = ref([])
const availableCoupons = ref([])
const loading = ref(true)
const claimingId = ref(null)
const activeFilter = ref('')

const filters = [
  { label: '全部', value: '' },
  { label: '未使用', value: 0 },
  { label: '已使用', value: 1 },
  { label: '已过期', value: 2 },
]

const displayCoupons = computed(() => {
  if (activeFilter.value === '') return coupons.value
  return coupons.value.filter(c => c.status === Number(activeFilter.value))
})

function typeTag(c) { const t = c.type || 'FIXED'; return t === 'FIXED' ? 'CASH OFF' : 'DISCOUNT' }
function formatVal(c) { return Math.floor(parseFloat(c.value || 0)) }
function percentToFold(v) { const d = (100 - Number(v || 0)) / 10; return d === Math.floor(d) ? Math.floor(d) : d.toFixed(1) }
function conditionText(c) {
  const type = c.type || 'FIXED'
  if (type === 'FIXED') { const min = c.minOrderAmount || 0; return min > 0 ? `满¥${min}可用 · 全线影片适用` : '全场通用 · 无门槛' }
  return '仅限首映场次 · 最高抵扣 ¥30'
}
function expireText(c) { if (c.expireTime) { const t = c.expireTime.substring(0,10); return `${t.replace(/-/g,'.')}` }; return c.expireDays ? `${c.expireDays}天内有效` : '--' }
function isExpired(c) { return c.status === 2 }
function isUsed(c) { return c.status === 1 }
function isOwned(c) { return c.userId != null || c.obtainTime != null }
function canClaim(c) { return !isOwned(c) && (c.remainingQty == null || c.remainingQty > 0) }
function ownStatus(c) { return c.status === 1 ? '已使用' : c.status === 2 ? '已过期' : '可用' }

async function claimCoupon(coupon) {
  claimingId.value = coupon.id
  try { await obtainCoupon(coupon.id); ElMessage.success('领取成功！'); await loadAll() }
  catch (e) { ElMessage.error(e?.response?.data?.message || '领取失败') }
  finally { claimingId.value = null }
}

async function loadAll() {
  loading.value = true
  try {
    const [myRes, availRes] = await Promise.all([getMyCoupons(), getAvailableCoupons()])
    coupons.value = myRes.data || []
    const myIds = new Set(coupons.value.map(c => c.couponId).filter(Boolean))
    availableCoupons.value = (availRes.data || []).filter(c => !myIds.has(c.id) && (c.remainingQty == null || c.remainingQty > 0))
  } catch { coupons.value = []; availableCoupons.value = [] }
  finally { loading.value = false }
}

onMounted(loadAll)
</script>

<style scoped>
.coupons-page { min-height: 100vh; background: var(--bg-primary); }
.coupons-container { max-width: 1200px; margin: 0 auto; padding: 40px 32px; }

/* Header */
.page-hero { display: flex; justify-content: space-between; align-items: flex-start; gap: 20px; margin-bottom: 36px; flex-wrap: wrap; }
.breadcrumb { display: flex; align-items: center; gap: 6px; font-size: 11px; color: var(--text-tertiary); text-transform: uppercase; letter-spacing: 0.05em; margin-bottom: 8px; }
.breadcrumb .active { color: var(--color-primary); }
.breadcrumb .material-symbols-outlined { font-size: 12px; }
.page-hero__title { font-size: 32px; font-weight: 700; color: var(--text-primary); margin-bottom: 6px; }
.page-hero__desc { font-size: 14px; color: var(--text-secondary); max-width: 500px; }

.filter-pills { display: flex; background: var(--bg-card); border-radius: var(--radius-pill); padding: 3px; border: 1px solid var(--border-light); }
.filter-pill { padding: 8px 20px; border: none; border-radius: var(--radius-pill); font-size: 13px; font-weight: 500; color: var(--text-secondary); background: transparent; cursor: pointer; font-family: inherit; transition: all 0.2s; }
.filter-pill:hover { color: var(--text-primary); }
.filter-pill.active { background: var(--color-primary); color: #fff; }
[data-theme='dark'] .filter-pill.active { color: #1A1814; }

/* Coupon Grid */
.coupon-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(340px, 1fr)); gap: 24px; margin-bottom: 48px; }
.section-title { font-family: Georgia,'Noto Serif SC',serif; font-size: 24px; font-weight: 600; color: var(--text-primary); margin-bottom: 24px; }

/* Card */
.coupon-card {
  position: relative; display: flex; height: 160px; border-radius: var(--radius-xl); overflow: hidden;
  background: var(--bg-card); border: 1px solid var(--border-light); cursor: pointer;
  transition: all 0.4s ease;
}
[data-theme='dark'] .coupon-card { background: rgba(20,20,31,0.6); backdrop-filter: blur(12px); border-color: rgba(255,255,255,0.05); }
.coupon-card:hover { transform: translateY(-3px); box-shadow: 0 12px 30px rgba(0,0,0,0.3); }
.coupon-card:hover .arrow-icon { transform: translateX(4px); }

.coupon-card--expired { filter: grayscale(0.6); opacity: 0.55; pointer-events: none; }
.coupon-card--used { opacity: 0.65; }

/* Left */
.coupon-left { width: 35%; display: flex; flex-direction: column; align-items: center; justify-content: center; position: relative; overflow: hidden; flex-shrink: 0; }
.coupon-left.gold { background: linear-gradient(135deg, #e8a850 0%, #a6732e 100%); color: #2a1800; }
.coupon-left.velvet { background: linear-gradient(135deg, #89182a 0%, #40000b 100%); color: #ffc0c1; }
.coupon-left__glow { position: absolute; top: -30px; left: -30px; width: 80px; height: 80px; background: rgba(255,255,255,0.08); border-radius: 50%; filter: blur(20px); }
.coupon-left__tag { font-family: 'JetBrains Mono',monospace; font-size: 10px; font-weight: 700; letter-spacing: 0.08em; opacity: 0.8; margin-bottom: 2px; }
.coupon-left__val { font-size: 44px; font-weight: 800; line-height: 1; display: flex; align-items: baseline; }
.coupon-left__val .sym { font-size: 18px; font-weight: 700; margin-right: 2px; }

/* Perforation */
.coupon-perf { width: 1px; background: repeating-linear-gradient(to bottom, rgba(128,128,128,0.15) 0, rgba(128,128,128,0.15) 6px, transparent 6px, transparent 12px); }
/* Cutouts */
.coupon-cut-top, .coupon-cut-bot { position: absolute; left: 35%; top: -7px; width: 12px; height: 12px; border-radius: 50%; background: var(--bg-primary); z-index: 1; transform: translateX(-50%); }
.coupon-cut-bot { top: auto; bottom: -7px; }

/* Right */
.coupon-right { flex: 1; padding: 18px 20px; display: flex; flex-direction: column; justify-content: space-between; min-width: 0; }
.coupon-right__name { font-size: 16px; font-weight: 600; color: var(--text-primary); margin-bottom: 4px; }
.coupon-right__desc { font-size: 12px; color: var(--text-secondary); line-height: 1.4; }
.coupon-right__exp-label { font-size: 10px; color: var(--text-tertiary); text-transform: uppercase; letter-spacing: 0.05em; margin-bottom: 2px; }
.coupon-right__exp { font-family: 'JetBrains Mono',monospace; font-size: 11px; color: var(--text-secondary); }
.coupon-right__bot { display: flex; align-items: flex-end; justify-content: space-between; gap: 8px; }
.arrow-icon { font-size: 16px; color: var(--color-primary); transition: transform 0.2s; }
.claim-btn { padding: 6px 16px; border: none; border-radius: var(--radius-pill); background: var(--color-primary); color: #fff; font-size: 12px; font-weight: 600; cursor: pointer; font-family: inherit; white-space: nowrap; }
.claim-btn:hover { filter: brightness(1.1); }
.claim-btn:disabled { opacity: 0.6; }
.owned-badge { padding: 4px 12px; border-radius: var(--radius-pill); font-size: 11px; font-weight: 600; }
.owned-badge.ok { background: rgba(232,168,80,0.1); color: var(--color-primary); }
.owned-badge.used { background: rgba(91,141,239,0.1); color: var(--color-info); }
.owned-badge.exp { background: rgba(128,128,128,0.15); color: var(--text-tertiary); }

.stock { color: var(--color-danger); }

/* Skeleton */
.skeleton-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(340px, 1fr)); gap: 24px; }
.skeleton-card { display: flex; height: 160px; border-radius: var(--radius-xl); background: var(--bg-card); overflow: hidden; }
.sk-left { width: 35%; background: var(--bg-hover); }
.sk-right { flex: 1; padding: 24px; display: flex; flex-direction: column; gap: 12px; }
.sk-line { height: 14px; background: var(--bg-hover); border-radius: 4px; }
.sk-line.w60 { width: 60%; }
.sk-line.w40 { width: 40%; }

/* Empty */
.empty-state { text-align: center; padding: 80px 24px; color: var(--text-tertiary); }
.empty-state .material-symbols-outlined { font-size: 56px; margin-bottom: 12px; display: block; }

/* Promo */
.promo-section { margin-top: 64px; padding-top: 48px; border-top: 1px solid var(--border-light); }
.promo-sub { font-size: 14px; color: var(--text-secondary); margin-bottom: 24px; }
.promo-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
.promo-card { position: relative; height: 220px; border-radius: var(--radius-xl); overflow: hidden; cursor: pointer; }
.promo-card__img { position: absolute; inset: 0; background-size: cover; background-position: center; transition: transform 0.5s; }
.promo-card:hover .promo-card__img { transform: scale(1.1); }
.vip-bg { background: linear-gradient(135deg, #1a1a2e, #2c3e50); }
.review-bg { background: linear-gradient(135deg, #1a2e1a, #2e4a3e); }
.promo-card__overlay { position: absolute; inset: 0; background: linear-gradient(to top, rgba(10,10,16,0.9) 0%, rgba(10,10,16,0.2) 100%); }
.promo-card__content { position: absolute; bottom: 24px; left: 24px; right: 24px; z-index: 1; }
.promo-badge { display: inline-block; padding: 3px 10px; border-radius: var(--radius-sm); font-size: 11px; font-weight: 700; margin-bottom: 8px; }
.promo-badge.gold { background: var(--color-primary); color: #2a1800; }
.promo-badge.green { background: var(--color-emerald); color: #fff; }
.promo-card__content h4 { font-size: 22px; font-weight: 700; color: #fff; margin-bottom: 4px; }
.promo-card__content p { font-size: 13px; color: rgba(255,255,255,0.65); }

@media (max-width: 768px) {
  .coupon-grid { grid-template-columns: 1fr; }
  .promo-grid { grid-template-columns: 1fr; }
}
</style>
