<template>
  <div class="coupon-page">
    <div class="coupon-container">
      <!-- Header -->
      <header class="page-hero">
        <div class="page-hero__left">
          <nav class="breadcrumb">
            <span>系统</span>
            <span class="material-symbols-outlined">chevron_right</span>
            <span>营销中心</span>
            <span class="material-symbols-outlined">chevron_right</span>
            <span class="breadcrumb--active">优惠券管理</span>
          </nav>
          <h2 class="page-hero__title">优惠券库 <i class="page-hero__sub">Coupons</i></h2>
        </div>
        <button class="golden-btn" @click="openAdd">
          <span class="material-symbols-outlined">add</span>
          <span>创建新优惠券</span>
        </button>
      </header>

      <!-- Filters -->
      <section class="filter-bar">
        <div class="filter-pills">
          <button
            v-for="f in statusFilters"
            :key="f.value"
            class="filter-pill"
            :class="{ active: filterStatus === f.value }"
            @click="filterStatus = f.value"
          >
            {{ f.label }}
          </button>
        </div>
        <div class="filter-actions">
          <div class="search-box">
            <span class="material-symbols-outlined search-box__icon">search</span>
            <input v-model="searchQuery" type="text" placeholder="搜索券名称" class="search-box__input" />
          </div>
        </div>
      </section>

      <!-- Coupon Grid -->
      <div v-if="loading" class="loading-grid">
        <div v-for="i in 4" :key="i" class="skeleton-card">
          <div class="skeleton-body"><div class="sk-line w-60"></div><div class="sk-line w-40"></div></div>
        </div>
      </div>

      <div v-else class="coupon-grid">
        <!-- Coupon Cards -->
        <div
          v-for="coupon in filteredCoupons"
          :key="coupon.id"
          class="coupon-card"
          :class="{ 'coupon-card--expired': isExpired(coupon) }"
        >
          <div class="coupon-card__inner">
            <!-- Left: Info -->
            <div class="coupon-left">
              <div class="coupon-left__top">
                <div class="coupon-type-row">
                  <span class="coupon-type-badge" :class="typeBadgeClass(coupon)">
                    {{ typeLabel(coupon) }}
                  </span>
                  <span class="coupon-id">ID: {{ coupon.id }}</span>
                </div>
                <h3 class="coupon-name">{{ coupon.name }}</h3>
                <p class="coupon-desc" v-if="coupon.minOrderAmount > 0">
                  满 ¥{{ coupon.minOrderAmount }} 可用
                </p>
                <p class="coupon-desc" v-else>无门槛使用</p>
              </div>
              <div class="coupon-left__bottom">
                <span class="material-symbols-outlined">schedule</span>
                <span v-if="isExpired(coupon)" class="coupon-expiry expired-text">已过期</span>
                <span v-else>有效期 {{ coupon.expireDays || 30 }} 天</span>
              </div>
            </div>

            <!-- Dashed divider -->
            <div class="coupon-divider"></div>

            <!-- Right: Value -->
            <div class="coupon-right" :class="valueBgClass(coupon)">
              <span class="material-symbols-outlined coupon-right__icon">
                {{ coupon.type === 'FIXED' ? 'verified' : 'star' }}
              </span>
              <div class="coupon-right__value">
                <template v-if="coupon.type === 'FIXED' || !coupon.type">
                  <span class="coupon-right__symbol">¥</span>
                  <span class="coupon-right__num">{{ coupon.value }}</span>
                </template>
                <template v-else>
                  <span class="coupon-right__num">{{ percentToFold(coupon.value) }}</span>
                  <span class="coupon-right__symbol">折</span>
                </template>
              </div>
              <span class="coupon-right__tag">{{ coupon.type === 'FIXED' ? '立减' : '折扣券' }}</span>
            </div>
          </div>

          <!-- Footer -->
          <div class="coupon-footer">
            <span class="coupon-footer__stock">
              已领取 <strong>{{ coupon.remainingQty ?? coupon.totalQty }}</strong> / {{ coupon.totalQty || '∞' }}
            </span>
            <div class="coupon-footer__actions">
              <button class="coupon-footer__btn" @click="openEdit(coupon)">编辑</button>
              <button class="coupon-footer__btn coupon-footer__btn--danger" @click="handleDelete(coupon.id)">停用</button>
            </div>
          </div>
        </div>

        <!-- Create New Placeholder -->
        <div class="coupon-card coupon-card--new" @click="openAdd">
          <div class="coupon-card--new__inner">
            <div class="new-icon-wrap">
              <span class="material-symbols-outlined">add_circle</span>
            </div>
            <span class="new-title">添加更多优惠</span>
            <p class="new-desc">点击通过模板快速创建营销活动</p>
          </div>
        </div>
      </div>

      <!-- Footer -->
      <footer class="page-footer">
        <span>共 {{ coupons.length }} 项优惠券</span>
      </footer>
    </div>

    <!-- Create/Edit Modal -->
    <Teleport to="body">
      <div class="modal-overlay" :class="{ show: showDialog }" @click.self="showDialog = false">
        <div class="modal-panel" :class="{ show: showDialog }">
          <div class="modal-header">
            <h3 class="modal-title">{{ editingId ? '编辑优惠券' : '创建新优惠券' }}</h3>
            <button class="modal-close" @click="showDialog = false">
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>
          <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="modal-form">
            <el-form-item label="优惠券名称" prop="name">
              <el-input v-model="form.name" placeholder="如：新片首映立减券" />
            </el-form-item>
            <div class="form-row-2">
              <el-form-item label="类型" prop="type">
                <el-select v-model="form.type" style="width:100%">
                  <el-option label="固定金额 (立减)" value="FIXED" />
                  <el-option label="百分比折扣" value="PERCENT" />
                </el-select>
              </el-form-item>
              <el-form-item :label="form.type === 'FIXED' ? '金额 (¥)' : '优惠力度 (%)'" prop="value">
                <el-input-number v-model="form.value" :min="1" :max="form.type === 'FIXED' ? 1000 : 90" style="width:100%" />
                <div v-if="form.type === 'PERCENT'" style="font-size:11px;color:var(--text-tertiary);margin-top:4px">
                  优惠 {{ form.value }}%（即打{{ percentToChinese(form.value) }}折）
                </div>
              </el-form-item>
            </div>
            <div class="form-row-2">
              <el-form-item label="最低消费 (¥)">
                <el-input-number v-model="form.minOrderAmount" :min="0" :step="10" style="width:100%" />
              </el-form-item>
              <el-form-item label="有效期 (天)">
                <el-input-number v-model="form.expireDays" :min="1" :max="365" style="width:100%" />
              </el-form-item>
            </div>
            <el-form-item label="发行数量" prop="totalQty">
              <el-input-number v-model="form.totalQty" :min="1" :max="10000" style="width:100%" />
            </el-form-item>
          </el-form>
          <div class="modal-actions">
            <button class="btn-cancel" @click="showDialog = false">取消</button>
            <button class="golden-btn" :disabled="submitting" @click="handleSubmit">
              {{ editingId ? '保存修改' : '确认创建' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { getCouponList, createCoupon, updateCoupon, deleteCoupon } from '@/api/coupon'
import { ElMessage } from 'element-plus'

const coupons = ref([]); const loading = ref(false); const showDialog = ref(false)
const submitting = ref(false); const editingId = ref(null); const formRef = ref(null)
const filterStatus = ref(''); const searchQuery = ref('')

const form = reactive({ name: '', type: 'FIXED', value: 10, minOrderAmount: 0, totalQty: 100, expireDays: 30 })
const rules = { name: [{ required: true, message: '请输入名称' }], value: [{ required: true, message: '请输入面值' }] }

const statusFilters = [
  { label: '全部', value: '' },
  { label: '进行中', value: 'active' },
  { label: '已过期', value: 'expired' },
]

const filteredCoupons = computed(() => {
  let list = coupons.value
  if (searchQuery.value) {
    const q = searchQuery.value.toLowerCase()
    list = list.filter(c => (c.name || '').toLowerCase().includes(q))
  }
  // Simple filter — in real app would check dates
  if (filterStatus.value === 'expired') list = list.filter(c => c.status === 0)
  if (filterStatus.value === 'active') list = list.filter(c => c.status !== 0)
  return list
})

function isExpired(c) { return c.status === 0 }
function typeLabel(c) { return c.type === 'FIXED' ? '立减' : '折扣' }
function percentToFold(v) { const d = (100 - v) / 10; return d === Math.floor(d) ? Math.floor(d) : d.toFixed(1) }
function percentToChinese(v) { const d = (100 - v) / 10; return d === Math.floor(d) ? String(Math.floor(d)) : d.toFixed(1) }
function typeBadgeClass(c) { return c.type === 'FIXED' ? 'badge-fixed' : 'badge-discount' }
function valueBgClass(c) { return c.type === 'FIXED' ? 'bg-fixed' : 'bg-discount' }

async function fetchData() {
  loading.value = true
  try { const res = await getCouponList(); coupons.value = res?.data || res || [] }
  catch (e) { console.error('获取优惠券列表失败', e); ElMessage.error('获取优惠券列表失败') }
  finally { loading.value = false }
}

function openAdd() { editingId.value = null; Object.assign(form, { name: '', type: 'FIXED', value: 10, minOrderAmount: 0, totalQty: 100, expireDays: 30 }); showDialog.value = true }

function openEdit(coupon) {
  editingId.value = coupon.id
  Object.assign(form, {
    name: coupon.name, type: coupon.type || 'FIXED', value: coupon.value,
    minOrderAmount: coupon.minOrderAmount || 0, totalQty: coupon.totalQty || 100, expireDays: coupon.expireDays || 30
  })
  showDialog.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  const v = await formRef.value.validate().catch(() => false); if (!v) return
  submitting.value = true
  try {
    const payload = { ...form }
    // 新创建的优惠券，剩余数量 = 总发行量
    if (!editingId.value) payload.remainingQty = form.totalQty
    if (editingId.value) { await updateCoupon({ id: editingId.value, ...payload }); ElMessage.success('已更新') }
    else { await createCoupon(payload); ElMessage.success('创建成功') }
    showDialog.value = false
    await fetchData()
  } catch (e) {
    console.error('保存优惠券失败', e)
  } finally { submitting.value = false }
}

async function handleDelete(id) {
  try { await deleteCoupon(id); ElMessage.success('已删除'); await fetchData() }
  catch (e) { console.error('删除失败', e) }
}
onMounted(fetchData)
</script>

<style scoped>
/* ============================================================
   Coupon Management — Coupon Card Grid Edition
   ============================================================ */
.coupon-page { min-height: 100vh; background: var(--bg-primary); }
.coupon-container { max-width: 1280px; margin: 0 auto; padding: 24px 32px; }

/* ---- Header ---- */
.page-hero { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 28px; flex-wrap: wrap; gap: 16px; }
.breadcrumb { display: flex; align-items: center; gap: 4px; font-size: 11px; color: var(--text-tertiary); margin-bottom: 8px; }
.breadcrumb .material-symbols-outlined { font-size: 11px; }
.breadcrumb--active { color: var(--color-primary); font-weight: 500; }
.page-hero__title { font-family: Georgia, 'Noto Serif SC', serif; font-size: 36px; font-weight: 700; color: var(--text-primary); }
.page-hero__sub { font-weight: 400; color: rgba(132,84,0,0.2); font-style: italic; font-size: 28px; }

.golden-btn {
  display: inline-flex; align-items: center; gap: 8px; padding: 12px 24px; border: none; border-radius: var(--radius-lg);
  font-size: 14px; font-weight: 700; color: #fff; cursor: pointer; transition: all 0.3s ease; font-family: inherit;
  background: var(--color-primary);
}
[data-theme='dark'] .golden-btn { color: #2a1800; background: linear-gradient(135deg, #e8a850, #ffc67c, #e8a850); }
.golden-btn:hover { filter: brightness(1.1); box-shadow: 0 0 20px rgba(132,84,0,0.3); transform: scale(1.02); }
.golden-btn:disabled { opacity: 0.5; cursor: not-allowed; }

/* ---- Filter Bar ---- */
.filter-bar { display: flex; justify-content: space-between; align-items: center; gap: 16px; margin-bottom: 28px; padding-bottom: 20px; border-bottom: 1px solid var(--border-light); flex-wrap: wrap; }
.filter-pills { display: flex; gap: 8px; }
.filter-pill {
  padding: 8px 20px; border-radius: var(--radius-pill); border: 1px solid var(--border-light); background: var(--bg-card);
  color: var(--text-secondary); font-size: 13px; font-weight: 500; cursor: pointer; transition: all 0.2s ease; font-family: inherit;
}
.filter-pill:hover { border-color: var(--color-primary); color: var(--color-primary); }
.filter-pill.active { background: var(--color-primary); color: #fff; border-color: var(--color-primary); font-weight: 600; }
[data-theme='dark'] .filter-pill.active { color: #1A1814; }

.filter-actions { display: flex; align-items: center; gap: 12px; }
.search-box { position: relative; }
.search-box__icon { position: absolute; left: 12px; top: 50%; transform: translateY(-50%); font-size: 16px; color: var(--text-tertiary); }
.search-box__input {
  width: 220px; padding: 9px 14px 9px 36px; background: transparent; border: none; border-bottom: 1px solid var(--border-color);
  color: var(--text-primary); font-size: 13px; font-family: inherit; outline: none; transition: border-color 0.2s ease;
}
.search-box__input:focus { border-bottom-color: var(--color-primary); }
.search-box__input::placeholder { color: var(--text-tertiary); }

/* ---- Coupon Grid ---- */
.coupon-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(360px, 1fr)); gap: 24px; }

/* ---- Coupon Card ---- */
.coupon-card {
  background: var(--bg-card); border-radius: var(--radius-xl); border: 1px solid var(--border-light);
  box-shadow: var(--shadow-light); overflow: hidden; transition: all 0.3s ease; position: relative;
}
.coupon-card:hover { box-shadow: 0 10px 30px rgba(0,0,0,0.06); transform: translateY(-2px); }
[data-theme='dark'] .coupon-card:hover { box-shadow: 0 10px 30px rgba(0,0,0,0.3); }

/* Cutout holes */
.coupon-card::before, .coupon-card::after {
  content: ''; position: absolute; left: 70%; transform: translateX(-50%); width: 18px; height: 18px;
  border-radius: 50%; background: var(--bg-primary); z-index: 2;
}
.coupon-card::before { top: -9px; }
.coupon-card::after { bottom: 52px; }

/* Expired */
.coupon-card--expired { opacity: 0.6; }
.coupon-card--expired .coupon-left, .coupon-card--expired .coupon-right { filter: grayscale(0.5); }

.coupon-card__inner { display: flex; height: 150px; }

/* ---- Left Section ---- */
.coupon-left { width: 70%; padding: 18px 20px; display: flex; flex-direction: column; justify-content: space-between; }
.coupon-type-row { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.coupon-type-badge {
  padding: 2px 8px; border-radius: var(--radius-pill); font-size: 10px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.04em;
}
.badge-fixed { background: rgba(132,84,0,0.1); color: var(--color-primary); }
.badge-discount { background: rgba(170,50,64,0.1); color: #aa3240; }

.coupon-id { font-size: 11px; color: var(--text-tertiary); font-style: italic; }
.coupon-name { font-family: Georgia, 'Noto Serif SC', serif; font-size: 20px; font-weight: 600; color: var(--text-primary); margin-bottom: 4px; }
.coupon-desc { font-size: 13px; color: var(--text-secondary); }
.coupon-left__bottom { display: flex; align-items: center; gap: 4px; font-size: 11px; color: var(--text-tertiary); }
.coupon-left__bottom .material-symbols-outlined { font-size: 12px; }
.expired-text { color: var(--color-danger); }

/* ---- Dashed Divider ---- */
.coupon-divider {
  position: absolute; left: calc(70% - 1px); top: 18px; bottom: 18px; width: 0;
  border-left: 2px dashed var(--border-color);
}

/* ---- Right Section ---- */
.coupon-right {
  width: 30%; display: flex; flex-direction: column; align-items: center; justify-content: center; position: relative;
}
.bg-fixed { background: rgba(132,84,0,0.06); }
.bg-discount { background: rgba(170,50,64,0.05); }

.coupon-right__icon { position: absolute; top: 10px; right: 10px; font-size: 18px; opacity: 0.3; font-variation-settings: 'FILL' 1; }
.coupon-right__value { text-align: center; }
.coupon-right__symbol { font-size: 14px; font-weight: 700; opacity: 0.7; color: var(--color-primary); }
.coupon-right__num { font-family: Georgia, 'Noto Serif SC', serif; font-size: 44px; font-weight: 700; color: var(--text-primary); line-height: 1; }
.bg-discount .coupon-right__num, .bg-discount .coupon-right__symbol { color: #aa3240; }
.coupon-right__tag { font-size: 10px; font-weight: 700; opacity: 0.5; text-transform: uppercase; letter-spacing: 0.1em; margin-top: 6px; }

/* ---- Footer ---- */
.coupon-footer {
  display: flex; justify-content: space-between; align-items: center; padding: 12px 20px;
  background: var(--bg-secondary); border-top: 1px solid var(--border-light);
}
.coupon-footer__stock { font-size: 12px; color: var(--text-secondary); }
.coupon-footer__stock strong { color: var(--color-primary); font-weight: 700; }
.coupon-footer__actions { display: flex; gap: 8px; }
.coupon-footer__btn { padding: 4px 12px; border: none; border-radius: var(--radius-sm); background: transparent; color: var(--color-primary); font-size: 12px; font-weight: 500; cursor: pointer; font-family: inherit; }
.coupon-footer__btn:hover { text-decoration: underline; }
.coupon-footer__btn--danger { color: var(--text-secondary); }
.coupon-footer__btn--danger:hover { color: var(--color-danger); }

/* ---- Create New Placeholder ---- */
.coupon-card--new { border: 2px dashed var(--border-color); cursor: pointer; }
.coupon-card--new:hover { border-color: var(--color-primary); }
.coupon-card--new::before, .coupon-card--new::after { display: none; }
.coupon-card--new__inner { display: flex; flex-direction: column; align-items: center; justify-content: center; height: 190px; text-align: center; padding: 24px; }
.new-icon-wrap { width: 56px; height: 56px; border-radius: 50%; background: var(--bg-secondary); display: flex; align-items: center; justify-content: center; margin-bottom: 12px; transition: all 0.2s ease; }
.coupon-card--new:hover .new-icon-wrap { transform: scale(1.1); }
.new-icon-wrap .material-symbols-outlined { font-size: 28px; color: var(--text-secondary); transition: color 0.2s; }
.coupon-card--new:hover .new-icon-wrap .material-symbols-outlined { color: var(--color-primary); }
.new-title { font-family: Georgia, 'Noto Serif SC', serif; font-size: 18px; font-weight: 600; color: var(--text-secondary); margin-bottom: 4px; }
.new-desc { font-size: 12px; color: var(--text-tertiary); }

/* ---- Footer ---- */
.page-footer { margin-top: 40px; padding-top: 24px; border-top: 1px solid var(--border-light); font-size: 12px; color: var(--text-tertiary); }

/* ---- Skeleton ---- */
.loading-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(360px, 1fr)); gap: 24px; }
.skeleton-card { background: var(--bg-card); border-radius: var(--radius-xl); padding: 24px; border: 1px solid var(--border-light); }
.skeleton-body { display: flex; flex-direction: column; gap: 12px; }
.sk-line { height: 16px; border-radius: 4px; background: var(--bg-hover); }
.sk-line.w-60 { width: 60%; }
.sk-line.w-40 { width: 40%; }

/* ============================================================
   MODAL
   ============================================================ */
.modal-overlay { position: fixed; inset: 0; z-index: 1000; display: flex; align-items: center; justify-content: center; padding: 24px; background: rgba(0,0,0,0.5); opacity: 0; pointer-events: none; transition: opacity 0.3s ease; }
.modal-overlay.show { opacity: 1; pointer-events: auto; }
.modal-panel { width: 100%; max-width: 520px; max-height: 90vh; overflow-y: auto; background: var(--bg-card); border-radius: var(--radius-xl); border: 1px solid var(--border-light); box-shadow: var(--shadow-heavy); transform: scale(0.95) translateY(12px); transition: transform 0.3s ease; }
.modal-panel.show { transform: scale(1) translateY(0); }
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 24px 24px 0; }
.modal-title { font-size: 20px; font-weight: 700; color: var(--text-primary); }
.modal-close { width: 34px; height: 34px; display: flex; align-items: center; justify-content: center; border: none; border-radius: 50%; background: transparent; color: var(--text-secondary); cursor: pointer; }
.modal-close:hover { background: var(--bg-hover); color: var(--text-primary); }
.modal-form { padding: 20px 24px 0; }
.form-row-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 0 16px; }
.modal-actions { display: flex; gap: 12px; padding: 20px 24px 24px; }
.modal-actions .golden-btn { flex: 1; justify-content: center; padding: 14px; }
.btn-cancel { flex: 1; padding: 14px; border: 1px solid var(--border-color); border-radius: var(--radius-lg); background: transparent; color: var(--text-primary); font-size: 14px; font-weight: 600; cursor: pointer; font-family: inherit; }
.btn-cancel:hover { background: var(--bg-hover); }

@media (max-width: 768px) {
  .coupon-grid { grid-template-columns: 1fr; }
  .coupon-card__inner { height: auto; flex-direction: column; }
  .coupon-left { width: 100%; }
  .coupon-right { width: 100%; height: 80px; }
  .coupon-divider { display: none; }
  .coupon-card::before, .coupon-card::after { display: none; }
  .form-row-2 { grid-template-columns: 1fr; }
}
</style>
