async function api(method, url, body) {
  const res = await fetch(url, {
    method,
    credentials: 'include',
    headers: body ? { 'Content-Type': 'application/json' } : undefined,
    body: body ? JSON.stringify(body) : undefined,
  });
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
  // empty bodies are fine
  try { return await res.json(); } catch { return null; }
}

// auth
export const login = (username, password) => api('POST', '/api/auth/login', { username, password });
export const me = () => api('GET', '/api/auth/me');
export const logout = () => api('POST', '/api/auth/logout');

// products (controller returns ProductResponse[])
export const listProducts = (page=0, size=50) => api('GET', `/api/products?page=${page}&size=${size}`);
export const searchByName = (q) => api('GET', `/api/products/search/name?q=${encodeURIComponent(q||'')}`);
export const searchByDescription = (q) => api('GET', `/api/products/search/description?q=${encodeURIComponent(q||'')}`);
export const createProduct = (dto) => api('POST', '/api/products', dto);
export const updateProduct = (id, dto) => api('PUT', `/api/products/${encodeURIComponent(id)}`, dto);
export const deleteProduct = (id) => api('DELETE', `/api/products/${encodeURIComponent(id)}`);

// cart (user)
export const getCart = () => api('GET', '/api/cart');
// server returns updated cart for add/remove/restore
export const addToCart = (productId, amount=1) => api('POST', '/api/cart/items', { productId, amount });
export const removeFromCart = (productId) => api('DELETE', `/api/cart/items/${encodeURIComponent(productId)}`);
export const restoreCart = () => api('POST', '/api/cart/restore');

// admin sessions
export const sessionsOverThreshold = (th=10) => api('GET', `/api/cart/report?threshold=${th}`);
export const adminCartForSession = (sessionId) => api('GET', `/api/cart/${encodeURIComponent(sessionId)}`);