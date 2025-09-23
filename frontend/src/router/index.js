import { createRouter, createWebHistory } from 'vue-router';

import LoginView from '../views/LoginView.vue';
import UserHome from '../views/UserHome.vue';
import AdminHome from '../views/AdminHome.vue';

async function fetchMe() {
  try {
    const res = await fetch('/api/auth/me', { credentials: 'include' });
    if (!res.ok) return null;

    // tolerate empty body
    try { return await res.json(); } catch { return {}; }
  } catch {
    return null;
  }
}

function normalizeRoles(arr) {
  return (Array.isArray(arr) ? arr : []).map(r =>
    typeof r === 'string' && r.startsWith('ROLE_') ? r.slice(5) : r
  );
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/login' },
    { path: '/login', name: 'login', component: LoginView },
    { path: '/user',  name: 'user',  component: UserHome,  meta: { requiresAuth: true } },
    // IMPORTANT: meta expects normalized roles (e.g., "ADMIN")
    { path: '/admin', name: 'admin', component: AdminHome, meta: { requiresAuth: true, roles: ['ADMIN'] } },
    { path: '/:pathMatch(.*)*', redirect: '/login' }
  ],
});

// No global cache. Always resolve /me fresh on each protected nav.
router.beforeEach(async (to) => {
  if (to.name === 'login') return true;
  if (!to.meta?.requiresAuth) return true;

  const me = await fetchMe();
  console.debug('[guard] /me ->', me);

  if (!me) {
    console.debug('[guard] unauthenticated → login');
    return { name: 'login', query: { redirect: to.fullPath } };
  }

  const need = to.meta.roles || [];
  if (need.length) {
    const roles = normalizeRoles(me.roles);
    const allowed = roles.some(r => need.includes(r));
    console.debug('[guard] roles:', roles, 'need:', need, 'allowed:', allowed);
    if (!allowed) {
      console.debug('[guard] not allowed → /user');
      return { name: 'user' };
    }
  }

  return true;
});

export default router;