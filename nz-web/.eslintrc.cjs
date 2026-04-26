module.exports = {
  root: true,
  env: { browser: true, node: true, es2020: true },
  extends: [
    'eslint:recommended',
    'plugin:@typescript-eslint/recommended',
    'plugin:vue/vue3-recommended',
    'plugin:import/recommended',
    'plugin:import/typescript',
  ],
  parser: 'vue-eslint-parser',
  parserOptions: {
    parser: '@typescript-eslint/parser',
    ecmaVersion: 'latest',
    sourceType: 'module',
  },
  plugins: ['import'],
  settings: {
    'import/resolver': {
      typescript: {
        project: './tsconfig.app.json',
      },
      node: {
        extensions: ['.js', '.jsx', '.ts', '.tsx', '.vue'],
      },
    },
    // 声明虚拟模块，避免 import/no-unresolved 报错
    'import/core-modules': ['virtual:uno.css'],
  },
  rules: {
    'vue/multi-word-component-names': 'off',

    // ========== Import 排序规则 ==========
    'import/order': ['error', {
      groups: [
        'builtin',
        'external',
        'internal',
        ['parent', 'sibling'],
        'index',
        'object',
        'type',
      ],
      pathGroups: [
        {
          pattern: '@/**',
          group: 'internal',
          position: 'after',
        },
      ],
      pathGroupsExcludedImportTypes: ['builtin'],
      'newlines-between': 'always',
      alphabetize: {
        order: 'asc',
        caseInsensitive: true,
      },
    }],

    // 禁止未使用的导入（可选，根据项目需要调整）
    'import/no-unused-modules': 'off',

    // 确保导入语句在文件顶部
    'import/first': 'error',

    // 导入语句后需要一个空行
    'import/newline-after-import': 'error',

    // 禁止重复的导入
    'import/no-duplicates': 'error',

    // 关闭无法解析模块的报错（针对 Vite 虚拟模块）
    'import/no-unresolved': ['error', {
      ignore: ['^virtual:'],
    }],
  },
}
