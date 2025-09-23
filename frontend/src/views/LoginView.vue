<template>
  <div class="login">
    <h1>Sign in</h1>
    <form @submit.prevent="onLogin">
      <label>Username <input v-model.trim="username" required /></label>
      <label>Password <input v-model="password" type="password" required /></label>
      <button :disabled="loading">{{ loading ? 'Signing in…' : 'Login' }}</button>
      <p v-if="error" class="error">⚠ {{ error }}</p>
    </form>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter, useRoute } from 'vue-router';

const router = useRouter();
const route = useRoute();

const username = ref('user');
const password = ref('user123');
const loading = ref(false);
const error = ref('');

async function fetchMe() {
  try {
    const res = await fetch('/api/auth/me', { credentials: 'include' });
    if (!res.ok) return null;
    try { return await res.json(); } catch { return {}; } // tolerate empty body
  } catch {
    return null;
  }
}

async function onLogin() {
  loading.value = true;
  error.value = '';
  try {
    // 1) authenticate (sets session cookie)
    const res = await fetch('/api/auth/login', {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: username.value, password: password.value }),
    });
    if (!res.ok) throw new Error(`HTTP ${res.status} ${res.statusText}`);

    // 2) optional redirect query takes precedence
    const redirectTo = route.query.redirect?.toString();
    if (redirectTo) {
      await router.replace(redirectTo);
      return;
    }

    // 3) role-aware default redirect
    const me = await fetchMe(); // { username, roles } | {} | null
    const roles = Array.isArray(me?.roles) ? me.roles : [];
    const isAdmin = roles.includes('ADMIN') || roles.includes('ROLE_ADMIN');

    await router.replace({ name: isAdmin ? 'admin' : 'user' });
  } catch (e) {
    console.error('Login failed:', e);
    error.value = 'Invalid credentials or server error.';
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.login{ max-width:360px; margin:64px auto; padding:24px; border:1px solid #eee; border-radius:8px; background:#fff;}
label{ display:block; margin:10px 0;}
input{ width:100%; padding:6px 8px; border:1px solid #ccc; border-radius:6px;}
button{ margin-top:10px; padding:8px 12px;}
.error{ color:#b00020; margin-top:8px;}
</style>