<template>
  <div class="admin-layout">
    <NavBar />
    <div class="admin-content">
      <div class="page-header">
        <h2>影片管理</h2>
        <el-button type="primary" :icon="Plus" @click="openAdd">添加影片</el-button>
      </div>

      <div class="toolbar">
        <el-input v-model="searchText" placeholder="搜索影片名称..." :prefix-icon="Search" clearable style="width: 280px;" />
      </div>

      <div class="card">
        <el-table :data="movies" v-loading="loading" stripe>
          <el-table-column label="海报" width="90">
            <template #default="{ row }">
              <img :src="row.poster || defaultPoster" class="table-poster" @error="onImgError" />
            </template>
          </el-table-column>
          <el-table-column prop="name" label="影片名称" min-width="150" show-overflow-tooltip />
          <el-table-column prop="genre" label="类型" width="100" />
          <el-table-column prop="duration" label="时长(分)" width="90" />
          <el-table-column prop="releaseDate" label="上映日期" width="120" />
          <el-table-column label="票价" width="100">
            <template #default="{ row }">¥{{ row.price || '--' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : row.status === 2 ? 'warning' : 'danger'" size="small">
                {{ row.status === 1 ? '上映中' : row.status === 2 ? '即将上映' : '已下架' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="热映" width="80">
            <template #default="{ row }">
              <el-tag v-if="row.isHot === 1" type="danger" size="small">热映</el-tag>
              <span v-else style="color: var(--text-muted)">--</span>
            </template>
          </el-table-column>
          <el-table-column prop="rating" label="评分" width="80" />
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <el-button size="small" text type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button size="small" text :type="row.isHot ? 'warning' : 'success'" @click="toggleHot(row)">{{ row.isHot ? '取消热映' : '设为热映' }}</el-button>
              <el-button size="small" text type="danger" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div style="display: flex; justify-content: flex-end; padding: 16px 0;">
          <el-pagination
            v-model:current-page="currentPage"
            :page-size="pageSize"
            :total="total"
            layout="total, prev, pager, next, jumper"
            @current-change="handlePageChange"
          />
        </div>
      </div>

      <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑影片' : '添加影片'" width="620px" :close-on-click-modal="false">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="影片名称" prop="name"><el-input v-model="form.name" placeholder="请输入影片名称" /></el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="类型" prop="genre">
                <el-select v-model="form.genre" multiple placeholder="请选择类型（可多选）" style="width: 100%" collapse-tags collapse-tags-tooltip>
                  <el-option v-for="g in genreOptions" :key="g" :label="g" :value="g" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="时长(分钟)" prop="duration"><el-input-number v-model="form.duration" :min="1" :max="500" style="width: 100%" /></el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="票价" prop="price"><el-input-number v-model="form.price" :min="0" :precision="2" style="width: 100%" /></el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="导演" prop="director"><el-input v-model="form.director" placeholder="请输入导演" /></el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="演员" prop="actors"><el-input v-model="form.actors" placeholder="请输入主要演员(逗号分隔)" /></el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="国家/地区"><el-input v-model="form.country" placeholder="如：中国" /></el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="语言"><el-input v-model="form.language" placeholder="如：国语" /></el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="评分">
            <el-rate v-model="form.rating" :max="10" allow-half show-score />
          </el-form-item>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="上映日期"><el-date-picker v-model="form.releaseDate" type="date" placeholder="选择日期" style="width: 100%" value-format="YYYY-MM-DD" /></el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="状态">
                <el-select v-model="form.status" style="width: 100%">
                  <el-option :value="1" label="上架" />
                  <el-option :value="2" label="即将上映" />
                  <el-option :value="0" label="下架" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="海报图片">
            <div style="display: flex; gap: 12px; align-items: center; flex-wrap: wrap;">
              <el-upload
                :auto-upload="true"
                :show-file-list="false"
                :before-upload="beforePosterUpload"
                :http-request="handlePosterUpload"
                accept="image/*"
              >
                <el-button type="primary" :loading="uploading">选择图片上传</el-button>
              </el-upload>
              <span style="color: var(--text-muted); font-size: 13px;">或</span>
              <el-input v-model="form.poster" placeholder="输入海报图片URL" style="flex: 1; min-width: 200px;" />
            </div>
            <div v-if="form.poster" style="margin-top: 8px;">
              <img :src="form.poster" style="max-height: 120px; border-radius: 4px;" @error="$event.target.style.display='none'" />
            </div>
          </el-form-item>
          <el-form-item label="影片描述"><el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入影片描述" /></el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">{{ isEdit ? '保存修改' : '添加影片' }}</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive, watch } from 'vue'
import { getMovieList, searchMovies, addMovie, updateMovie, deleteMovie, uploadPoster } from '@/api/movie'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import NavBar from '@/components/NavBar.vue'

const movies = ref([])
const loading = ref(false)
const searchText = ref('')
const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref(null)
const submitting = ref(false)
const uploading = ref(false)
const formRef = ref(null)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const genreOptions = ['动作', '喜剧', '爱情', '科幻', '恐怖', '动画', '剧情', '悬疑', '战争', '纪录片', '奇幻', '犯罪', '冒险']

const defaultPoster = 'data:image/svg+xml,' + encodeURIComponent(`<svg xmlns="http://www.w3.org/2000/svg" width="80" height="106" viewBox="0 0 80 106"><rect fill="#1a1a2e" width="80" height="106"/><text fill="#7a8096" font-family="Arial" font-size="8" text-anchor="middle" x="40" y="56">暂无</text></svg>`)

function onImgError(e) { e.target.src = defaultPoster }

const form = reactive({ name: '', genre: [], duration: 90, price: 0, director: '', actors: '', country: '', language: '', releaseDate: '', rating: 0, poster: '', description: '', status: 1 })

const rules = {
  name: [{ required: true, message: '请输入影片名称', trigger: 'blur' }],
  genre: [{ type: 'array', required: true, message: '请至少选择一个类型', trigger: 'change' }],
  duration: [{ required: true, message: '请输入时长', trigger: 'blur' }],
  price: [{ required: true, message: '请输入票价', trigger: 'blur' }]
}

function resetForm() {
  Object.assign(form, { name: '', genre: [], duration: 90, price: 0, director: '', actors: '', country: '', language: '', releaseDate: '', rating: 0, poster: '', description: '', status: 1 })
}

function openAdd() { isEdit.value = false; editingId.value = null; resetForm(); dialogVisible.value = true }

function openEdit(row) {
  isEdit.value = true; editingId.value = row.id
  form.name = row.name; form.genre = row.genre ? row.genre.split(',').filter(g => g.trim()) : []; form.duration = row.duration; form.price = row.price
  form.director = row.director || ''; form.actors = row.actors || ''; form.country = row.country || ''; form.language = row.language || ''
  form.releaseDate = row.releaseDate; form.rating = row.rating || 0; form.poster = row.poster || ''; form.description = row.description || ''; form.status = row.status
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const genreStr = Array.isArray(form.genre) ? form.genre.join(',') : form.genre
    const payload = { name: form.name, genre: genreStr, duration: form.duration, price: form.price, director: form.director, actors: form.actors, country: form.country, language: form.language, releaseDate: form.releaseDate, rating: form.rating, poster: form.poster, description: form.description, status: form.status }
    if (isEdit.value) {
      await updateMovie({ id: editingId.value, ...payload })
      ElMessage.success('影片更新成功')
    } else {
      await addMovie(payload)
      ElMessage.success('影片添加成功')
    }
    dialogVisible.value = false
    fetchMovies()
  } catch (err) { /* handled */ } finally { submitting.value = false }
}

async function handleDelete(row) {
  try { await ElMessageBox.confirm(`确定要删除"${row.name}"吗？`, '删除确认', { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }) } catch { return }
  try { await deleteMovie(row.id); ElMessage.success('已删除'); fetchMovies() } catch (err) { /* handled */ }
}

async function toggleHot(row) {
  try {
    const newHot = row.isHot ? 0 : 1
    await updateMovie({ id: row.id, isHot: newHot })
    row.isHot = newHot
    ElMessage.success(newHot ? '已设为热映' : '已取消热映')
  } catch (err) { /* handled */ }
}

function beforePosterUpload(file) {
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('只能上传图片文件！')
    return false
  }
  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isLt10M) {
    ElMessage.error('图片大小不能超过 10MB！')
    return false
  }
  return true
}

async function handlePosterUpload(options) {
  uploading.value = true
  try {
    const res = await uploadPoster(options.file)
    if (res.data && res.data.url) {
      form.poster = res.data.url
      ElMessage.success('海报上传成功')
    }
  } catch (err) {
    // Error already shown by axios interceptor
  } finally {
    uploading.value = false
  }
}

async function fetchMovies() {
  loading.value = true
  try {
    const keyword = searchText.value?.trim()
    let res
    if (keyword) {
      res = await searchMovies(keyword)
      movies.value = res.data || []
      total.value = movies.value.length
    } else {
      res = await getMovieList({ page: currentPage.value, size: pageSize.value })
      movies.value = res.data?.records || res.data || []
      total.value = res.data?.total || 0
    }
  } catch (err) { movies.value = []; total.value = 0 } finally { loading.value = false }
}

function handlePageChange(page) {
  currentPage.value = page
  fetchMovies()
}

// 搜索防抖：输入停止300ms后自动触发服务端搜索
let searchTimer = null
watch(searchText, () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    currentPage.value = 1
    fetchMovies()
  }, 300)
})

onMounted(fetchMovies)
</script>

<style scoped>
.admin-layout { min-height: 100vh; background: var(--bg-secondary); }
.admin-content { max-width: 1400px; margin: 0 auto; padding: 24px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h2 { font-size: 22px; font-weight: 700; color: var(--text-primary); }
.toolbar { margin-bottom: 16px; }
.card { background: var(--bg-card); border-radius: var(--radius-md); box-shadow: var(--shadow-light); }
.table-poster { width: 60px; height: 80px; object-fit: cover; border-radius: 4px; }
</style>
