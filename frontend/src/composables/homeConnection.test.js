import test from 'node:test'
import assert from 'node:assert/strict'
import {readFileSync} from 'node:fs'
import vm from 'node:vm'
import {computed, ref} from 'vue'
import {useTheme} from './useTheme.js'

// Execute Home's real setup/lifecycle with only browser and rendering boundaries stubbed.
const homeScript = readFileSync(new URL('../views/Home.vue', import.meta.url), 'utf8')
  .match(/<script setup>([\s\S]*?)<\/script>/)[1]
  .replace(/^import .*$/gm, '')

const mountHome = () => {
  const mounted = []
  const unmounted = []
  const timers = new Map()
  const sockets = []
  const redirects = []
  let token = 'test-session'
  let timerId = 0
  class Socket {
    constructor() { sockets.push(this) }
    close() { this.onclose?.() }
  }
  const context = vm.createContext({
    useTheme, computed, ref, watch: () => {}, nextTick: callback => callback(),
    onMounted: callback => mounted.push(callback), onUnmounted: callback => unmounted.push(callback),
    useRouter: () => ({replace: path => redirects.push(path)}), useI18n: () => ({t: key => key}),
    Empty: {}, use: () => {}, BarChart: {}, CanvasRenderer: {}, GridComponent: {}, TooltipComponent: {},
    localStorage: {getItem: () => token}, WebSocket: Socket,
    window: {
      location: {protocol: 'http:', host: 'localhost'},
      setTimeout: callback => { timers.set(++timerId, callback); return timerId },
      clearTimeout: id => timers.delete(id), addEventListener: () => {}, removeEventListener: () => {}
    }
  })
  vm.runInContext(homeScript, context)
  mounted.forEach(callback => callback())
  return {
    sockets, redirects, timers,
    clearToken: () => { token = null },
    unmount: () => unmounted.forEach(callback => callback()),
    reconnect: () => { const [id, callback] = timers.entries().next().value; timers.delete(id); callback() },
    state: () => vm.runInContext('marketDashboardConnectionState.value', context)
  }
}

test('Home connects, reconnects after interruption and stops retrying on unmount', () => {
  const home = mountHome()
  assert.equal(home.sockets.length, 1)
  home.sockets[0].onopen()
  assert.equal(home.state(), 'Connected')
  home.sockets[0].close()
  assert.equal(home.state(), 'Reconnecting')
  assert.equal(home.timers.size, 1)
  home.reconnect()
  assert.equal(home.sockets.length, 2)
  home.unmount()
  assert.equal(home.timers.size, 0)
})

test('Home redirects to login when session disappears before reconnection', () => {
  const home = mountHome()
  home.sockets[0].close()
  home.clearToken()
  home.reconnect()
  assert.deepEqual(home.redirects, ['/login'])
  assert.equal(home.sockets.length, 1)
  assert.equal(home.timers.size, 0)
})
