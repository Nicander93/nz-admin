<template>
  <iframe
    ref="frame"
    :src="src"
    title="Warm-Flow 官方流程设计器"
    class="workflow-designer"
  />
</template>
<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

const props = defineProps<{ definitionId: string; readonly?: boolean }>()
const emit = defineEmits<{ saved: [] }>()
const frame = ref<HTMLIFrameElement>()
const src = computed(
  () =>
    `/api/workflow/designer/ui/index.html?id=${encodeURIComponent(props.definitionId)}&onlyDesignShow=true&disabled=${props.readonly ? 'true' : 'false'}`,
)
// 官方资源与宿主同源，从存储读取请求头；URL 只传定义标识。
localStorage.setItem('Warm-TokenName', 'Authorization')
localStorage.setItem('Warm-Authorization', localStorage.getItem('token') ?? '')
function receive(event: MessageEvent) {
  if (
    event.origin !== location.origin ||
    event.source !== frame.value?.contentWindow
  )
    return
  if (event.data?.method === 'close') emit('saved')
  if (event.data?.method === 'getTheme')
    frame.value?.contentWindow?.postMessage(
      {
        method: 'setTheme',
        theme: document.documentElement.classList.contains('dark')
          ? 'dark'
          : 'light',
      },
      location.origin,
    )
}
onMounted(() => window.addEventListener('message', receive))
onBeforeUnmount(() => {
  window.removeEventListener('message', receive)
  localStorage.removeItem('Warm-Authorization')
})
</script>
<style scoped>
.workflow-designer {
  width: 100%;
  height: calc(100vh - 120px);
  border: 0;
}
</style>
