<template>
  <div class="snack-page">
    <!-- Ambient Background -->
    <div class="ambient-bg" aria-hidden="true">
      <div class="ambient-orb ambient-orb--gold"></div>
      <div class="ambient-orb ambient-orb--blue"></div>
    </div>

    <div class="snack-container">
      <!-- Header -->
      <header class="page-hero">
        <div class="page-hero__text">
          <h2 class="page-hero__title">卖品管理</h2>
          <p class="page-hero__desc">管理影院卖品库存、价格与套餐配置，打造极致观影伴侣体验。</p>
        </div>
        <button class="golden-btn" @click="openSnackDialog()">
          <span class="material-symbols-outlined">add</span>
          <span>添加商品</span>
        </button>
      </header>

      <!-- Category Filter Bar -->
      <div class="filter-bar">
        <div class="filter-pills">
          <button
            v-for="cat in categories"
            :key="cat.value"
            class="filter-pill"
            :class="{ active: filterCategory === cat.value }"
            @click="filterCategory = cat.value"
          >
            {{ cat.label }}
          </button>
        </div>
        <div class="filter-search">
          <span class="material-symbols-outlined search-icon">search</span>
          <input
            v-model="searchQuery"
            type="text"
            placeholder="搜索商品..."
            class="search-input"
          />
        </div>
      </div>

      <!-- Stats Row -->
      <div class="stats-row" v-if="!snacksLoading">
        <div class="stat-pill">
          <span class="material-symbols-outlined">inventory_2</span>
          <span>{{ snacks.length }} 个商品</span>
        </div>
        <div class="stat-pill stat-pill--green">
          <span class="material-symbols-outlined">check_circle</span>
          <span>{{ inStockCount }} 有库存</span>
        </div>
        <div class="stat-pill stat-pill--danger" v-if="outOfStockCount > 0">
          <span class="material-symbols-outlined">warning</span>
          <span>{{ outOfStockCount }} 售罄</span>
        </div>
      </div>

      <!-- Tab Switcher -->
      <div class="tab-switcher">
        <button class="tab-btn" :class="{ active: activeTab === 'snacks' }" @click="activeTab = 'snacks'">
          <span class="material-symbols-outlined">fastfood</span> 卖品列表
        </button>
        <button class="tab-btn" :class="{ active: activeTab === 'combos' }" @click="activeTab = 'combos'">
          <span class="material-symbols-outlined">package_2</span> 套餐管理
        </button>
        <button class="tab-btn" :class="{ active: activeTab === 'orders' }" @click="activeTab = 'orders'">
          <span class="material-symbols-outlined">receipt_long</span> 销售记录
        </button>
      </div>

      <!-- ========== TAB 1: Snack Grid ========== -->
      <div v-show="activeTab === 'snacks'">
        <div v-if="snacksLoading" class="loading-grid">
          <div v-for="i in 6" :key="i" class="skeleton-card">
            <div class="skeleton-img"></div>
            <div class="skeleton-body">
              <div class="skeleton-line w-60"></div>
              <div class="skeleton-line w-40"></div>
            </div>
          </div>
        </div>

        <el-empty v-else-if="filteredSnacks.length === 0" description="暂无卖品数据" :image-size="100" />

        <div v-else class="snack-grid">
          <div
            v-for="snack in filteredSnacks"
            :key="snack.id"
            class="snack-card"
            :class="{ 'snack-card--out': isOutOfStock(snack) }"
          >
            <!-- Image -->
            <div class="snack-card__img-wrap">
              <img
                v-if="snack.imageUrl"
                :src="snack.imageUrl"
                :alt="snack.name"
                class="snack-card__img"
              />
              <div v-else class="snack-card__img-placeholder">
                <span class="material-symbols-outlined">fastfood</span>
              </div>

              <!-- Stock Badge -->
              <div class="snack-card__badge" :class="isOutOfStock(snack) ? 'badge-out' : 'badge-ok'">
                {{ isOutOfStock(snack) ? '售罄' : '充足' }}
              </div>

              <!-- Out of stock overlay -->
              <div v-if="isOutOfStock(snack)" class="snack-card__overlay">
                <span>售罄</span>
              </div>
            </div>

            <!-- Info -->
            <div class="snack-card__body">
              <div class="snack-card__top">
                <h3 class="snack-card__name">{{ snack.name }}</h3>
                <span class="snack-card__price">¥{{ snack.price }}</span>
              </div>
              <p class="snack-card__desc">{{ snack.description || categoryLabel(snack.category) }}</p>
              <div class="snack-card__bottom">
                <span class="snack-card__stock" :class="{ 'text-danger': isLowStock(snack) }">
                  库存: {{ snack.stock === -1 ? '无限' : snack.stock + ' 份' }}
                </span>
                <div class="snack-card__actions">
                  <button class="icon-btn" title="编辑" @click="openSnackDialog(snack)">
                    <span class="material-symbols-outlined">edit</span>
                  </button>
                  <button class="icon-btn icon-btn--danger" title="删除" @click="handleSnackDelete(snack.id)">
                    <span class="material-symbols-outlined">delete</span>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- ========== TAB 2: Combos ========== -->
      <div v-show="activeTab === 'combos'">
        <div style="margin-bottom:16px">
          <button class="golden-btn" style="padding:10px 20px;font-size:13px" @click="openComboDialog">
            <span class="material-symbols-outlined">add</span>
            <span>添加套餐</span>
          </button>
        </div>
        <div class="snack-grid" v-if="combos.length > 0">
          <div v-for="combo in combos" :key="combo.id" class="snack-card">
            <div class="snack-card__img-wrap">
              <img v-if="combo.imageUrl" :src="combo.imageUrl" :alt="combo.name" class="snack-card__img" />
              <div v-else class="snack-card__img-placeholder">
                <span class="material-symbols-outlined">package_2</span>
              </div>
            </div>
            <div class="snack-card__body">
              <div class="snack-card__top">
                <h3 class="snack-card__name">{{ combo.name }}</h3>
                <span class="snack-card__price">¥{{ combo.price }}</span>
              </div>
              <p class="snack-card__desc" v-if="combo.originalPrice">
                原价 <s>¥{{ combo.originalPrice }}</s> &nbsp;省 ¥{{ (combo.originalPrice - combo.price).toFixed(0) }}
              </p>
              <div class="snack-card__bottom">
                <span class="snack-card__stock">{{ combo.snacks?.length || 0 }} 个单品</span>
                <div class="snack-card__actions">
                  <button class="icon-btn" @click="openComboDialog(combo)"><span class="material-symbols-outlined">edit</span></button>
                  <button class="icon-btn icon-btn--danger" @click="handleComboDelete(combo.id)"><span class="material-symbols-outlined">delete</span></button>
                </div>
              </div>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无套餐" :image-size="80" />
      </div>

      <!-- ========== TAB 3: Orders ========== -->
      <div v-show="activeTab === 'orders'">
        <el-table :data="snackOrders" v-loading="ordersLoading" stripe>
          <el-table-column prop="orderNo" label="订单号" width="160" />
          <el-table-column label="商品" min-width="200">
            <template #default="{ row }">
              <template v-if="row.items">
                <template v-for="(item, i) in parseSnackItems(row.items)" :key="i">
                  {{ item.name }}×{{ item.qty }}<span v-if="i < parseSnackItems(row.items).length - 1">, </span>
                </template>
              </template>
            </template>
          </el-table-column>
          <el-table-column label="金额" width="100"><template #default="{ row }">¥{{ row.totalAmount }}</template></el-table-column>
          <el-table-column label="时间" width="160"><template #default="{ row }">{{ row.createTime ? new Date(row.createTime).toLocaleString('zh-CN') : '--' }}</template></el-table-column>
        </el-table>
      </div>
    </div>

    <!-- ========== Add/Edit Snack Modal ========== -->
    <Teleport to="body">
      <div class="modal-overlay" :class="{ show: showSnackDialog }" @click.self="showSnackDialog = false">
        <div class="modal-panel" :class="{ show: showSnackDialog }">
          <div class="modal-header">
            <h2 class="modal-title">{{ editingSnack ? '编辑卖品' : '添加新商品' }}</h2>
            <button class="modal-close" @click="showSnackDialog = false">
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>
          <el-form ref="snackFormRef" :model="snackForm" :rules="snackRules" label-position="top" class="modal-form">
            <div class="form-row-2">
              <el-form-item label="商品名称" prop="name">
                <el-input v-model="snackForm.name" placeholder="输入卖品名称" />
              </el-form-item>
              <el-form-item label="所属分类" prop="category">
                <el-select v-model="snackForm.category" style="width:100%">
                  <el-option label="爆米花" value="POPCORN" /><el-option label="饮料" value="DRINK" />
                  <el-option label="零食" value="SNACK" /><el-option label="其他" value="OTHER" />
                </el-select>
              </el-form-item>
            </div>
            <div class="form-row-2">
              <el-form-item label="销售价格 (¥)" prop="price">
                <el-input-number v-model="snackForm.price" :min="0" :precision="2" style="width:100%" />
              </el-form-item>
              <el-form-item label="当前库存">
                <el-input-number v-model="snackForm.stock" :min="-1" style="width:100%" placeholder="-1 表示无限" />
              </el-form-item>
            </div>
            <el-form-item label="商品介绍">
              <el-input v-model="snackForm.description" type="textarea" :rows="3" placeholder="简短描述商品的特色内容..." />
            </el-form-item>
            <el-form-item label="图片链接">
              <el-input v-model="snackForm.imageUrl" placeholder="输入图片URL地址" />
            </el-form-item>
          </el-form>
          <div class="modal-actions">
            <button class="btn-cancel" @click="showSnackDialog = false">取消</button>
            <button class="golden-btn" :disabled="snackSubmitting" @click="handleSnackSubmit">
              <span v-if="snackSubmitting">保存中...</span>
              <span v-else>确认{{ editingSnack ? '更新' : '发布' }}</span>
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getSnacks, addSnack, updateSnack, deleteSnack, getCombos, getSnackOrders } from '@/api/snack'
import { ElMessage, ElMessageBox } from 'element-plus'

const activeTab = ref('snacks')
const filterCategory = ref('')
const searchQuery = ref('')
const snacks = ref([])
const snacksLoading = ref(false)
const combos = ref([])
const snackOrders = ref([])
const ordersLoading = ref(false)
const showSnackDialog = ref(false)
const editingSnack = ref(null)
const snackSubmitting = ref(false)
const snackFormRef = ref(null)

const snackForm = ref({ name: '', category: 'POPCORN', price: 0, stock: 0, description: '', imageUrl: '' })
const snackRules = {
  name: [{ required: true, message: '请输入名称' }],
  price: [{ required: true, message: '请输入价格' }],
  category: [{ required: true, message: '请选择分类' }],
}

const categories = [
  { label: '全部', value: '' },
  { label: '小吃', value: 'SNACK' },
  { label: '饮料', value: 'DRINK' },
  { label: '爆米花', value: 'POPCORN' },
  { label: '套餐', value: 'COMBO' },
  { label: '其他', value: 'OTHER' },
]

const filteredSnacks = computed(() => {
  let list = snacks.value
  if (filterCategory.value) list = list.filter(s => s.category === filterCategory.value)
  if (searchQuery.value) {
    const q = searchQuery.value.toLowerCase()
    list = list.filter(s => s.name?.toLowerCase().includes(q) || s.description?.toLowerCase().includes(q))
  }
  return list
})

const inStockCount = computed(() => snacks.value.filter(s => s.stock === -1 || s.stock > 0).length)
const outOfStockCount = computed(() => snacks.value.filter(s => s.stock !== -1 && s.stock <= 0).length)

function isOutOfStock(snack) { return snack.stock !== -1 && snack.stock !== null && snack.stock <= 0 }
function isLowStock(snack) { return snack.stock !== -1 && snack.stock !== null && snack.stock < 10 && snack.stock > 0 }
function categoryLabel(cat) { const m = { POPCORN: '爆米花', DRINK: '饮料', SNACK: '零食', COMBO: '套餐', OTHER: '其他' }; return m[cat] || cat }

function parseSnackItems(items) { try { return typeof items === 'string' ? JSON.parse(items) : items } catch { return [] } }

async function fetchSnacks() { snacksLoading.value = true; try { const r = await getSnacks(); snacks.value = r.data || [] } catch {} snacksLoading.value = false }
async function fetchCombos() { try { const r = await getCombos(); combos.value = r.data || [] } catch {} }
async function fetchOrders() { ordersLoading.value = true; try { const r = await getSnackOrders(); snackOrders.value = r.data || [] } catch {} ordersLoading.value = false }

function openSnackDialog(row) {
  editingSnack.value = row || null
  snackForm.value = row
    ? { name: row.name, category: row.category, price: row.price, stock: row.stock, description: row.description || '', imageUrl: row.imageUrl || '' }
    : { name: '', category: 'POPCORN', price: 0, stock: 0, description: '', imageUrl: '' }
  showSnackDialog.value = true
}

function openComboDialog() { ElMessage.info('套餐功能请通过后端接口管理') }

async function handleSnackSubmit() {
  if (!snackFormRef.value) return
  const v = await snackFormRef.value.validate().catch(() => false)
  if (!v) return
  snackSubmitting.value = true
  try {
    if (editingSnack.value) await updateSnack({ ...snackForm.value, id: editingSnack.value.id })
    else await addSnack({ ...snackForm.value })
    ElMessage.success(editingSnack.value ? '已更新' : '已添加')
    showSnackDialog.value = false
    fetchSnacks()
  } catch {} finally { snackSubmitting.value = false }
}

async function handleSnackDelete(id) {
  try { await ElMessageBox.confirm('确定删除该卖品？', '确认删除', { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }) }
  catch { return }
  try { await deleteSnack(id); ElMessage.success('已删除'); fetchSnacks() } catch {}
}

async function handleComboDelete(id) {
  try { await ElMessageBox.confirm('确定删除该套餐？', '确认删除', { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }) }
  catch { return }
  try { await deleteSnack(id); ElMessage.success('已删除'); fetchCombos() } catch {}
}

onMounted(() => { fetchSnacks(); fetchCombos(); fetchOrders() })
</script>

<style scoped>
/* ============================================================
   Snack Management — Dark Cinema Gold Edition
   ============================================================ */

.snack-page {
  min-height: 100vh;
  position: relative;
  padding: 0 0 48px;
}

.snack-container {
  max-width: 1340px;
  margin: 0 auto;
  padding: 24px 32px;
  position: relative;
  z-index: 1;
}

/* ---- Ambient Background ---- */
.ambient-bg { position: fixed; inset: 0; overflow: hidden; pointer-events: none; z-index: 0; }
.ambient-orb { position: absolute; border-radius: 50%; filter: blur(120px); }
.ambient-orb--gold { top: -200px; right: -200px; width: 600px; height: 600px; background: rgba(232, 168, 80, 0.04); }
.ambient-orb--blue { bottom: -100px; left: -100px; width: 400px; height: 400px; background: rgba(43, 58, 94, 0.15); }

/* ---- Hero Header ---- */
.page-hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 28px;
}

.page-hero__title {
  font-family: Georgia, 'Noto Serif SC', serif;
  font-size: 42px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.5px;
  margin-bottom: 6px;
}

.page-hero__desc {
  font-size: 14px;
  color: var(--text-secondary);
  max-width: 420px;
}

/* ---- Golden Button ---- */
.golden-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 28px;
  border: none;
  border-radius: var(--radius-lg);
  font-size: 14px;
  font-weight: 700;
  color: #2a1800;
  cursor: pointer;
  background: linear-gradient(135deg, #e8a850 0%, #ffc67c 50%, #e8a850 100%);
  transition: all 0.3s ease;
  font-family: inherit;
  white-space: nowrap;
}

.golden-btn:hover:not(:disabled) {
  filter: brightness(1.1);
  box-shadow: 0 0 24px rgba(232, 168, 80, 0.35);
  transform: scale(1.02);
}

.golden-btn:active:not(:disabled) { transform: scale(0.97); }
.golden-btn:disabled { opacity: 0.5; cursor: not-allowed; }

.golden-btn .material-symbols-outlined { font-size: 20px; }

/* ---- Filter Bar ---- */
.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  gap: 16px;
  flex-wrap: wrap;
}

.filter-pills {
  display: flex;
  background: var(--bg-card);
  padding: 4px;
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-light);
}

.filter-pill {
  padding: 8px 22px;
  border: none;
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  background: transparent;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: inherit;
}

.filter-pill:hover { color: var(--text-primary); }

.filter-pill.active {
  background: var(--bg-hover);
  color: var(--color-primary);
  box-shadow: var(--shadow-light);
}

.filter-search {
  position: relative;
}

.search-icon {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 18px;
  color: var(--text-tertiary);
  pointer-events: none;
  transition: color 0.2s ease;
}

.filter-search:focus-within .search-icon { color: var(--color-primary); }

.search-input {
  width: 240px;
  padding: 10px 14px 10px 38px;
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  color: var(--text-primary);
  font-size: 13px;
  font-family: inherit;
  outline: none;
  transition: all 0.2s ease;
}

.search-input:focus {
  border-color: rgba(232, 168, 80, 0.4);
  box-shadow: 0 0 0 3px rgba(232, 168, 80, 0.08);
}

.search-input::placeholder { color: var(--text-tertiary); }

/* ---- Stats Row ---- */
.stats-row { display: flex; gap: 12px; margin-bottom: 24px; flex-wrap: wrap; }

.stat-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: var(--radius-pill);
  font-size: 12px;
  font-weight: 500;
  color: var(--text-secondary);
  background: var(--bg-card);
  border: 1px solid var(--border-light);
}

.stat-pill .material-symbols-outlined { font-size: 16px; }
.stat-pill--green { color: var(--color-emerald); }
.stat-pill--danger { color: var(--color-danger); }

/* ---- Tab Switcher ---- */
.tab-switcher {
  display: flex;
  gap: 4px;
  margin-bottom: 24px;
  border-bottom: 1px solid var(--border-light);
  padding-bottom: 0;
}

.tab-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  border: none;
  border-bottom: 2px solid transparent;
  background: transparent;
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: inherit;
  margin-bottom: -1px;
}

.tab-btn:hover { color: var(--text-primary); }
.tab-btn.active { color: var(--color-primary); border-bottom-color: var(--color-primary); font-weight: 600; }
.tab-btn .material-symbols-outlined { font-size: 18px; }

/* ---- Snack Grid ---- */
.snack-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(270px, 1fr));
  gap: 20px;
}

/* ---- Snack Card ---- */
.snack-card {
  background: var(--bg-card);
  border-radius: var(--radius-xl);
  border: 1px solid var(--border-light);
  overflow: hidden;
  transition: all 0.3s ease;
}

.snack-card:hover {
  box-shadow: 0 0 20px rgba(232, 168, 80, 0.12);
  border-color: rgba(232, 168, 80, 0.25);
  transform: translateY(-2px);
}

/* Out of stock */
.snack-card--out { opacity: 0.75; }
.snack-card--out .snack-card__img { filter: grayscale(0.4); }
.snack-card--out .snack-card__name,
.snack-card--out .snack-card__price { opacity: 0.6; }

.snack-card__img-wrap {
  position: relative;
  aspect-ratio: 4/3;
  overflow: hidden;
  background: var(--bg-secondary);
}

.snack-card__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}

.snack-card:hover .snack-card__img { transform: scale(1.1); }

.snack-card__img-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-tertiary);
}

.snack-card__img-placeholder .material-symbols-outlined { font-size: 48px; }

/* Stock badge */
.snack-card__badge {
  position: absolute;
  top: 10px;
  right: 10px;
  padding: 4px 12px;
  border-radius: var(--radius-pill);
  font-size: 11px;
  font-weight: 700;
  backdrop-filter: blur(12px);
  letter-spacing: 0.03em;
}

.badge-ok {
  background: rgba(26, 107, 76, 0.2);
  color: #4ade80;
  border: 1px solid rgba(26, 107, 76, 0.3);
}

.badge-out {
  background: rgba(147, 0, 10, 0.6);
  color: #fff;
  border: 1px solid rgba(255, 180, 171, 0.3);
}

/* Out of stock overlay */
.snack-card__overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  backdrop-filter: blur(2px);
  display: flex;
  align-items: center;
  justify-content: center;
}

.snack-card__overlay span {
  padding: 6px 20px;
  background: rgba(147, 0, 10, 0.8);
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(255, 180, 171, 0.5);
}

/* Card body */
.snack-card__body { padding: 18px; }

.snack-card__top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 6px;
}

.snack-card__name {
  font-size: 17px;
  font-weight: 600;
  color: var(--text-primary);
}

.snack-card__price {
  font-family: Georgia, 'Noto Serif SC', serif;
  font-size: 18px;
  font-weight: 700;
  color: var(--color-primary);
  white-space: nowrap;
}

.snack-card__desc {
  font-size: 12px;
  color: var(--text-tertiary);
  margin-bottom: 14px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.snack-card__bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.snack-card__stock {
  font-size: 11px;
  color: var(--text-tertiary);
  font-weight: 500;
}

.text-danger { color: var(--color-danger) !important; }

.snack-card__actions { display: flex; gap: 4px; }

.icon-btn {
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: var(--radius-md);
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.15s ease;
}

.icon-btn:hover { background: var(--bg-hover); color: var(--color-primary); }
.icon-btn--danger:hover { color: var(--color-danger); }
.icon-btn .material-symbols-outlined { font-size: 18px; }

/* ---- Loading ---- */
.loading-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(270px, 1fr));
  gap: 20px;
}

.skeleton-card {
  background: var(--bg-card);
  border-radius: var(--radius-xl);
  overflow: hidden;
  border: 1px solid var(--border-light);
}

.skeleton-img {
  aspect-ratio: 4/3;
  background: var(--bg-hover);
}

.skeleton-body {
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.skeleton-line {
  height: 14px;
  background: var(--bg-hover);
  border-radius: 4px;
}

.skeleton-line.w-60 { width: 60%; }
.skeleton-line.w-40 { width: 40%; }

/* ============================================================
   MODAL — Glass Panel
   ============================================================ */
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(0, 0, 0, 0.7);
  backdrop-filter: blur(4px);
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.3s ease;
}

.modal-overlay.show { opacity: 1; pointer-events: auto; }

.modal-panel {
  width: 100%;
  max-width: 600px;
  max-height: 90vh;
  overflow-y: auto;
  background: var(--bg-card);
  border-radius: var(--radius-xl);
  border: 1px solid rgba(255, 255, 255, 0.06);
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.6);
  transform: scale(0.95) translateY(12px);
  transition: transform 0.3s ease;
}

[data-theme='dark'] .modal-panel {
  background: rgba(20, 20, 31, 0.92);
  backdrop-filter: blur(20px);
}

.modal-panel.show { transform: scale(1) translateY(0); }

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 28px 28px 0;
}

.modal-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
}

.modal-close {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.15s ease;
}

.modal-close:hover { background: var(--bg-hover); color: var(--color-primary); }
.modal-close .material-symbols-outlined { font-size: 20px; }

.modal-form { padding: 24px 28px; }
.form-row-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 0 20px; }

.modal-actions {
  display: flex;
  gap: 12px;
  padding: 0 28px 28px;
}

.modal-actions .golden-btn { flex: 2; justify-content: center; padding: 14px; }

.btn-cancel {
  flex: 1;
  padding: 14px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  background: transparent;
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: inherit;
}

.btn-cancel:hover { background: var(--bg-hover); }

/* ---- Responsive ---- */
@media (max-width: 768px) {
  .page-hero { flex-direction: column; align-items: flex-start; gap: 16px; }
  .page-hero__title { font-size: 28px; }
  .filter-bar { flex-direction: column; align-items: stretch; }
  .filter-pills { overflow-x: auto; }
  .search-input { width: 100%; }
  .snack-grid { grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); }
  .form-row-2 { grid-template-columns: 1fr; }
}
</style>
