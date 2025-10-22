import js from '@eslint/js';
import eslintConfigPrettier from 'eslint-config-prettier';
import pluginPrettier from 'eslint-plugin-prettier';
import pluginVue from 'eslint-plugin-vue';
import globals from 'globals';
import autoImport from './.eslintrc-auto-import.json' with { type: 'json' };

const normalizeGlobals = (rawGlobals = {}) =>
  Object.fromEntries(
    Object.entries(rawGlobals).map(([key, value]) => [
      key,
      value ? 'readonly' : value,
    ]),
  );

const autoImportGlobals = normalizeGlobals(autoImport.globals);

const sharedGlobals = Object.assign(
  {},
  globals.browser,
  globals.node,
  globals.es2021,
  autoImportGlobals,
  {
    structuredClone: 'readonly',
    AbortController: 'readonly',
    FormData: 'readonly',
  },
);

export default [
  // 忽略文件
  {
    ignores: [
      'dist/**',
      'node_modules/**',
      '*.d.ts',
      '.eslintrc-auto-import.json',
      'package-lock.json',
      'yarn.lock',
      'pnpm-lock.yaml',
      '.vscode/**',
    ],
  },

  // 官方推荐
  js.configs.recommended,
  ...pluginVue.configs['flat/recommended'],

  // JS 基础
  {
    files: ['**/*.{js,jsx,mjs,cjs,ts,tsx}'],
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

  // Vue 专用
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

  // Prettier
  eslintConfigPrettier,
  {
    plugins: { prettier: pluginPrettier },
    rules: { ...pluginPrettier.configs.recommended.rules },
  },
];
