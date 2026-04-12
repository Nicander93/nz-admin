<template>
  <el-aside width="220px" class="border-r border-gray-200">
    <div class="h-14 flex items-center justify-center font-bold text-lg border-b border-gray-200">
      NZ Admin
    </div>
    <el-menu :default-active="route.path" router>
      <el-sub-menu index="/system">
        <template #title>
          <el-icon><Setting /></el-icon>
          <span>系统管理</span>
        </template>
        <el-menu-item index="/system/user">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/system/role">
          <el-icon><UserFilled /></el-icon>
          <span>角色管理</span>
        </el-menu-item>
        <el-menu-item index="/system/dept">
          <el-icon><OfficeBuilding /></el-icon>
          <span>部门管理</span>
        </el-menu-item>
        <el-menu-item index="/system/menu">
          <el-icon><Menu /></el-icon>
          <span>菜单管理</span>
        </el-menu-item>
        <el-menu-item index="/system/dict">
          <el-icon><Collection /></el-icon>
          <span>字典管理</span>
        </el-menu-item>
      </el-sub-menu>
    </el-menu>
  </el-aside>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'
import { User, UserFilled, OfficeBuilding, Setting, Menu, Collection } from '@element-plus/icons-vue'

const route = useRoute()
</script>
