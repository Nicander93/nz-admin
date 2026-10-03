<template>
  <el-alert v-if="error" :title="error" type="warning" :closable="false" />
  <div v-else-if="model" class="model-editor">
    <el-form inline label-width="80px">
      <el-form-item label="流程编码"
        ><el-input v-model="model.flowCode" @change="commit"
      /></el-form-item>
      <el-form-item label="流程名称"
        ><el-input v-model="model.flowName" @change="commit"
      /></el-form-item>
    </el-form>
    <svg
      :viewBox="`0 0 800 ${height}`"
      role="img"
      aria-label="流程节点和连线预览"
    >
      <defs>
        <marker
          id="flow-arrow"
          markerWidth="8"
          markerHeight="8"
          refX="7"
          refY="4"
          orient="auto"
        >
          <path d="M0,0 L8,4 L0,8 Z" fill="currentColor" />
        </marker>
      </defs>
      <g v-for="(node, i) in model.nodeList" :key="node.nodeCode + i">
        <path
          v-for="(edge, j) in node.skipList"
          :key="j"
          :d="edgePath(i, edge.nextNodeCode)"
          fill="none"
          stroke="currentColor"
          stroke-width="1.5"
          marker-end="url(#flow-arrow)"
        />
      </g>
      <g
        v-for="(node, i) in model.nodeList"
        :key="i"
        role="button"
        tabindex="0"
        :aria-label="`编辑节点 ${node.nodeName}`"
        @click="selected = i"
        @keydown.enter="selected = i"
        @keydown.space.prevent="selected = i"
      >
        <rect
          :x="position(i).x"
          :y="position(i).y"
          width="160"
          height="60"
          rx="8"
          :fill="
            selected === i
              ? 'var(--el-color-primary-light-9)'
              : 'var(--el-fill-color-blank)'
          "
          stroke="var(--el-color-primary)"
        />
        <text
          :x="position(i).x + 80"
          :y="position(i).y + 24"
          text-anchor="middle"
          fill="var(--el-text-color-primary)"
        >
          {{ node.nodeName.slice(0, 12) }}
        </text>
        <text
          :x="position(i).x + 80"
          :y="position(i).y + 45"
          text-anchor="middle"
          fill="var(--el-text-color-secondary)"
          font-size="12"
        >
          {{ types[node.nodeType] }}
        </text>
      </g>
    </svg>
    <el-button @click="addNode">添加节点</el-button>
    <el-form v-if="current" label-width="100px" class="node-form">
      <el-form-item label="节点编码"
        ><el-input v-model="nodeCodeDraft" @change="rename"
      /></el-form-item>
      <el-form-item label="节点名称"
        ><el-input v-model="current.nodeName" @change="commit"
      /></el-form-item>
      <el-form-item label="节点类型"
        ><el-select v-model="current.nodeType" @change="commit"
          ><el-option
            v-for="(name, i) in types"
            :key="i"
            :label="name"
            :value="i" /></el-select
      ></el-form-item>
      <el-form-item v-if="current.nodeType === 1" label="办理人"
        ><el-input
          v-model="current.permissionFlag"
          placeholder="user:1 或 role:manager，多个用逗号分隔"
          @change="commit"
      /></el-form-item>
      <el-form-item v-if="current.nodeType === 1" label="通过比例"
        ><el-input
          v-model="current.nodeRatio"
          placeholder="0 为或签，100 为会签，中间值为票签比例"
          @change="commit"
      /></el-form-item>
      <el-form-item label="连线">
        <div class="edges">
          <div v-for="(edge, i) in current.skipList" :key="i" class="edge">
            <el-select
              v-model="edge.nextNodeCode"
              aria-label="目标节点"
              @change="commit"
              ><el-option
                v-for="node in model.nodeList"
                :key="node.nodeCode"
                :label="node.nodeName"
                :value="node.nodeCode"
            /></el-select>
            <el-select
              v-model="edge.skipType"
              aria-label="流转类型"
              @change="commit"
              ><el-option label="通过" value="PASS" /><el-option
                label="退回"
                value="REJECT"
            /></el-select>
            <el-input
              v-model="edge.skipCondition"
              placeholder="可选条件，如 ge@@amount|100"
              @change="commit"
            />
            <el-button aria-label="删除连线" @click="removeEdge(i)"
              >删除</el-button
            >
          </div>
          <el-button @click="addEdge">添加连线</el-button>
        </div>
      </el-form-item>
      <el-form-item
        ><el-button type="danger" @click="removeNode"
          >删除节点</el-button
        ></el-form-item
      >
    </el-form>
  </div>
</template>
<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, ref, watch } from 'vue'

interface Edge {
  nextNodeCode: string
  skipType?: 'PASS' | 'REJECT'
  skipCondition?: string
}
interface Node {
  nodeCode: string
  nodeName: string
  nodeType: number
  permissionFlag?: string
  nodeRatio?: string
  skipList: Edge[]
}
interface Model {
  flowCode: string
  flowName: string
  nodeList: Node[]
}
const props = defineProps<{ modelValue: string }>()
const emit = defineEmits<{ 'update:modelValue': [value: string] }>()
const model = ref<Model>()
const error = ref('')
const selected = ref(0)
const types = ['开始', '审批', '结束', '互斥网关', '并行网关', '包容网关']
watch(
  () => props.modelValue,
  (value) => {
    try {
      const parsed = JSON.parse(value) as Model
      if (
        !parsed ||
        !Array.isArray(parsed.nodeList) ||
        parsed.nodeList.some(
          (n) =>
            !n ||
            typeof n.nodeCode !== 'string' ||
            typeof n.nodeName !== 'string' ||
            !Number.isInteger(n.nodeType) ||
            n.nodeType < 0 ||
            n.nodeType > 5 ||
            (n.skipList !== undefined &&
              (!Array.isArray(n.skipList) ||
                n.skipList.some(
                  (edge) => !edge || typeof edge.nextNodeCode !== 'string',
                ))),
        )
      )
        throw new Error('请先提供包含 nodeList 的流程模型')
      parsed.nodeList.forEach((node) => {
        node.skipList ??= []
      })
      model.value = parsed
      error.value = ''
    } catch {
      error.value = '模型 JSON 格式不正确，请在 JSON 面板修正后继续编辑'
    }
  },
  { immediate: true },
)
const current = computed(() => model.value?.nodeList[selected.value])
const nodeCodeDraft = ref('')
watch(
  () => current.value?.nodeCode,
  (code) => {
    nodeCodeDraft.value = code ?? ''
  },
  { immediate: true },
)
const height = computed(() =>
  Math.max(100, Math.ceil((model.value?.nodeList.length ?? 0) / 4) * 110),
)
function position(i: number) {
  return { x: 15 + (i % 4) * 200, y: 20 + Math.floor(i / 4) * 110 }
}
function edgePath(i: number, code: string) {
  const target =
    model.value?.nodeList.findIndex((n) => n.nodeCode === code) ?? -1
  if (target < 0) return ''
  const a = position(i)
  const b = position(target)
  return `M${a.x + 160},${a.y + 30} C${a.x + 185},${a.y + 30} ${b.x - 25},${b.y + 30} ${b.x},${b.y + 30}`
}
function commit() {
  emit('update:modelValue', JSON.stringify(model.value, null, 2))
}
function addNode() {
  if (!model.value) return
  const code = `node_${crypto.randomUUID().slice(0, 8)}`
  model.value.nodeList.push({
    nodeCode: code,
    nodeName: '新节点',
    nodeType: 1,
    skipList: [],
  })
  selected.value = model.value.nodeList.length - 1
  commit()
}
function rename(code: string) {
  if (!current.value || !model.value) return
  if (
    !/^[A-Za-z][A-Za-z0-9_-]{0,63}$/.test(code) ||
    model.value.nodeList.some((n) => n !== current.value && n.nodeCode === code)
  ) {
    nodeCodeDraft.value = current.value.nodeCode
    ElMessage.error('节点编码须合法且唯一')
    return
  }
  const previous = current.value.nodeCode
  for (const node of model.value.nodeList)
    for (const edge of node.skipList)
      if (edge.nextNodeCode === previous) edge.nextNodeCode = code
  current.value.nodeCode = code
  commit()
}
function removeNode() {
  if (!model.value || !current.value) return
  const code = current.value.nodeCode
  model.value.nodeList.splice(selected.value, 1)
  model.value.nodeList.forEach((node) => {
    node.skipList = node.skipList.filter((edge) => edge.nextNodeCode !== code)
  })
  selected.value = 0
  commit()
}
function addEdge() {
  if (current.value) {
    current.value.skipList.push({ nextNodeCode: '', skipType: 'PASS' })
    commit()
  }
}
function removeEdge(i: number) {
  current.value?.skipList.splice(i, 1)
  commit()
}
</script>
<style scoped>
.model-editor svg {
  width: 100%;
  max-height: 480px;
  color: var(--el-color-primary);
}
.node-form {
  margin-top: 16px;
  max-width: 900px;
}
.edges {
  display: grid;
  gap: 8px;
  width: 100%;
}
.edge {
  display: flex;
  gap: 8px;
}
.edge .el-select {
  min-width: 120px;
}
@media (max-width: 800px) {
  .edge {
    flex-wrap: wrap;
  }
}
</style>
