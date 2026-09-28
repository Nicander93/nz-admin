import js from '@eslint/js'
import tsPlugin from '@typescript-eslint/eslint-plugin'
import * as tsParser from '@typescript-eslint/parser'
import { createTypeScriptImportResolver } from 'eslint-import-resolver-typescript'
import { importX } from 'eslint-plugin-import-x'
import vue from 'eslint-plugin-vue'
import globals from 'globals'
import * as vueParser from 'vue-eslint-parser'

const sourceFiles = ['**/*.{js,mjs,cjs,ts,tsx,vue}']
const typeScriptFiles = ['**/*.{ts,tsx}']
const vueFiles = ['**/*.vue']

export default [
  {
    ignores: [
      'dist/**',
      'dist-ssr/**',
      'node_modules/**',
      'playwright-report/**',
      'test-results/**',
      '**/*.d.ts',
      '**/*.tsbuildinfo',
      '**/*.orig',
    ],
  },
  js.configs.recommended,
  importX.flatConfigs.recommended,
  importX.flatConfigs.typescript,
  ...vue.configs['flat/essential'],
  {
    files: sourceFiles,
    languageOptions: {
      ecmaVersion: 'latest',
      sourceType: 'module',
      globals: {
        ...globals.browser,
        ...globals.node,
      },
    },
    settings: {
      'import-x/resolver-next': [
        createTypeScriptImportResolver({
          project: './tsconfig.app.json',
        }),
      ],
      'import-x/core-modules': ['virtual:uno.css'],
    },
    rules: {
      'vue/multi-word-component-names': 'off',
      'import-x/default': 'off',
      'import-x/namespace': 'off',
      'import-x/no-named-as-default-member': 'off',
      'import-x/order': [
        'error',
        {
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
        },
      ],
      'import-x/no-unused-modules': 'off',
      'import-x/first': 'error',
      'import-x/newline-after-import': 'error',
      'import-x/no-duplicates': 'error',
      'import-x/no-unresolved': [
        'error',
        {
          ignore: ['^virtual:'],
        },
      ],
    },
  },
  {
    files: typeScriptFiles,
    languageOptions: {
      parser: tsParser,
      parserOptions: {
        ecmaVersion: 'latest',
        sourceType: 'module',
      },
    },
    plugins: {
      '@typescript-eslint': tsPlugin,
    },
    rules: tsPlugin.configs.recommended.rules,
  },
  {
    files: vueFiles,
    languageOptions: {
      parser: vueParser,
      parserOptions: {
        parser: tsParser,
        ecmaVersion: 'latest',
        sourceType: 'module',
        extraFileExtensions: ['.vue'],
      },
    },
    plugins: {
      '@typescript-eslint': tsPlugin,
    },
    rules: tsPlugin.configs.recommended.rules,
  },
]
