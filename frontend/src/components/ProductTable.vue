<template>
  <div class="table-wrap">
    <table>
      <thead>
        <tr>
          <th class="left">Name</th>
          <th class="left">SKU</th>
          <th class="left">Description</th>
          <th class="right">Price</th>
          <th v-if="mode==='user'" class="center" style="width:110px;">Amount</th>
          <th style="width: 200px;"></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="p in products" :key="p.id">
          <template v-if="mode==='admin'">
            <td><input v-model.trim="buffer[p.id].name" /></td>
            <td class="mono"><input v-model.trim="buffer[p.id].sku" /></td>
            <td><input v-model.trim="buffer[p.id].description" /></td>
            <td class="mono right"><input type="number" min="0" step="0.01" v-model.number="buffer[p.id].price" class="price"/></td>
            <td class="right">
              <button @click="$emit('update', p.id, { ...buffer[p.id] })" :disabled="busyMap[p.id]">Update</button>
              <button class="danger" @click="$emit('delete', p.id)" :disabled="busyMap[p.id]">Delete</button>
            </td>
          </template>

          <template v-else>
            <td>{{ p.name }}</td>
            <td class="mono">{{ p.sku }}</td>
            <td class="muted">{{ p.description }}</td>
            <td class="mono right">€ {{ fmt(p.price) }}</td>
            <td class="center">
              <input type="number" min="1" step="1" v-model.number="amountById[p.id]" @focus="ensureAmount(p.id)" class="amount"/>
            </td>
            <td class="right">
              <button @click="$emit('add', p.id, amountById[p.id] || 1)" :disabled="busyMap[p.id]">Add to cart</button>
            </td>
          </template>
        </tr>
        <tr v-if="!products.length">
          <td :colspan="mode==='admin'?5:6" class="muted center">No products.</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { reactive, watch, toRefs } from 'vue';

const props = defineProps({
  mode: { type: String, default: 'user' }, // 'user' | 'admin'
  products: { type: Array, default: () => [] },
  busyMap: { type: Object, default: () => ({}) }, // map of id->boolean for row buttons
});

const emit = defineEmits(['add','update','delete']);

const amountById = reactive({});
const buffer = reactive({});

watch(() => props.products, (list) => {
  for (const p of list) {
    if (!amountById[p.id]) amountById[p.id] = 1;
    if (!buffer[p.id]) buffer[p.id] = { sku: p.sku ?? '', name: p.name ?? '', description: p.description ?? '', price: Number(p.price ?? 0) };
  }
}, { immediate: true });

function ensureAmount(id) {
  if (!amountById[id] || amountById[id] < 1) amountById[id] = 1;
}
function fmt(v){ const n = Number(v ?? 0); return n.toFixed(2); }
</script>

<style scoped>
.table-wrap{ max-height: calc(100vh - 260px); overflow:auto; border:1px solid #eee; border-radius:8px; background:#fff;}
table{width:100%; border-collapse:collapse;}
thead th{ position:sticky; top:0; background:#fafafa; border-bottom:1px solid #eee; padding:10px;}
tbody td{ border-bottom:1px solid #f3f3f3; padding:10px; vertical-align:top;}
.left{text-align:left}.right{text-align:right}.center{text-align:center}
.mono{ font-family: ui-monospace, SFMono-Regular, Menlo, monospace;}
.muted{ color:#636e72;}
.amount{ width:70px; text-align:center; padding:6px 8px; border:1px solid #d0d0d0; border-radius:6px;}
.price{ width:120px; text-align:right;}
button{ padding:6px 10px; border:1px solid #c8c8c8; border-radius:6px; background:#f7f7f7; cursor:pointer;}
button.danger{ background:#ffeef0; border-color:#ffccd0;}
</style>