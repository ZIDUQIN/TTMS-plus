<template>
  <div class="admin-layout">
    <!-- Atmospheric Background -->
    <div class="movie-atmosphere"></div>

    <div class="admin-content movie-page">
      <!-- Action Bar -->
      <div class="mv-bar">
        <div>
          <h2 class="mv-title">影片库</h2>
          <p class="mv-sub">共管理 {{ total }} 部影片，其中 {{ hotCount }} 部正在热映</p>
        </div>
        <div class="mv-actions">
          <div class="mv-filters">
            <button v-for="f in filters" :key="f.value" class="mv-fbtn" :class="{ active: activeFilter === f.value }"
              @click="activeFilter = f.value">{{ f.label }}</button>
          </div>
          <button class="mv-add-btn" @click="openAdd">
            <el-icon :size="18"><Plus /></el-icon>
            <span>添加影片</span>
          </button>
        </div>
      </div>

      <!-- Movie Grid -->
      <div v-loading="loading" class="mv-grid">
        <el-empty v-if="!loading && filteredMovies.length === 0" description="暂无影片" :image-size="100" />

        <div v-for="m in filteredMovies" :key="m.id" class="mv-card"
          @mousemove="onCardHover($event, m.id)" @mouseleave="clearHalo(m.id)">
          <!-- Poster -->
          <div class="mv-poster">
            <img :src="m.poster || defaultPoster" @error="onImgError" />
            <div class="mv-poster-gradient"></div>
            <span class="mv-status" :class="statusClass(m.status)">{{ statusLabel(m.status) }}</span>
            <!-- Hover overlay -->
            <div class="mv-overlay">
              <button class="mv-obtn" title="编辑" @click.stop="openEdit(m)"><el-icon :size="16"><Edit /></el-icon></button>
              <button class="mv-obtn" title="热映切换" @click.stop="toggleHot(m)"><el-icon :size="16"><StarFilled /></el-icon></button>
              <button class="mv-obtn mv-obtn-del" title="删除" @click.stop="handleDelete(m)"><el-icon :size="16"><Delete /></el-icon></button>
            </div>
          </div>
          <!-- Info -->
          <div class="mv-info" :ref="el => setHaloRef(m.id, el)">
            <h3 class="mv-name">{{ m.name }}</h3>
            <div class="mv-meta">
              <span class="mv-date">{{ m.releaseDate || '--' }}</span>
              <span class="mv-dot"></span>
              <span>{{ m.genre || '--' }}</span>
            </div>
            <div class="mv-foot">
              <div class="mv-rating" v-if="m.rating">
                <el-icon :size="14"><StarFilled /></el-icon>
                <span>{{ m.rating }}</span>
              </div>
              <span v-else class="mv-no-rating">暂无评分</span>
              <span class="mv-tag" v-if="m.duration">{{ m.duration }}分钟</span>
            </div>
          </div>
        </div>

        <!-- Empty Add Card -->
        <div class="mv-card mv-card-add" @click="openAdd">
          <div class="mv-add-icon">
            <el-icon :size="32"><VideoCameraFilled /></el-icon>
          </div>
          <p class="mv-add-title">导入新影片</p>
          <p class="mv-add-desc">支持手动录入影片信息</p>
        </div>
      </div>

      <!-- Pagination -->
      <div class="mv-pager" v-if="total > pageSize">
        <button class="mv-pg-btn" :disabled="currentPage <= 1" @click="handlePageChange(currentPage - 1)">
          <el-icon><ArrowLeft /></el-icon>
        </button>
        <div class="mv-pg-nums">
          <button v-for="p in pageList" :key="p"
            class="mv-pg-num" :class="{ active: p === currentPage, dot: p === '...' }"
            :disabled="p === '...'" @click="p !== '...' && handlePageChange(p)">{{ p }}</button>
        </div>
        <button class="mv-pg-btn" :disabled="currentPage >= totalPages" @click="handlePageChange(currentPage + 1)">
          <el-icon><ArrowRight /></el-icon>
        </button>
      </div>

      <!-- Dialog -->
      <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑影片' : '添加影片'" width="620px" :close-on-click-modal="false">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
          <el-row :gutter="16">
            <el-col :span="12"><el-form-item label="影片名称" prop="name"><el-input v-model="form.name" /></el-form-item></el-col>
            <el-col :span="12">
              <el-form-item label="类型" prop="genre">
                <el-select v-model="form.genre" multiple placeholder="多选" style="width:100%" collapse-tags>
                  <el-option v-for="g in genreOptions" :key="g" :label="g" :value="g" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12"><el-form-item label="时长(分钟)" prop="duration"><el-input-number v-model="form.duration" :min="1" :max="500" style="width:100%" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="票价" prop="price"><el-input-number v-model="form.price" :min="0" :precision="2" style="width:100%" /></el-form-item></el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12"><el-form-item label="导演"><el-input v-model="form.director" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="演员"><el-input v-model="form.actors" /></el-form-item></el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12"><el-form-item label="国家"><el-input v-model="form.country" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="语言"><el-input v-model="form.language" /></el-form-item></el-col>
          </el-row>
          <el-form-item label="评分"><el-rate v-model="form.rating" :max="10" allow-half show-score /></el-form-item>
          <el-row :gutter="16">
            <el-col :span="12"><el-form-item label="上映日期"><el-date-picker v-model="form.releaseDate" type="date" style="width:100%" value-format="YYYY-MM-DD" /></el-form-item></el-col>
            <el-col :span="12">
              <el-form-item label="状态"><el-select v-model="form.status" style="width:100%"><el-option :value="1" label="上架" /><el-option :value="2" label="即将上映" /><el-option :value="0" label="下架" /></el-select></el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="海报">
            <div style="display:flex;gap:10px;align-items:center">
              <el-upload :auto-upload="true" :show-file-list="false" :before-upload="beforePosterUpload" :http-request="handlePosterUpload" accept="image/*">
                <el-button type="primary" :loading="uploading">上传图片</el-button>
              </el-upload>
              <span style="color:var(--text-muted);font-size:12px">或</span>
              <el-input v-model="form.poster" placeholder="海报URL" style="flex:1" />
            </div>
            <img v-if="form.poster" :src="form.poster" style="max-height:100px;margin-top:8px;border-radius:6px" @error="e=>e.target.style.display='none'" />
          </el-form-item>
          <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dialogVisible=false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">{{ isEdit ? '保存' : '添加' }}</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive, watch } from 'vue'
import { getMovieList, searchMovies, addMovie, updateMovie, deleteMovie, uploadPoster } from '@/api/movie'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, StarFilled, Delete, VideoCameraFilled, ArrowLeft, ArrowRight } from '@element-plus/icons-vue'

const movies = ref([]); const loading = ref(false); const searchText = ref(''); const dialogVisible = ref(false)
const isEdit = ref(false); const editingId = ref(null); const submitting = ref(false); const uploading = ref(false)
const formRef = ref(null); const currentPage = ref(1); const pageSize = ref(20); const total = ref(0)
const activeFilter = ref('all'); const haloRefs = ref({})

const genreOptions = ['动作','喜剧','爱情','科幻','恐怖','动画','剧情','悬疑','战争','纪录片','奇幻','犯罪','冒险']
const filters = [{label:'全部',value:'all'},{label:'上映中',value:'showing'},{label:'即将上映',value:'upcoming'}]
const defaultPoster = 'data:image/svg+xml,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" width="80" height="106"><rect fill="#14141F" width="80" height="106"/><text fill="#5C5A6A" font-family="Arial" font-size="8" text-anchor="middle" x="40" y="56">暂无海报</text></svg>')
function onImgError(e) { e.target.src = defaultPoster }

const form = reactive({ name:'', genre:[], duration:90, price:0, director:'', actors:'', country:'', language:'', releaseDate:'', rating:0, poster:'', description:'', status:1 })
const rules = { name:[{required:true,message:'请输入影片名称',trigger:'blur'}], genre:[{type:'array',required:true,message:'请选择类型',trigger:'change'}], duration:[{required:true,message:'请输入时长'}], price:[{required:true,message:'请输入票价'}] }

const hotCount = computed(() => movies.value.filter(m => m.isHot).length)
const totalPages = computed(() => Math.ceil(total.value / pageSize.value))

const filteredMovies = computed(() => {
  let list = movies.value
  if (activeFilter.value === 'showing') list = list.filter(m => m.status === 1)
  else if (activeFilter.value === 'upcoming') list = list.filter(m => m.status === 2)
  return list
})

const pageList = computed(() => {
  const pages = []; const tp = totalPages.value; const cp = currentPage.value
  if (tp <= 7) { for (let i = 1; i <= tp; i++) pages.push(i) }
  else {
    pages.push(1)
    if (cp > 3) pages.push('...')
    for (let i = Math.max(2, cp - 1); i <= Math.min(tp - 1, cp + 1); i++) pages.push(i)
    if (cp < tp - 2) pages.push('...')
    pages.push(tp)
  }
  return pages
})

function statusLabel(s) { const m={0:'已下架',1:'上映中',2:'即将上映'}; return m[s]??'--' }
function statusClass(s) { const m={0:'off',1:'on',2:'soon'}; return m[s]??'' }

function setHaloRef(id, el) { if (el) haloRefs.value[id] = el }
function onCardHover(e, id) {
  const el = haloRefs.value[id]; if (!el) return
  const rect = el.getBoundingClientRect()
  const x = e.clientX - rect.left; const y = e.clientY - rect.top
  el.style.background = `radial-gradient(circle at ${x}px ${y}px, rgba(255,198,124,0.08), transparent 70%)`
}
function clearHalo(id) { const el = haloRefs.value[id]; if (el) el.style.background = '' }

function resetForm() { Object.assign(form, { name:'',genre:[],duration:90,price:0,director:'',actors:'',country:'',language:'',releaseDate:'',rating:0,poster:'',description:'',status:1 }) }
function openAdd() { isEdit.value=false; editingId.value=null; resetForm(); dialogVisible.value=true }
function openEdit(row) {
  isEdit.value=true; editingId.value=row.id
  form.name=row.name; form.genre=row.genre?row.genre.split(',').filter(g=>g.trim()):[]; form.duration=row.duration; form.price=row.price
  form.director=row.director||''; form.actors=row.actors||''; form.country=row.country||''; form.language=row.language||''
  form.releaseDate=row.releaseDate; form.rating=row.rating||0; form.poster=row.poster||''; form.description=row.description||''; form.status=row.status
  dialogVisible.value=true
}

async function handleSubmit() {
  if(!formRef.value) return; const v=await formRef.value.validate().catch(()=>false); if(!v) return
  submitting.value=true
  try {
    const p={ name:form.name, genre:Array.isArray(form.genre)?form.genre.join(','):form.genre, duration:form.duration, price:form.price, director:form.director, actors:form.actors, country:form.country, language:form.language, releaseDate:form.releaseDate, rating:form.rating, poster:form.poster, description:form.description, status:form.status }
    isEdit.value ? await updateMovie({id:editingId.value,...p}) : await addMovie(p)
    ElMessage.success(isEdit.value?'已更新':'已添加'); dialogVisible.value=false; fetchMovies()
  } catch{} finally{ submitting.value=false }
}

async function handleDelete(row) {
  try{ await ElMessageBox.confirm(`确定删除"${row.name}"？`,'确认',{type:'warning'}) } catch{ return }
  try{ await deleteMovie(row.id); ElMessage.success('已删除'); fetchMovies() } catch{}
}

async function toggleHot(row) {
  try{ const v=row.isHot?0:1; await updateMovie({id:row.id,isHot:v}); row.isHot=v; ElMessage.success(v?'已设为热映':'已取消热映') } catch{}
}

function beforePosterUpload(f) {
  if(!f.type.startsWith('image/')){ ElMessage.error('只能上传图片'); return false }
  if(f.size/1024/1024>10){ ElMessage.error('不超过10MB'); return false }
  return true
}

async function handlePosterUpload(o) {
  uploading.value=true
  try{ const r=await uploadPoster(o.file); if(r.data?.url){ form.poster=r.data.url; ElMessage.success('上传成功') } } catch{} finally{ uploading.value=false }
}

async function fetchMovies() {
  loading.value=true
  try{
    const kw=searchText.value?.trim()
    const res=kw ? await searchMovies(kw) : await getMovieList({page:currentPage.value,size:pageSize.value})
    movies.value=res.data?.records||res.data||[]; total.value=res.data?.total||movies.value.length
  } catch{ movies.value=[]; total.value=0 } finally{ loading.value=false }
}

function handlePageChange(p){ currentPage.value=p; fetchMovies() }

let t=null
watch(searchText,()=>{ clearTimeout(t); t=setTimeout(()=>{ currentPage.value=1; fetchMovies() },300) })
watch(activeFilter,()=>{ currentPage.value=1 })
onMounted(fetchMovies)
</script>

<style scoped>
.admin-layout { min-height:100vh; background:#0A0A10; position:relative; overflow:hidden; }
.movie-atmosphere { position:fixed; inset:0; pointer-events:none; z-index:0; }
.movie-atmosphere::before { content:''; position:absolute; top:-10%; left:-10%; width:40%; height:40%; background:radial-gradient(circle, rgba(232,168,80,0.015), transparent 70%); border-radius:50%; }
.movie-atmosphere::after { content:''; position:absolute; bottom:-10%; right:-10%; width:40%; height:40%; background:radial-gradient(circle, rgba(45,207,138,0.01), transparent 70%); border-radius:50%; }

.movie-page { max-width:1440px; margin:0 auto; padding:32px; position:relative; z-index:1; }

/* Action Bar */
.mv-bar { display:flex; justify-content:space-between; align-items:flex-end; margin-bottom:32px; flex-wrap:wrap; gap:16px; }
.mv-title { font-size:28px; font-weight:600; color:var(--text-primary); }
.mv-sub { font-size:13px; color:var(--text-secondary); margin-top:4px; }
.mv-actions { display:flex; gap:12px; align-items:center; }
.mv-filters { display:flex; background:#1f1f24; border-radius:24px; padding:3px; border:1px solid rgba(255,255,255,0.06); }
.mv-fbtn { padding:6px 16px; border-radius:20px; border:none; background:transparent; color:var(--text-secondary); font-size:12px; font-weight:500; cursor:pointer; transition:all .2s; }
.mv-fbtn.active { background:var(--color-primary); color:#1A1814; box-shadow:0 2px 8px rgba(0,0,0,0.3); }
.mv-fbtn:hover:not(.active) { color:var(--text-primary); }
.mv-add-btn { display:flex; align-items:center; gap:6px; padding:8px 22px; border:none; border-radius:24px; background:linear-gradient(135deg, #E8A850, #C88A30); color:#1A1814; font-size:13px; font-weight:700; cursor:pointer; transition:all .2s; box-shadow:0 4px 12px rgba(232,168,80,0.2); }
.mv-add-btn:hover { transform:scale(1.04); }
.mv-add-btn:active { transform:scale(0.97); }

/* Grid */
.mv-grid { display:grid; grid-template-columns:repeat(auto-fill, minmax(220px, 1fr)); gap:24px; min-height:300px; position:relative; z-index:1; }

/* Card */
.mv-card { background:#14141F; border-radius:12px; overflow:hidden; border:1px solid rgba(255,255,255,0.05); cursor:pointer; transition:all .4s; position:relative; }
.mv-card:hover { transform:translateY(-6px); border-color:rgba(232,168,80,0.4); box-shadow:0 0 30px rgba(232,168,80,0.06); }
.mv-poster { aspect-ratio:2/3; position:relative; overflow:hidden; background:#1b1b20; }
.mv-poster img { width:100%; height:100%; object-fit:cover; transition:transform .6s; }
.mv-card:hover .mv-poster img { transform:scale(1.1); }
.mv-poster-gradient { position:absolute; inset:0; background:linear-gradient(to top, rgba(10,10,16,0.9), transparent 60%); opacity:0.6; }
.mv-status { position:absolute; top:10px; right:10px; padding:3px 10px; border-radius:12px; font-size:10px; font-weight:700; background:rgba(0,0,0,0.45); letter-spacing:0.5px; }
.mv-status.on { background:rgba(26,107,76,0.15); color:#2DCF8A; border:1px solid rgba(45,207,138,0.25); }
.mv-status.soon { background:rgba(91,141,239,0.1); color:#5B8DEF; border:1px solid rgba(91,141,239,0.25); }
.mv-status.off { background:rgba(92,90,106,0.15); color:#5C5A6A; border:1px solid rgba(92,90,106,0.25); }

/* Overlay */
.mv-overlay { position:absolute; inset:0; display:flex; align-items:center; justify-content:center; gap:10px; background:rgba(10,10,16,0.65); opacity:0; transition:opacity .25s; }
.mv-card:hover .mv-overlay { opacity:1; }
.mv-obtn { width:40px; height:40px; border-radius:50%; border:1px solid rgba(255,255,255,0.2); background:rgba(255,255,255,0.08); color:#fff; cursor:pointer; display:flex; align-items:center; justify-content:center; transition:all .15s; }
.mv-obtn:hover { background:var(--color-primary); color:#1A1814; border-color:var(--color-primary); transform:scale(1.1); }
.mv-obtn-del:hover { background:var(--color-danger); border-color:var(--color-danger); color:#fff; }

/* Info */
.mv-info { padding:14px 16px; position:relative; }
.mv-name { font-size:16px; font-weight:600; color:var(--text-primary); white-space:nowrap; overflow:hidden; text-overflow:ellipsis; margin-bottom:6px; }
.mv-card:hover .mv-name { color:var(--color-primary); }
.mv-meta { display:flex; align-items:center; gap:6px; margin-bottom:10px; font-size:11px; color:var(--text-tertiary); }
.mv-date { font-family:monospace; }
.mv-dot { width:4px; height:4px; border-radius:50%; background:var(--text-tertiary); flex-shrink:0; }
.mv-foot { display:flex; align-items:center; justify-content:space-between; }
.mv-rating { display:flex; align-items:center; gap:4px; color:var(--color-primary); font-size:13px; font-weight:700; }
.mv-rating .el-icon { color:var(--color-primary); }
.mv-no-rating { font-size:11px; color:var(--text-tertiary); font-style:italic; }
.mv-tag { font-size:10px; color:var(--text-tertiary); padding:2px 8px; border:1px solid rgba(255,255,255,0.08); border-radius:10px; }

/* Add Card */
.mv-card-add { display:flex; flex-direction:column; align-items:center; justify-content:center; min-height:360px; border:2px dashed rgba(255,255,255,0.06); background:rgba(20,20,31,0.3); }
.mv-card-add:hover { border-color:rgba(232,168,80,0.3); }
.mv-add-icon { width:60px; height:60px; border-radius:50%; background:rgba(255,255,255,0.04); display:flex; align-items:center; justify-content:center; margin-bottom:14px; color:var(--color-primary); transition:transform .2s; }
.mv-card-add:hover .mv-add-icon { transform:scale(1.1); }
.mv-add-title { font-size:14px; font-weight:600; color:var(--text-secondary); }
.mv-add-desc { font-size:10px; color:var(--text-tertiary); margin-top:4px; }

/* Pagination */
.mv-pager { display:flex; align-items:center; justify-content:center; gap:8px; margin-top:40px; }
.mv-pg-btn { width:36px; height:36px; border-radius:8px; border:1px solid rgba(255,255,255,0.08); background:transparent; color:var(--text-secondary); cursor:pointer; display:flex; align-items:center; justify-content:center; transition:all .15s; }
.mv-pg-btn:hover:not(:disabled) { color:var(--color-primary); border-color:var(--color-primary); }
.mv-pg-btn:disabled { opacity:0.3; cursor:not-allowed; }
.mv-pg-nums { display:flex; gap:6px; }
.mv-pg-num { width:36px; height:36px; border-radius:8px; border:none; background:transparent; color:var(--text-secondary); font-size:13px; font-weight:500; cursor:pointer; transition:all .15s; }
.mv-pg-num:hover:not(.dot):not(.active) { background:rgba(255,255,255,0.04); }
.mv-pg-num.active { background:var(--color-primary); color:#1A1814; font-weight:700; }
.mv-pg-num.dot { cursor:default; }

/* ===== Light Mode ===== */
[data-theme='light'] .admin-layout { background:#fff8f4; }
[data-theme='light'] .movie-atmosphere::before { background:radial-gradient(circle, rgba(132,84,0,0.04), transparent 70%); }
[data-theme='light'] .movie-atmosphere::after { background:radial-gradient(circle, rgba(26,107,76,0.03), transparent 70%); }
[data-theme='light'] .mv-filters { background:#fff; border-color:rgba(0,0,0,0.08); }
[data-theme='light'] .mv-fbtn.active { color:#fff; }
[data-theme='light'] .mv-card { background:#fff; border-color:rgba(0,0,0,0.06); }
[data-theme='light'] .mv-card:hover { border-color:rgba(132,84,0,0.3); box-shadow:0 4px 20px rgba(0,0,0,0.08); }
[data-theme='light'] .mv-poster { background:#f8ece1; }
[data-theme='light'] .mv-poster-gradient { background:linear-gradient(to top, rgba(0,0,0,0.25), transparent 50%); }
[data-theme='light'] .mv-overlay { background:rgba(255,255,255,0.3); }
[data-theme='light'] .mv-obtn { border-color:rgba(0,0,0,0.15); background:rgba(255,255,255,0.7); color:#201b14; }
[data-theme='light'] .mv-obtn:hover { background:#845400; color:#fff; border-color:#845400; }
[data-theme='light'] .mv-obtn-del:hover { background:#ba1a1a; color:#fff; border-color:#ba1a1a; }
[data-theme='light'] .mv-tag { border-color:rgba(0,0,0,0.08); }
[data-theme='light'] .mv-card-add { background:rgba(0,0,0,0.02); border-color:rgba(0,0,0,0.08); }
[data-theme='light'] .mv-card-add:hover { border-color:rgba(132,84,0,0.25); }
[data-theme='light'] .mv-add-icon { background:rgba(0,0,0,0.04); }
[data-theme='light'] .mv-pg-btn { border-color:rgba(0,0,0,0.1); }
[data-theme='light'] .mv-no-rating { color:#9B9590; }
</style>
