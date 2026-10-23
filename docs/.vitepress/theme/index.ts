import type { Theme } from 'vitepress'
import { h } from 'vue'
import DefaultTheme from 'vitepress/theme'
import Breadcrumbs from './Breadcrumbs.vue'
import CatalogExplorer from './CatalogExplorer.vue'
import './style.css'

export default {
  extends: DefaultTheme,
  enhanceApp({ app }) {
    app.component('CatalogExplorer', CatalogExplorer)
  },
  Layout: () =>
    h(DefaultTheme.Layout, null, {
      'doc-before': () => h(Breadcrumbs),
    }),
} satisfies Theme
