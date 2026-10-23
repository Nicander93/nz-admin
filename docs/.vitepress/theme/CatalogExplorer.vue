<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { withBase } from 'vitepress'
import catalog from '../../data/source-catalog.json'

type Module = (typeof catalog.modules)[number]
type View = 'modules' | 'capabilities' | 'extensions'
const query = ref('')
const layer = ref('all')
const view = ref<View>('modules')
const selectedId = ref('nz-app')
const details = ref<HTMLElement>()
let hydrated = false

const layers: Record<string, string> = {
  all: '全部架构层',
  application: '应用装配',
  business: '业务模块',
  framework: '框架 Starter',
  common: '公共对象与协议',
  aggregation: '聚合工程',
}
const views: Array<{ key: View; label: string }> = [
  { key: 'modules', label: '模块目录' },
  { key: 'capabilities', label: '能力与边界' },
  { key: 'extensions', label: '扩展协议' },
]
const statuses: Record<string, string> = {
  available: '已实现',
  conditional: '按需配置',
  planned: '后续规划',
}
const byId = new Map(catalog.modules.map((item) => [item.id, item]))
const matches = (value: unknown) =>
  JSON.stringify(value)
    .toLocaleLowerCase()
    .includes(query.value.trim().toLocaleLowerCase())
const modules = computed(() =>
  catalog.modules.filter(
    (item) =>
      (layer.value === 'all' || item.layer === layer.value) && matches(item),
  ),
)
const capabilities = computed(() => catalog.capabilities.filter(matches))
const extensions = computed(() => catalog.extensions.filter(matches))
const selected = computed(
  () =>
    modules.value.find((item) => item.id === selectedId.value) ??
    modules.value[0],
)
const count = computed(() =>
  view.value === 'modules'
    ? modules.value.length
    : view.value === 'capabilities'
      ? capabilities.value.length
      : extensions.value.length,
)
const sourceLink = (path: string) =>
  `${catalog.repository}/blob/master/${path.split('/').map(encodeURIComponent).join('/')}`
const moduleName = (id: string) => byId.get(id)?.title ?? id

async function chooseModule(id: string) {
  selectedId.value = id
  await nextTick()
  if (window.matchMedia('(max-width: 740px)').matches)
    details.value?.scrollIntoView({ block: 'start' })
}

function selectModule(id: string) {
  query.value = ''
  layer.value = 'all'
  view.value = 'modules'
  return chooseModule(id)
}

function searchableModule(item: Module) {
  return `${item.title} ${item.id}`
}

onMounted(() => {
  const params = new URL(window.location.href).searchParams
  query.value = params.get('q') ?? ''
  const selectedView = params.get('view')
  if (views.some((item) => item.key === selectedView))
    view.value = selectedView as View
  const selectedLayer = params.get('layer')
  if (selectedLayer && selectedLayer in layers) layer.value = selectedLayer
  const module = params.get('module')
  if (module && byId.has(module)) selectedId.value = module
  hydrated = true
})

watch([query, layer, view, selectedId], () => {
  if (!hydrated) return
  const url = new URL(window.location.href)
  for (const [key, value] of Object.entries({
    q: query.value.trim(),
    view: view.value === 'modules' ? '' : view.value,
    layer: layer.value === 'all' ? '' : layer.value,
    module: selectedId.value === 'nz-app' ? '' : selectedId.value,
  })) {
    if (value) url.searchParams.set(key, value)
    else url.searchParams.delete(key)
  }
  window.history.replaceState(window.history.state, '', url)
})
</script>

<template>
  <section class="catalog" aria-label="源码能力浏览器">
    <p class="catalog__summary">
      <strong>{{ catalog.modules.length }}</strong> 个 Maven 工程 ·
      <strong>{{
        catalog.modules.filter((item) => item.layer === 'framework').length
      }}</strong>
      个 Starter · <strong>{{ catalog.extensions.length }}</strong> 个扩展协议
    </p>
    <div class="catalog__views" role="group" aria-label="目录视图">
      <button
        v-for="item in views"
        :key="item.key"
        type="button"
        :aria-pressed="view === item.key"
        @click="view = item.key"
      >
        {{ item.label }}
      </button>
    </div>
    <div class="catalog__filters">
      <label class="catalog__query"
        >搜索模块、能力或接口
        <input
          v-model="query"
          type="search"
          placeholder="例如 tenant、工作流、SmsProvider"
        />
      </label>
      <label v-if="view === 'modules'"
        >架构层
        <select v-model="layer">
          <option v-for="(label, key) in layers" :key="key" :value="key">
            {{ label }}
          </option>
        </select>
      </label>
    </div>
    <p class="catalog__count" role="status">找到 {{ count }} 项</p>
    <p v-if="!count" class="catalog__empty">
      没有匹配结果。试试模块名、接口名或清空筛选。
    </p>

    <div v-else-if="view === 'modules'" class="catalog__workspace">
      <nav class="catalog__results" aria-label="模块结果">
        <button
          v-for="item in modules"
          :key="item.id"
          type="button"
          :aria-label="searchableModule(item)"
          :aria-pressed="selected?.id === item.id"
          @click="chooseModule(item.id)"
        >
          <span>{{ item.title }}</span
          ><code>{{ item.id }}</code>
          <small>{{ layers[item.layer] }}</small>
        </button>
      </nav>
      <article
        v-if="selected"
        ref="details"
        class="catalog__details"
        aria-label="模块详情"
      >
        <p class="catalog__eyebrow">{{ layers[selected.layer] }}</p>
        <h2>{{ selected.title }}</h2>
        <code>{{ selected.id }}</code>
        <p>{{ selected.description }}</p>
        <div class="catalog__links">
          <a :href="withBase(selected.guide)">阅读接入指南</a>
          <a :href="sourceLink(selected.pom)" target="_blank" rel="noreferrer"
            >查看 POM ↗</a
          >
        </div>
        <p v-if="selected.manifest" class="catalog__note">
          清单默认{{
            selected.manifest.defaultEnabled ? '启用' : '关闭'
          }}；实际状态以进程配置为准。
        </p>
        <h3>依赖关系</h3>
        <div class="catalog__dependency">
          <p class="catalog__eyebrow">直接使用本模块</p>
          <div class="catalog__chips">
            <button
              v-for="id in selected.dependents"
              :key="id"
              type="button"
              @click="selectModule(id)"
            >
              {{ id }}</button
            ><span v-if="!selected.dependents.length">无仓库内直接依赖方</span>
          </div>
          <p class="catalog__direction">↓ {{ selected.id }} ↓</p>
          <p class="catalog__eyebrow">本模块的显式生产依赖</p>
          <div class="catalog__chips">
            <button
              v-for="id in selected.dependencies"
              :key="id"
              type="button"
              :title="moduleName(id)"
              @click="selectModule(id)"
            >
              {{ id }}</button
            ><span v-if="!selected.dependencies.length"
              >无仓库内部生产依赖</span
            >
          </div>
        </div>
        <p v-if="selected.testDependencies.length" class="catalog__note">
          测试依赖：{{
            selected.testDependencies.join('、')
          }}（不计入生产关系）。
        </p>
        <p v-if="selected.providedDependencies.length" class="catalog__note">
          非运行期声明（provided/system）：{{
            selected.providedDependencies.join('、')
          }}。
        </p>
        <template v-if="selected.manifest && selected.frontend">
          <h3>前后端装配</h3>
          <ul>
            <li>
              <a
                :href="sourceLink(selected.manifest.source)"
                target="_blank"
                rel="noreferrer"
                >业务清单</a
              >：{{ selected.manifest.code }}
            </li>
            <li>
              <a
                :href="sourceLink(selected.frontend.source)"
                target="_blank"
                rel="noreferrer"
                >前端清单</a
              >：组件前缀 {{ selected.frontend.componentPrefix }}
            </li>
            <li>
              必需模块：{{
                selected.manifest.requiredModules.join('、') || '无'
              }}
            </li>
            <li>
              声明 Starter：{{
                selected.manifest.requiredStarters.join('、') || '未单独声明'
              }}
            </li>
          </ul>
        </template>
        <template v-if="selected.autoConfigurations.length">
          <h3>自动配置入口</h3>
          <ul>
            <li v-for="item in selected.autoConfigurations" :key="item.source">
              <a
                :href="sourceLink(item.source)"
                target="_blank"
                rel="noreferrer"
                >{{ item.name.split('.').at(-1) }}</a
              >
            </li>
          </ul>
        </template>
        <template v-if="selected.contracts.length">
          <h3>公开接口</h3>
          <ul>
            <li v-for="item in selected.contracts" :key="item.source">
              <a
                :href="sourceLink(item.source)"
                target="_blank"
                rel="noreferrer"
                >{{ item.name }}</a
              >
            </li>
          </ul>
        </template>
        <template v-if="selected.tests.length">
          <h3>测试入口</h3>
          <p class="catalog__note">
            {{
              selected.tests.length
            }}
            个测试源文件，数量不代表本次执行或通过结果。
          </p>
          <ul>
            <li v-for="path in selected.tests.slice(0, 5)" :key="path">
              <a :href="sourceLink(path)" target="_blank" rel="noreferrer">{{
                path.split('/').at(-1)
              }}</a>
            </li>
          </ul>
        </template>
      </article>
    </div>

    <div v-else-if="view === 'capabilities'" class="catalog__entries">
      <article
        v-for="item in capabilities"
        :key="item.id"
        class="catalog__entry"
      >
        <p class="catalog__eyebrow">{{ statuses[item.status] }}</p>
        <h2>{{ item.title }}</h2>
        <p>{{ item.summary }}</p>
        <a :href="withBase(item.guide)">阅读能力与接入说明 →</a>
        <div v-if="item.modules.length" class="catalog__chips">
          <button
            v-for="id in item.modules"
            :key="id"
            type="button"
            @click="selectModule(id)"
          >
            {{ id }}
          </button>
        </div>
        <p v-if="item.sources.length" class="catalog__sources">
          <a
            v-for="path in item.sources"
            :key="path"
            :href="sourceLink(path)"
            target="_blank"
            rel="noreferrer"
            >源码：{{ path.split('/').at(-1) }} ↗</a
          >
        </p>
      </article>
    </div>
    <div v-else class="catalog__entries">
      <article
        v-for="item in extensions"
        :key="item.name"
        class="catalog__entry"
      >
        <p class="catalog__eyebrow">{{ item.module }}</p>
        <h2>{{ item.name }}</h2>
        <p>{{ item.purpose }}</p>
        <div class="catalog__links">
          <a :href="withBase(item.guide)">阅读接入指南</a
          ><a :href="sourceLink(item.source)" target="_blank" rel="noreferrer"
            >查看接口源码 ↗</a
          >
        </div>
        <button
          class="catalog__module-link"
          type="button"
          @click="selectModule(item.module)"
        >
          查看所在模块及依赖 →
        </button>
      </article>
    </div>
  </section>
</template>

<style scoped>
.catalog {
  margin: 28px 0;
}
.catalog__summary {
  color: var(--vp-c-text-2);
  font-size: 14px;
}
.catalog__summary strong {
  color: var(--vp-c-text-1);
}
.catalog__views {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 20px 0;
}
.catalog__views button {
  padding: 7px 14px;
  border: 1px solid var(--vp-c-divider);
  border-radius: 6px;
  font-size: 14px;
}
.catalog__views button[aria-pressed='true'] {
  border-color: var(--vp-c-brand-1);
  color: var(--vp-c-brand-1);
  background: var(--vp-c-brand-soft);
}
.catalog__filters {
  display: flex;
  align-items: end;
  gap: 16px;
}
.catalog__filters label {
  display: grid;
  gap: 6px;
  color: var(--vp-c-text-2);
  font-size: 13px;
}
.catalog__query {
  flex: 1;
  min-width: 0;
}
.catalog__filters input,
.catalog__filters select {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--vp-c-divider);
  border-radius: 6px;
  color: var(--vp-c-text-1);
  background: var(--vp-c-bg);
  font-size: 14px;
}
.catalog__count {
  color: var(--vp-c-text-2);
  font-size: 13px;
}
.catalog__workspace {
  display: grid;
  grid-template-columns: minmax(190px, 1fr) minmax(0, 2fr);
  border-top: 1px solid var(--vp-c-divider);
}
.catalog__results {
  max-height: 600px;
  overflow: auto;
  border-right: 1px solid var(--vp-c-divider);
  padding: 12px 12px 12px 0;
}
.catalog__results button {
  display: grid;
  gap: 3px;
  width: 100%;
  padding: 12px;
  text-align: left;
  border-left: 2px solid transparent;
  font-size: 14px;
}
.catalog__results button[aria-pressed='true'] {
  border-color: var(--vp-c-brand-1);
  background: var(--vp-c-brand-soft);
}
.catalog__results code {
  font-size: 11px;
  overflow-wrap: anywhere;
}
.catalog__results small {
  color: var(--vp-c-text-2);
  font-size: 11px;
}
.catalog__details {
  padding: 20px 0 20px 24px;
  min-width: 0;
  scroll-margin-top: 120px;
}
.catalog__details h2,
.catalog__entry h2 {
  margin: 0 0 8px;
  padding: 0;
  border: 0;
  font-size: 20px;
  overflow-wrap: anywhere;
}
.catalog__details h3 {
  margin-top: 28px;
  font-size: 15px;
}
.catalog__details p,
.catalog__entry p,
.catalog__details li {
  font-size: 14px;
}
.catalog__eyebrow {
  margin: 0 0 7px;
  color: var(--vp-c-text-2);
  font-size: 12px !important;
}
.catalog__links {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 20px;
  margin: 16px 0;
  font-size: 14px;
}
.catalog__note {
  color: var(--vp-c-text-2);
  font-size: 12px !important;
}
.catalog__dependency {
  padding: 14px 0;
  border-bottom: 1px solid var(--vp-c-divider);
}
.catalog__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
  font-size: 12px;
}
.catalog__chips button {
  padding: 2px 7px;
  border: 1px solid var(--vp-c-divider);
  border-radius: 4px;
  overflow-wrap: anywhere;
  text-align: left;
}
.catalog__chips button:hover,
.catalog__module-link:hover {
  color: var(--vp-c-brand-1);
}
.catalog__chips span {
  color: var(--vp-c-text-2);
}
.catalog__direction {
  margin: 14px 0;
  font-family: var(--vp-font-family-mono);
  overflow-wrap: anywhere;
  font-size: 12px !important;
}
.catalog__entries {
  border-top: 1px solid var(--vp-c-divider);
}
.catalog__entry {
  padding: 24px 0;
  border-bottom: 1px solid var(--vp-c-divider);
}
.catalog__sources {
  display: grid;
  gap: 4px;
  overflow-wrap: anywhere;
  font-size: 12px !important;
}
.catalog__module-link {
  color: var(--vp-c-brand-1);
  font-size: 13px;
}
.catalog__empty {
  padding: 24px 0;
  color: var(--vp-c-text-2);
}
.catalog button:focus-visible,
.catalog input:focus-visible,
.catalog select:focus-visible {
  outline: 2px solid var(--vp-c-brand-1);
  outline-offset: 3px;
}
@media (max-width: 740px) {
  .catalog__filters {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
  }
  .catalog__workspace {
    grid-template-columns: 1fr;
  }
  .catalog__results {
    max-height: 270px;
    border-right: 0;
    border-bottom: 1px solid var(--vp-c-divider);
    padding: 10px 0;
  }
  .catalog__details {
    padding: 20px 0;
  }
}
</style>
