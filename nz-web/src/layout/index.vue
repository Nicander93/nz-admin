<template>
  <el-container class="h-full">
    <Sidebar />
    <el-container>
      <Header />
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import Sidebar from './Sidebar.vue'
import Header from './Header.vue'
</script>
