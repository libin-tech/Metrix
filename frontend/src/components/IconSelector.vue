<template>
  <a-modal
    :open="visible"
    :title="$t('admin.selectIcon')"
    @cancel="$emit('close')"
    :footer="null"
    width="580px"
    destroy-on-close
  >
    <a-row :gutter="[8, 8]">
      <a-col v-for="name in iconNames" :key="name" :span="4">
        <div
          class="icon-item"
          :class="{ selected: selected === name }"
          @click="$emit('select', name)"
          :title="name"
        >
          <component :is="iconMap[name]" style="font-size: 22px;" />
        </div>
      </a-col>
    </a-row>
  </a-modal>
</template>

<script setup>
import { iconMap } from '../composables/iconMap'

defineProps({
  visible: Boolean,
  selected: String
})

defineEmits(['select', 'close'])

const iconNames = Object.keys(iconMap).sort()
</script>

<style scoped>
.icon-item {
  display: flex; align-items: center; justify-content: center;
  height: 48px; border: 1px solid #e8e8e8; border-radius: 6px;
  cursor: pointer; transition: all 0.2s;
}
.icon-item:hover { border-color: #1890ff; color: #1890ff; background: #e6f7ff; }
.icon-item.selected { border-color: #1890ff; color: #1890ff; background: #bae7ff; }
</style>
