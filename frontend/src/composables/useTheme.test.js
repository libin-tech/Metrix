import test from 'node:test'
import assert from 'node:assert/strict'
import {initializeTheme, normalizeTheme, setTheme, useTheme} from './useTheme.js'

const withBrowser = callback => {
  const previousDocument = globalThis.document
  const previousStorage = globalThis.localStorage
  const values = new Map()
  globalThis.document = {documentElement: {dataset: {}, style: {}}}
  globalThis.localStorage = {
    getItem: key => values.get(key) ?? null,
    setItem: (key, value) => values.set(key, value)
  }
  try { callback(values) } finally {
    globalThis.document = previousDocument
    globalThis.localStorage = previousStorage
  }
}

test('defaults to light and migrates unsupported saved themes', () => withBrowser(values => {
  assert.equal(normalizeTheme(null), 'light')
  values.set('metrix-theme', 'purple')
  initializeTheme()
  assert.equal(useTheme().currentTheme.value, 'light')
  assert.equal(document.documentElement.dataset.theme, 'light')
}))

test('restores dark preference and persists toggling in both directions', () => withBrowser(values => {
  values.set('metrix-theme', 'dark')
  initializeTheme()
  assert.equal(document.documentElement.style.colorScheme, 'dark')
  assert.equal(useTheme().isDark.value, true)
  useTheme().toggleTheme()
  assert.equal(values.get('metrix-theme'), 'light')
  assert.equal(document.documentElement.dataset.theme, 'light')
  useTheme().toggleTheme()
  assert.equal(values.get('metrix-theme'), 'dark')
  initializeTheme()
  assert.equal(useTheme().isDark.value, true)
}))

test('switching works when browser storage is unavailable', () => withBrowser(() => {
  localStorage.getItem = () => { throw new Error('storage disabled') }
  localStorage.setItem = () => { throw new Error('storage disabled') }
  assert.doesNotThrow(initializeTheme)
  assert.equal(useTheme().currentTheme.value, 'light')
  assert.doesNotThrow(() => setTheme('dark'))
  assert.equal(document.documentElement.dataset.theme, 'dark')
}))
