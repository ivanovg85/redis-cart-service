<template>
  <div class="cart">
    <div class="cart-header">
      <h2>{{ title }}</h2>
      <slot name="actions"></slot>
    </div>

    <ul class="cart-list">
      <li v-for="item in items" :key="item.productId ?? item.product_id ?? item.id" class="cart-row">
        <div class="cart-main">
          <div class="cart-name">
            {{ item.name }}
            <small v-if="showIds" class="muted">[id: {{ idOf(item) }}]</small>
          </div>
          <div class="cart-desc muted">{{ item.shortDescription ?? item.short_desc ?? item.description }}</div>
          <div class="cart-meta">
            <span class="tag">x{{ item.amount }}</span>
            <span class="mono">€ {{ fmt(item.totalPrice ?? item.total_price) }}</span>
          </div>
        </div>

        <div class="cart-actions" v-if="removable">
          <button class="danger" @click="onRemove(item)">Remove</button>
        </div>
      </li>

      <li v-if="!items?.length" class="muted center">No items.</li>
    </ul>
  </div>
</template>

<script setup>
const props = defineProps({
  title: { type: String, default: 'Cart' },
  items: { type: Array, default: () => [] },
  removable: { type: Boolean, default: true },
  showIds: { type: Boolean, default: false },
});

const emit = defineEmits(['remove-item']);

function idOf(item) {
  return item?.productId
      ?? item?.product_id
      ?? item?.productID
      ?? item?.id
      ?? null;
}

function onRemove(item) {
  const id = idOf(item);
  console.log('[CartPanel] remove clicked, id =', id);
  if (id) emit('remove-item', id);
}

function fmt(v) {
  const n = Number(v ?? 0);
  return n.toFixed(2);
}
</script>

<style scoped>
.cart{ border:1px solid #eee; border-radius:8px; background:#fff; padding:12px; display:flex; flex-direction:column;}
.cart-header{ display:flex; justify-content:space-between; align-items:center;}
.cart-list{ list-style:none; padding:0; margin:12px 0 0 0; overflow:auto; max-height:60vh;}
.cart-row{ display:grid; grid-template-columns:1fr auto; gap:8px; padding:10px 0; border-bottom:1px solid #f5f5f5;}
.cart-name{ font-weight:600;}
.cart-meta{ display:flex; gap:8px; align-items:center; margin-top:6px;}
.tag{ display:inline-block; font-size:12px; padding:2px 6px; border-radius:999px; background:#eef2ff; color:#333;}
.mono{ font-family: ui-monospace, SFMono-Regular, Menlo, monospace;}
.muted{ color:#636e72;}
.center{text-align:center;}
button.danger{ padding:6px 10px; border:1px solid #ffccd0; background:#ffeef0; border-radius:6px; cursor:pointer;}
</style>