import js from '@eslint/js';
import eslintConfigPrettier from 'eslint-config-prettier';
import pluginPrettier from 'eslint-plugin-prettier';
import pluginVue from 'eslint-plugin-vue';
import globals from 'globals';
import autoImport from './src/.eslintrc-auto-import.json' with { type: 'json' };

const sharedGlobals = {
  ...globals.browser,
  ...globals.node,
  ...globals.es2021,
  ...autoImport.globals,
  structuredClone: 'readonly',
  AbortController: 'readonly',
  FormData: 'readonly',
};

export default [
  // 忽略文件
  {
    ignores: [
      'dist/**',
      'node_modules/**',
      '*.d.ts',
      '.eslintrc-auto-import.json',
      'src/*.d.ts',
      'src/.eslintrc-auto-import.json',
      'pnpm-lock.yaml',
      '.vscode/**',
    ],
  },

  // 基础 JS 推荐规则
  js.configs.recommended,

  // Vue 推荐规则
  ...pluginVue.configs['flat/recommended'],

  // JS
  {
    files: ['**/*.{js,jsx,mjs,cjs,ts,tsx,vue}'],
    languageOptions: {
      ecmaVersion: 'latest',
      sourceType: 'module',
      globals: sharedGlobals,
    },
    rules: {
      'no-console': ['warn', { allow: ['warn', 'error'] }],
      'no-debugger': process.env.NODE_ENV === 'production' ? 'error' : 'warn',
      'no-alert': 'warn',
      'no-eval': 'error',
      'no-implied-eval': 'error',
      'no-unused-vars': [
        'warn',
        {
          argsIgnorePattern: '^_',
          varsIgnorePattern: '^_',
          caughtErrorsIgnorePattern: '^_',
        },
      ],
      'no-undef': 'error',
      'no-redeclare': 'error',
      'no-shadow': 'warn',
      eqeqeq: ['error', 'always', { null: 'ignore' }],
      'prefer-const': 'warn',
      'no-var': 'error',
    },
  },

  // Vue
  {
    files: ['**/*.vue'],
    languageOptions: {
      globals: sharedGlobals,
    },
    rules: {
      'vue/multi-word-component-names': 'off',
      'vue/no-v-html': 'warn',
      'vue/prefer-import-from-vue': 'error',
      'vue/no-useless-mustaches': 'warn',
      'vue/no-useless-v-bind': 'warn',
      'vue/prefer-true-attribute-shorthand': 'warn',
      'vue/component-name-in-template-casing': [
        'error',
        'PascalCase',
        { registeredComponentsOnly: false },
      ],
      'vue/attribute-hyphenation': ['error', 'always'],
      'vue/v-on-event-hyphenation': ['error', 'always'],
      'vue/require-v-for-key': 'error',
      'vue/no-side-effects-in-computed-properties': 'error',
      'vue/no-async-in-computed-properties': 'error',
      'vue/return-in-computed-property': 'error',
    },
  },

  // Prettier — 放最后，让它覆盖可能冲突的 ESLint 规则
  eslintConfigPrettier,
  {
    plugins: { prettier: pluginPrettier },
    rules: { ...pluginPrettier.configs.recommended.rules },
  },
];
