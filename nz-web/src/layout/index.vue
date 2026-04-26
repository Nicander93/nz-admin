<template>
  <el-container class="app-shell app-layout">
    <Sidebar />
    <el-container class="min-w-0">
      <Header />
      <el-main class="app-main">
        <div class="app-page">
          <router-view />
        </div>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import Sidebar from './Sidebar.vue'
import Header from './Header.vue'
</script>
