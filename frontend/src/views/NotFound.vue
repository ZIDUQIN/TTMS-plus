<template>
  <div class="not-found-page">
    <div class="not-found-content">
      <h1 class="not-found-code">404</h1>
      <p class="not-found-text">页面未找到</p>
      <p class="not-found-desc">您访问的页面不存在或已被移除</p>
      <div class="not-found-actions">
        <el-button type="primary" @click="$router.push('/home')">返回首页</el-button>
        <el-button @click="$router.go(-1)">返回上一页</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
// 404 NotFound page - shown when no route matches
</script>

<style scoped>
.not-found-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-secondary, #f5f7fa);
}
.not-found-content {
  text-align: center;
}
.not-found-code {
  font-size: 120px;
  font-weight: 800;
  color: var(--brand-primary, #409eff);
  line-height: 1;
  margin: 0;
}
.not-found-text {
  font-size: 24px;
  color: var(--text-primary, #303133);
  margin: 16px 0 8px;
}
.not-found-desc {
  font-size: 14px;
  color: var(--text-muted, #909399);
  margin-bottom: 32px;
}
.not-found-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}
</style>
