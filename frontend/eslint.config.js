import js from '@eslint/js'
import prettierConfig from 'eslint-config-prettier'
import react from 'eslint-plugin-react'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import { defineConfig, globalIgnores } from 'eslint/config'
import globals from 'globals'
import tseslint from 'typescript-eslint'

export default defineConfig([
  globalIgnores(['dist', 'node_modules', 'coverage', '*.min.js']),

  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      js.configs.recommended,
      tseslint.configs.recommended,
      reactHooks.configs.flat.recommended,
      reactRefresh.configs.vite,
      prettierConfig,
    ],

    plugins: {
      react,
    },

    languageOptions: {
      globals: globals.browser,
    },

    settings: {
      react: { version: 'detect' },
    },

    rules: {
      '@typescript-eslint/no-unused-vars': [
        'error',
        { argsIgnorePattern: '^_', varsIgnorePattern: '^_' },
      ],

      'react-hooks/rules-of-hooks': 'error',

      'react-hooks/exhaustive-deps': 'error',

      '@typescript-eslint/no-explicit-any': 'error',

      '@typescript-eslint/ban-ts-comment': [
        'error',
        {
          'ts-ignore': 'allow-with-description',
          'ts-expect-error': 'allow-with-description',
          minimumDescriptionLength: 10,
        },
      ],

      eqeqeq: ['error', 'always', { null: 'ignore' }],

      'no-return-await': 'error',

      'react/jsx-key': ['error', { checkFragmentShorthand: true }],

      'react/no-array-index-key': 'error',

      'no-param-reassign': ['error', { props: false }],

      '@typescript-eslint/consistent-type-imports': [
        'error',
        { prefer: 'type-imports', fixStyle: 'separate-type-imports' },
      ],

      'no-console': ['warn', { allow: ['warn', 'error'] }],


      complexity: ['warn', { max: 10 }],

      'max-lines': ['warn', { max: 300, skipBlankLines: true, skipComments: true }],

      
      'max-lines-per-function': ['warn', { max: 80, skipBlankLines: true, skipComments: true }],

      '@typescript-eslint/no-unsafe-assignment': 'warn',
      '@typescript-eslint/no-unsafe-member-access': 'warn',
      '@typescript-eslint/no-unsafe-call': 'warn',
      '@typescript-eslint/no-unsafe-return': 'warn',

      'react/react-in-jsx-scope': 'off',
      'react/prop-types': 'off',
    },

    settings: {
      react: { version: 'detect' },
    },

    rules: {

      '@typescript-eslint/no-unused-vars': [
        'error',
        { argsIgnorePattern: '^_', varsIgnorePattern: '^_' },
      ],


      'react-hooks/rules-of-hooks': 'error',

      'react-hooks/exhaustive-deps': 'error',

      '@typescript-eslint/no-explicit-any': 'error',

      '@typescript-eslint/ban-ts-comment': [
        'error',
        {
          'ts-ignore': 'allow-with-description',
          'ts-expect-error': 'allow-with-description',
          minimumDescriptionLength: 10,
        },
      ],

      eqeqeq: ['error', 'always', { null: 'ignore' }],

      'no-return-await': 'error',

      'react/jsx-key': ['error', { checkFragmentShorthand: true }],

      'react/no-array-index-key': 'error',

      'no-param-reassign': ['error', { props: false }],

      '@typescript-eslint/consistent-type-imports': [
        'error',
        { prefer: 'type-imports', fixStyle: 'separate-type-imports' },
      ],

      'no-console': ['warn', { allow: ['warn', 'error'] }],

      complexity: ['warn', { max: 10 }],

      'max-lines': ['warn', { max: 300, skipBlankLines: true, skipComments: true }],

      'max-lines-per-function': ['warn', { max: 80, skipBlankLines: true, skipComments: true }],

      '@typescript-eslint/no-unsafe-assignment': 'warn',
      '@typescript-eslint/no-unsafe-member-access': 'warn',
      '@typescript-eslint/no-unsafe-call': 'warn',
      '@typescript-eslint/no-unsafe-return': 'warn',

      'react/react-in-jsx-scope': 'off',
      'react/prop-types': 'off',
    },
  },
])
