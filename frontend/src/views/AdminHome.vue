<template>
  <div class="page">
    <header class="toolbar">
      <h1>Admin</h1>

      <div class="search">
        <input v-model.trim="qName" @keyup.enter="searchName" placeholder="Search name…" />
        <button @click="searchName" :disabled="busy.products">By name</button>
        <input v-model.trim="qDesc" @keyup.enter="searchDesc" placeholder="Search description…" />
        <button @click="searchDesc" :disabled="busy.products">By description</button>
        <button class="ghost" @click="resetAndLoad" :disabled="busy.products">Reset</button>
      </div>

      <button class="logout" @click="doLogout" :disabled="busy.logout">Logout</button>
    </header>

    <main class="grid">
      <section>
        <form class="create-form" @submit.prevent="create">
          <h2>Create Product</h2>
          <div class="grid2">
            <label>SKU <input v-model.trim="form.sku" required /></label>
            <label>Name <input v-model.trim="form.name" required /></label>
            <label class="span2">Description <input v-model.trim="form.description" /></label>
            <label>Price <input type="number" min="0" step="0.01" v-model.number="form.price" required /></label>
          </div>
          <button :disabled="busy.create">{{ busy.create ? 'Creating…' : 'Create' }}</button>
        </form>

        <ProductTable
          :mode="'admin'"
          :products="products"
          :busyMap="busy.row"
          @update="updateRow"
          @delete="deleteRow"
        />
      </section>

      <aside>
        <div class="sessions card">
          <div class="hdr">
            <h2>Sessions &gt; {{ threshold }} items</h2>
            <div class="controls">
              <input type="number" min="1" v-model.number="threshold" />
              <button @click="loadSessions" :disabled="busy.sessions">
                {{ busy.sessions ? 'Loading…' : 'Refresh' }}
              </button>
            </div>
          </div>

          <!-- Clean, scrollable list of session IDs -->
          <ul class="session-list">
            <li v-for="sid in sessions" :key="sid">
              <button class="link mono" @click="pickSession(sid)">{{ sid }}</button>
            </li>
            <li v-if="!sessions.length && !busy.sessions" class="muted">No sessions.</li>
          </ul>
        </div>

        <div class="card cart-panel">
          <h3 v-if="selectedSession">Cart for: <span class="mono">{{ selectedSession }}</span></h3>
          <CartPanel
            v-if="selectedSession"
            :items="adminCart"
            :removable="false"
            :showIds="true"
          />
          <div v-else class="muted">Select a session to view its cart.</div>
        </div>

        <div v-if="error" class="error">⚠ {{ error }}</div>
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
  listProducts, searchByName, searchByDescription,
  createProduct, updateProduct, deleteProduct,
  sessionsOverThreshold, // returns [{ sessionId, items: CartItemDto[] }, ...]
  logout as apiLogout,
} from '../api';

const router = useRouter();

const products = ref([]);
const qName = ref('');
const qDesc = ref('');
const error = ref('');
const form = reactive({ sku:'', name:'', description:'', price:0.0 });

const threshold = ref(10);
// NEW: state for sessions + lookup map of items by session
const sessions = ref([]);                   // string[] of sessionIds
const cartsBySession = ref({});             // { [sessionId: string]: CartItemDto[] }
const selectedSession = ref('');
const adminCart = ref([]);

const busy = reactive({
  products:false, create:false, row:{}, sessions:false, logout:false
});

// products
async function loadAll(){ busy.products=true; error.value=''; try{ products.value = await listProducts(); }catch(e){ error.value=e.message;} finally{ busy.products=false; } }
async function searchName(){ busy.products=true; error.value=''; try{ products.value = await searchByName(qName.value||''); }catch(e){ error.value=e.message;} finally{ busy.products=false; } }
async function searchDesc(){ busy.products=true; error.value=''; try{ products.value = await searchByDescription(qDesc.value||''); }catch(e){ error.value=e.message;} finally{ busy.products=false; } }
function resetAndLoad(){ qName.value=''; qDesc.value=''; loadAll(); }

async function create(){
  busy.create=true; error.value='';
  try{
    await createProduct({ sku:form.sku, name:form.name, description:form.description, price:Number(form.price) });
    form.sku=''; form.name=''; form.description=''; form.price=0;
    await loadAll();
  }catch(e){ error.value=e.message; } finally{ busy.create=false; }
}
async function updateRow(id, dto){
  busy.row[id]=true; error.value='';
  try{ await updateProduct(id, { ...dto, price:Number(dto.price) }); await loadAll(); }
  catch(e){ error.value=e.message; } finally{ busy.row[id]=false; }
}
async function deleteRow(id){
  busy.row[id]=true; error.value='';
  try{ await deleteProduct(id); await loadAll(); }
  catch(e){ error.value=e.message; } finally{ busy.row[id]=false; }
}

// sessions & carts
async function loadSessions(){
  busy.sessions = true;
  error.value = '';
  try {
    const data = await sessionsOverThreshold(threshold.value) || [];
    // Expecting [{ sessionId, items: [] }, ...]
    sessions.value = data.map(x => x.sessionId);
    cartsBySession.value = Object.fromEntries(
      data.map(x => [x.sessionId, Array.isArray(x.items) ? x.items : []])
    );
    // Reset selection if current session not present anymore
    if (selectedSession.value && !sessions.value.includes(selectedSession.value)) {
      selectedSession.value = '';
      adminCart.value = [];
    }
  } catch (e) {
    error.value = e.message;
  } finally {
    busy.sessions = false;
  }
}

function pickSession(sid) {
  selectedSession.value = sid;
  adminCart.value = cartsBySession.value[sid] || [];
}

// logout
async function doLogout() {
  busy.logout = true;
  try { await apiLogout(); } catch {}
  finally { busy.logout = false; router.replace({ name: 'login' }); }
}

onMounted(async () => {
  await Promise.all([loadAll(), loadSessions()]);
});
</script>

<style scoped>
.page{ display:flex; flex-direction:column; min-height:100vh;}
.toolbar{ display:flex; align-items:center; gap:12px; padding:12px 16px; border-bottom:1px solid #eee; background:#fff;}
.toolbar .search{ display:flex; gap:8px; align-items:center; margin-left:auto;}
button.logout{ margin-left:12px; padding:6px 10px; border:1px solid #ffccd0; background:#ffeef0; border-radius:6px; cursor:pointer;}

.grid{ display:grid; grid-template-columns: 1fr 420px; gap:16px; padding:16px;}
.card{ border:1px solid #eee; border-radius:8px; background:#fff; padding:12px; }

.create-form{ border:1px solid #eee; border-radius:8px; background:#fff; padding:12px; margin-bottom:12px;}
.create-form .grid2{ display:grid; grid-template-columns:1fr 1fr; gap:12px;}
.create-form .span2{ grid-column: span 2;}

.sessions .hdr{ display:flex; justify-content:space-between; align-items:center; }
.controls{ display:flex; gap:8px; align-items:center; }
.session-list{ list-style:none; padding:0; margin:8px 0 0 0; max-height:180px; overflow:auto; }
.link{ background:transparent; border:none; color:#0b67ff; cursor:pointer; padding:2px 0; text-align:left;}
.mono{ font-family: ui-monospace, SFMono-Regular, Menlo, monospace; }

.cart-panel{ margin-top:12px; }
.muted{ color:#636e72; }
.error{ color:#b00020; padding-top:8px; }
button.ghost{ background:transparent; border:1px solid #ddd; padding:6px 10px; border-radius:6px;}
</style>