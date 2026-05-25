# AGENTS.md — Frontend

> 全局通用规范参见：`../AGENTS.md`

# 技术栈
- Vue 3.4
- Vite 5
- Ant Design Vue 4
- ECharts 5
- Vue Router 4
- Vue i18n
- Axios
- marked / markstream-vue

# 开发规范
## 1. 项目结构
- 页面组件存放于 `frontend/src/views/`
- 公共组件存放于 `frontend/src/components/`
- API 调用统一封装在 `frontend/src/api/index.js`
- 国际化文件存放于 `frontend/src/i18n/`
- 路由配置存放于 `frontend/src/router/index.js`
- 组合式函数存放于 `frontend/src/composables/`

## 2. 组件规范
- 统一使用 Vue 3 Composition API（`<script setup>`）。
- 组件命名采用 PascalCase。
- 组件 props 必须定义类型和默认值。

## 3. API 规范
- 所有 HTTP 请求统一通过 `frontend/src/api/index.js` 中封装的 axios 实例发起。
- 不在组件中直接引入 axios 或创建新的请求实例。

## 4. 样式规范
- 优先使用 Ant Design Vue 内置组件和样式。
- 自定义样式使用 scoped 方式。

## 5. 国际化
- 所有用户可见文本必须定义在 i18n 文件中，禁止在组件中硬编码中文。
- 新增文案同时补充 `zh-CN.js` 和 `en.js`。
