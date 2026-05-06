<template>
  <el-container class="app-shell app-layout">
    <Sidebar />
    <el-container class="app-workspace">
      <Header />
      <el-main class="app-main app-content">
        <div class="app-page">
          <router-view />
        </div>
      </el-main>
      <el-footer class="app-footer">
        <span>NZ Admin Console</span>
        <span>稳定运行中</span>
      </el-footer>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import Sidebar from './Sidebar.vue'
import Header from './Header.vue'
</script>
