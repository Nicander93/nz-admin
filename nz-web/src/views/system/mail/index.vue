<template>
  <el-card class="mail-card">
    <template #header><span>SMTP 邮件测试</span></template>
    <el-alert type="info" :closable="false" class="mb-4">
      发送前请在服务端配置 nz.mail 和 spring.mail；邮件能力默认关闭。
    </el-alert>
    <el-form :model="form" label-width="100px">
      <el-form-item label="收件人"><el-input v-model="form.to" placeholder="user@example.com" /></el-form-item>
      <el-form-item label="主题"><el-input v-model="form.subject" /></el-form-item>
      <el-form-item label="HTML"><el-switch v-model="form.html" /></el-form-item>
      <el-form-item label="正文"><el-input v-model="form.content" type="textarea" :rows="8" /></el-form-item>
      <el-form-item>
        <el-button v-permission="'system:mail:test'" type="primary" :loading="loading" @click="send">发送测试邮件</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup lang="ts">
import { useMailTest } from './hooks'

const { form, loading, send } = useMailTest()
</script>

<style scoped>
.mail-card { max-width: 760px; }
</style>