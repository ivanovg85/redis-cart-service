<template>
  <div class="page">
    <header class="toolbar">
      <h1>User</h1>
      <div class="search">
        <input v-model.trim="qName" @keyup.enter="searchName" placeholder="Search name…" />
        <button @click="searchName" :disabled="busy.products">By name</button>
        <input v-model.trim="qDesc" @keyup.enter="searchDesc" placeholder="Search description…" />
        <button @click="searchDesc" :disabled="busy.products">By description</button>
        <button class="ghost" @click="resetAndLoad" :disabled="busy.products">Reset</button>
      </div>

      <button class="logout" @click="doLogout" :disabled="busy.logout">Logout</button>
      <button
        v-if="isAdmin"
        class="secondary"
        @click="goAdmin"
        :disabled="busy.products || busy.cart"
      >
        Admin
      </button>
    </header>

    <main class="grid">
      <section>
        <ProductTable
          :mode="'user'"
          :products="products"
          :busyMap="busy.addMap"
          @add="handleAdd"
        />
      </section>

      <aside>
        <CartPanel
          title="Your Cart"
          :items="cart"
          :removable="true"
          @remove-item="handleRemove"
        >
          <template #actions>
            <button @click="doRestore" :disabled="busy.restore">{{ busy.restore ? 'Restoring…' : 'Restore previous cart' }}</button>
            <button class="ghost" @click="loadCart" :disabled="busy.cart">⟳ Refresh</button>
          </template>
        </CartPanel>
      </aside>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import ProductTable from '../components/ProductTable.vue';
import CartPanel from '../components/CartPanel.vue';
import {
  listProducts,
  searchByName,
  searchByDescription,
  getCart,
  addToCart,
  removeFromCart,
  restoreCart,
  me as apiMe,
  logout as apiLogout,
} from '../api';

const router = useRouter();

// --- State ---
const products = ref([]);
const cart = ref([]);
const qName = ref('');
const qDesc = ref('');
const busy = reactive({
  products: false,
  cart: false,
  addMap: {},
  remove: {},
  restore: false,
  logout:false
});
const isAdmin = ref(false);

// --- Role handling ---
function normalizeRoles(arr) {
  return (Array.isArray(arr) ? arr : []).map(r =>
    r?.startsWith('ROLE_') ? r.slice(5) : r
  );
}

async function loadMe() {
  try {
    const info = await apiMe(); // { username, roles }
    const roles = normalizeRoles(info?.roles);
    isAdmin.value = roles.includes('ADMIN');
  } catch {
    isAdmin.value = false;
  }
}

// --- Product methods ---
async function loadAll() {
  busy.products = true;
  try {
    products.value = await listProducts();
  } finally {
    busy.products = false;
  }
}

async function searchProductsByName() {
  if (!qName.value.trim()) return loadAll();
  busy.products = true;
  try {
    products.value = await searchByName(qName.value.trim());
  } finally {
    busy.products = false;
  }
}

async function searchProductsByDesc() {
  if (!qDesc.value.trim()) return loadAll();
  busy.products = true;
  try {
    products.value = await searchByDescription(qDesc.value.trim());
  } finally {
    busy.products = false;
  }
}

function normalizeCartItems(arr) {
  return (Array.isArray(arr) ? arr : []).map(it => {
    // try the common variants; extend if your backend uses a different key
    const pid =
      it.productId ??
      it.product_id ??
      it.productID ??
      it.id ?? null; // avoid sku unless your backend accepts it for DELETE

    return { ...it, productId: pid };
  });
}

// --- Cart methods ---
async function loadCart() {
  busy.cart = true;
  try {
    const data = await getCart();
    cart.value = normalizeCartItems(data);
    console.log(cart.value);
  } finally {
    busy.cart = false;
  }
}

async function handleAdd(productId, amount) {
  if (!productId) return;
  const qty = Number(amount) || 1;
  busy.addMap[productId] = true;
  try {
    const data = await addToCart(productId, qty);
    cart.value = normalizeCartItems(data);
  } finally {
    busy.addMap[productId] = false;
  }
}

async function handleRemove(productId) {
  if (!productId) return;
  busy.remove[productId] = true;
  try {
    const data = await removeFromCart(productId);
    cart.value = normalizeCartItems(data);
  } finally {
    busy.remove[productId] = false;
  }
}

async function restoreCartHandler() {
  busy.cart = true;
  try {
    const data = await restoreCart();
    cart.value = normalizeCartItems(data);
  } finally {
    busy.cart = false;
  }
}

// logout
async function doLogout() {
  busy.logout = true;
  try { await apiLogout(); } catch {}
  finally { busy.logout = false; router.replace({ name: 'login' }); }
}

function goAdmin() { router.push({ name: 'admin' }); }

// --- Init ---
onMounted(async () => {
  await Promise.all([loadMe(), loadAll(), loadCart()]);
});
</script>

<style scoped>
.page{ display:flex; flex-direction:column; min-height:100vh;}
.toolbar{ display:flex; align-items:center; gap:12px; padding:12px 16px; border-bottom:1px solid #eee; background:#fff;}
.toolbar .search{ display:flex; gap:8px; align-items:center; margin-left:auto;}
.grid{ display:grid; grid-template-columns: 1fr 360px; gap:16px; padding:16px;}
button.ghost{ background:transparent; border:1px solid #ddd; padding:6px 10px; border-radius:6px; }
.error{ color:#b00020; padding:8px 16px;}
button.secondary { background: #eef6ff; border: 1px solid #b6d4fe; border-radius: 6px; padding: 6px 10px; }
button.logout {
  margin-left: 12px;
  padding: 6px 10px;
  border: 1px solid #ffccd0;
  background: #ffeef0;
  border-radius: 6px;
  cursor: pointer;
}
</style>