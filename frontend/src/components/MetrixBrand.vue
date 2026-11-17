<template>
  <RouterLink class="metrix-brand" :to="to">
    <img class="brand-mark" src="/metrix-mark.webp" alt="" width="32" height="34" />
    <span>Metrix</span>
  </RouterLink>
</template>

<script setup>
defineProps({to: {type: String, default: '/'}})
</script>

<style scoped>
.metrix-brand {
  display: inline-flex;
  align-items: center;
  justify-self: start;
  gap: 9px;
  width: max-content;
  color: var(--workspace-ink);
  font-size: 22px;
  font-weight: 680;
  line-height: 1;
  letter-spacing: -.05em;
  text-decoration: none;
}

.metrix-brand:hover { color: var(--workspace-ink); }

.brand-mark {
  display: block;
  flex: none;
  width: 32px;
  height: 34px;
  object-fit: contain;
}
</style>
