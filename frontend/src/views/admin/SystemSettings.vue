<template>
  <div class="admin-layout">
    <NavBar />
    <div class="admin-content">
      <div class="page-header">
        <h2>系统设置</h2>
      </div>

      <!-- Theme Management -->
      <div class="card" style="margin-bottom: 20px;">
        <div class="card-header"><h3>主题管理</h3></div>
        <div class="card-body">
          <div class="theme-cards">
            <div
              v-for="theme in themes"
              :key="theme.key"
              class="theme-card"
              :class="{ active: defaultTheme === theme.key }"
              @click="defaultTheme = theme.key"
            >
              <div class="theme-preview" :class="`theme-preview-${theme.key}`">
                <div class="preview-bar"></div>
                <div class="preview-body">
                  <div class="preview-sidebar"></div>
                  <div class="preview-content"></div>
                </div>
              </div>
              <div class="theme-info">
                <span class="theme-name">{{ theme.name }}</span>
                <span class="theme-desc">{{ theme.desc }}</span>
              </div>
              <el-icon v-if="defaultTheme === theme.key" class="theme-check"><CircleCheckFilled /></el-icon>
            </div>
          </div>
          <div style="margin-top: 16px;">
            <el-button type="primary" :loading="savingTheme" @click="saveTheme">应用主题</el-button>
          </div>
        </div>
      </div>

      <!-- System Config -->
      <div class="card" style="margin-bottom: 20px;">
        <div class="card-header"><h3>系统配置</h3></div>
        <div class="card-body">
          <el-form :model="config" label-width="140px" size="default">
            <el-form-item label="系统名称">
              <el-input v-model="config.systemName" placeholder="系统名称" />
            </el-form-item>
            <el-form-item label="联系电话">
              <el-input v-model="config.contactPhone" placeholder="联系电话" />
            </el-form-item>
            <el-form-item label="联系邮箱">
              <el-input v-model="config.contactEmail" placeholder="联系邮箱" />
            </el-form-item>
            <el-form-item label="最大选座数">
              <el-input-number v-model="config.maxSeatSelect" :min="1" :max="20" />
            </el-form-item>
            <el-form-item label="退票截止时间">
              <el-input v-model="config.refundDeadline" placeholder="如：开场前30分钟">
                <template #append>分钟</template>
              </el-input>
            </el-form-item>
            <el-form-item label="影院分账比例">
              <el-input v-model.number="config.share_ratio" placeholder="52">
                <template #append>%</template>
              </el-input>
            </el-form-item>
            <el-form-item label="是否开启注册">
              <el-switch v-model="config.allowRegister" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="savingConfig" @click="saveConfig">保存配置</el-button>
              <el-button @click="resetConfig">重置</el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>

      <!-- Operation Logs -->
      <div class="card">
        <div class="card-header">
          <h3>操作日志</h3>
          <el-button :icon="Refresh" size="small" @click="fetchLogs">刷新</el-button>
        </div>
        <div class="card-body">
          <el-table :data="logs" v-loading="logLoading" stripe max-height="400">
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="operator" label="操作人" width="120" />
            <el-table-column prop="action" label="操作" min-width="140" show-overflow-tooltip />
            <el-table-column prop="module" label="模块" width="100" />
            <el-table-column prop="result" label="结果" width="90">
              <template #default="{ row }">
                <el-tag :type="row.result === '成功' ? 'success' : 'danger'" size="small">{{ row.result || '成功' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="ip" label="IP" width="140" />
            <el-table-column label="时间" width="170">
              <template #default="{ row }">{{ formatDateTime(row.createTime || row.createdAt || row.operateTime) }}</template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!logLoading && logs.length === 0" description="暂无日志" :image-size="100" />
          <!-- Pagination -->
          <div style="margin-top: 16px; text-align: right;" v-if="logs.length > 0">
            <el-pagination
              v-model:current-page="logPage"
              :page-size="logPageSize"
              :total="logTotal"
              layout="total, prev, pager, next"
              background
              small
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { getSystemConfig, updateSystemConfig, setSystemTheme, getSystemLogs } from '@/api/order'
import { useThemeStore } from '@/stores/theme'
import { ElMessage } from 'element-plus'
import { CircleCheckFilled, Refresh } from '@element-plus/icons-vue'
import NavBar from '@/components/NavBar.vue'

const themeStore = useThemeStore()

const themes = [
  { key: 'light', name: '浅色模式', desc: '明亮通透，适合日常办公' },
  { key: 'dark', name: '深色模式', desc: '深邃暗色，护眼沉浸体验' }
]

const defaultTheme = ref('light')
const savingTheme = ref(false)
const savingConfig = ref(false)

const config = reactive({
  systemName: 'TTMS 电影院综合管理系统',
  contactPhone: '',
  contactEmail: '',
  maxSeatSelect: 6,
  refundDeadline: '30',
  allowRegister: true,
  share_ratio: 52
})

const originalConfig = { ...config }

const allLogs = ref([])
const logLoading = ref(false)
const logPage = ref(1)
const logPageSize = ref(15)
const logTotal = ref(0)

// 使用computed确保分页切换时数据响应式更新
const logs = computed(() => {
  const start = (logPage.value - 1) * logPageSize.value
  return allLogs.value.slice(start, start + logPageSize.value)
})

function formatDateTime(s) {
  if (!s) return '--'
  const d = new Date(s)
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
}

async function saveTheme() {
  savingTheme.value = true
  try {
    await setSystemTheme({ theme: defaultTheme.value })
    themeStore.setTheme(defaultTheme.value)
    ElMessage.success('主题已更新')
  } catch (err) { /* handled */ }
  finally { savingTheme.value = false }
}

async function saveConfig() {
  savingConfig.value = true
  try {
    await updateSystemConfig({ ...config })
    Object.assign(originalConfig, config)
    ElMessage.success('配置已保存')
  } catch (err) { /* handled */ }
  finally { savingConfig.value = false }
}

function resetConfig() {
  Object.assign(config, originalConfig)
}

async function fetchConfig() {
  try {
    const res = await getSystemConfig()
    const data = res.data || {}
    Object.assign(config, {
      systemName: data.systemName || config.systemName,
      contactPhone: data.contactPhone || '',
      contactEmail: data.contactEmail || '',
      maxSeatSelect: data.maxSeatSelect || 6,
      refundDeadline: data.refundDeadline || '30',
      allowRegister: data.allowRegister !== false,
      share_ratio: data.share_ratio ? parseInt(data.share_ratio) : 52
    })
    Object.assign(originalConfig, config)
    if (data.theme) {
      defaultTheme.value = data.theme
    }
  } catch (err) { /* use defaults */ }
}

async function fetchLogs() {
  logLoading.value = true
  try {
    const res = await getSystemLogs()
    allLogs.value = res.data || []
    logTotal.value = allLogs.value.length
    logPage.value = 1
  } catch (err) {
    allLogs.value = []
    logTotal.value = 0
  }
  finally { logLoading.value = false }
}

onMounted(() => {
  fetchConfig()
  fetchLogs()
})
</script>

<style scoped>
.admin-layout { min-height: 100vh; background: var(--bg-secondary); }
.admin-content { max-width: 1400px; margin: 0 auto; padding: 24px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h2 { font-size: 22px; font-weight: 700; color: var(--text-primary); }
.card { background: var(--bg-card); border-radius: var(--radius-md); box-shadow: var(--shadow-light); }
.card-header { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; border-bottom: 1px solid var(--border-light); }
.card-header h3 { font-size: 16px; font-weight: 600; color: var(--text-primary); }
.card-body { padding: 20px; }

/* Theme cards */
.theme-cards { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }
.theme-card { padding: 16px; border-radius: var(--radius-md); border: 2px solid var(--border-color); cursor: pointer; transition: all 0.2s; position: relative; }
.theme-card:hover { border-color: var(--color-primary-light); }
.theme-card.active { border-color: var(--color-primary); }
.theme-check { position: absolute; top: 8px; right: 8px; color: var(--color-primary); font-size: 20px; }
.theme-preview { height: 70px; border-radius: 4px; overflow: hidden; margin-bottom: 12px; }
.preview-bar { height: 8px; }
.preview-body { display: flex; height: 62px; }
.preview-sidebar { width: 20px; }
.preview-content { flex: 1; }
.theme-info { display: flex; flex-direction: column; gap: 2px; }
.theme-name { font-size: 14px; font-weight: 600; color: var(--text-primary); }
.theme-desc { font-size: 13px; color: var(--text-muted); }

.theme-preview-white { background: #f5f7fa; }
.theme-preview-white .preview-bar { background: #fff; }
.theme-preview-white .preview-sidebar { background: #e4e7ed; }
.theme-preview-white .preview-content { background: #fff; }

.theme-preview-dark { background: #16213e; }
.theme-preview-dark .preview-bar { background: #1a1a2e; }
.theme-preview-dark .preview-sidebar { background: #0f0f23; }
.theme-preview-dark .preview-content { background: #1e2a4a; }

@media (max-width: 768px) {
  .theme-cards { grid-template-columns: 1fr; }
}
</style>
