/* =============================================
   DOCSTORER — CORE APP UTILITIES
   ============================================= */
const API_BASE = '/api';

// ---- TOKEN / AUTH ----
const Auth = {
  getToken:    () => localStorage.getItem('accessToken'),
  getRefresh:  () => localStorage.getItem('refreshToken'),
  getUser:     () => { try { return JSON.parse(localStorage.getItem('user') || 'null'); } catch { return null; } },
  save: (data) => {
    localStorage.setItem('accessToken', data.accessToken);
    if (data.refreshToken) localStorage.setItem('refreshToken', data.refreshToken);
    if (data.user) localStorage.setItem('user', JSON.stringify(data.user));
  },
  clear: () => { localStorage.removeItem('accessToken'); localStorage.removeItem('refreshToken'); localStorage.removeItem('user'); },
  isLoggedIn: () => !!localStorage.getItem('accessToken')
};

// ---- FETCH WRAPPER ----
async function apiRequest(method, path, body = null, isFormData = false) {
  const headers = {};
  const token = Auth.getToken();
  if (token) headers['Authorization'] = 'Bearer ' + token;
  if (!isFormData && body) headers['Content-Type'] = 'application/json';

  const opts = { method, headers };
  if (body) opts.body = isFormData ? body : JSON.stringify(body);

  const res = await fetch(API_BASE + path, opts);

  if (res.status === 401) {
    Auth.clear();
    window.location.href = '/login.html';
    return;
  }

  const contentType = res.headers.get('content-type') || '';
  if (contentType.includes('application/json')) {
    const data = await res.json();
    if (!res.ok) throw new Error(data.message || 'Request failed');
    return data;
  }
  if (!res.ok) throw new Error('Request failed: ' + res.status);
  return res;
}

const api = {
  get:    (path)         => apiRequest('GET', path),
  post:   (path, body)   => apiRequest('POST', path, body),
  put:    (path, body)   => apiRequest('PUT', path, body),
  delete: (path)         => apiRequest('DELETE', path),
  upload: (path, formData) => apiRequest('POST', path, formData, true),
};

// ---- GUARD ----
function requireAuth() {
  if (!Auth.isLoggedIn()) {
    window.location.href = '/login.html';
    return false;
  }
  return true;
}

function logout() {
  const token = Auth.getToken();
  if (token) fetch(API_BASE + '/auth/logout', {
    method: 'POST',
    headers: { 'Authorization': 'Bearer ' + token }
  }).catch(() => {});
  Auth.clear();
  window.location.href = '/login.html';
}

// ---- SIDEBAR ----
function toggleSidebar() {
  document.getElementById('sidebar').classList.toggle('open');
  document.getElementById('sidebarOverlay').classList.toggle('open');
}

// ---- AVATAR MENU ----
function toggleAvatarMenu() {
  document.getElementById('avatarDropdown').classList.toggle('open');
}
document.addEventListener('click', (e) => {
  const dd = document.getElementById('avatarDropdown');
  if (dd && !e.target.closest('.avatar-menu')) dd.classList.remove('open');
});

// ---- THEME ----
function toggleTheme() {
  const current = document.documentElement.getAttribute('data-theme');
  const next = current === 'dark' ? 'light' : 'dark';
  document.documentElement.setAttribute('data-theme', next);
  localStorage.setItem('theme', next);
  const icon = document.getElementById('themeIcon');
  if (icon) {
    icon.className = next === 'dark' ? 'fa-solid fa-sun' : 'fa-solid fa-circle-half-stroke';
  }
}
function applyTheme() {
  const t = localStorage.getItem('theme') || 'light';
  document.documentElement.setAttribute('data-theme', t);
  const icon = document.getElementById('themeIcon');
  if (icon) icon.className = t === 'dark' ? 'fa-solid fa-sun' : 'fa-solid fa-circle-half-stroke';
}

// ---- TOAST ----
function toast(msg, type = 'info', duration = 3500) {
  const container = document.getElementById('toastContainer');
  if (!container) return;
  const icons = { success:'fa-check-circle', error:'fa-circle-xmark', info:'fa-circle-info', warning:'fa-triangle-exclamation' };
  const t = document.createElement('div');
  t.className = `toast toast-${type}`;
  t.innerHTML = `<i class="fa-solid ${icons[type]||icons.info}"></i><span>${msg}</span><button class="toast-close" onclick="this.parentElement.remove()"><i class="fa-solid fa-xmark"></i></button>`;
  container.appendChild(t);
  setTimeout(() => { t.style.opacity = '0'; t.style.transform = 'translateX(100%)'; setTimeout(() => t.remove(), 300); }, duration);
}

// ---- MODALS ----
function openModal(id) {
  const m = document.getElementById(id);
  if (m) { m.classList.add('open'); m.style.display = 'flex'; }
}
function closeModal(id) {
  const m = document.getElementById(id);
  if (m) { m.classList.remove('open'); m.style.display = 'none'; }
}

// ---- NOTIFICATIONS ----
function toggleNotifications() {
  document.getElementById('notifPanel')?.classList.toggle('open');
}

// ---- FILE TYPE HELPERS ----
const FILE_COLORS = {
  pdf:'ft-pdf', doc:'ft-doc', docx:'ft-docx', xls:'ft-xls', xlsx:'ft-xlsx',
  ppt:'ft-ppt', pptx:'ft-pptx', jpg:'ft-jpg', jpeg:'ft-jpeg', png:'ft-png',
  gif:'ft-gif', txt:'ft-txt', zip:'ft-zip', rar:'ft-rar'
};
const FILE_ICONS = {
  pdf:'fa-file-pdf', doc:'fa-file-word', docx:'fa-file-word',
  xls:'fa-file-excel', xlsx:'fa-file-excel', ppt:'fa-file-powerpoint',
  pptx:'fa-file-powerpoint', jpg:'fa-file-image', jpeg:'fa-file-image',
  png:'fa-file-image', gif:'fa-file-image', txt:'fa-file-lines',
  zip:'fa-file-zipper', rar:'fa-file-zipper'
};

function getFileClass(ext) { return FILE_COLORS[(ext||'').toLowerCase()] || 'ft-default'; }
function getFileIcon(ext)  { return FILE_ICONS[(ext||'').toLowerCase()]  || 'fa-file'; }

function formatBytes(bytes) {
  if (!bytes) return '0 B';
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1048576) return (bytes/1024).toFixed(1) + ' KB';
  if (bytes < 1073741824) return (bytes/1048576).toFixed(1) + ' MB';
  return (bytes/1073741824).toFixed(1) + ' GB';
}

function timeAgo(dateStr) {
  if (!dateStr) return '';
  const diff = Date.now() - new Date(dateStr).getTime();
  const m = Math.floor(diff/60000), h = Math.floor(diff/3600000), d = Math.floor(diff/86400000);
  if (diff < 60000) return 'just now';
  if (m < 60) return m + 'm ago';
  if (h < 24) return h + 'h ago';
  if (d < 7) return d + 'd ago';
  return new Date(dateStr).toLocaleDateString();
}

function formatDate(dateStr) {
  if (!dateStr) return '';
  return new Date(dateStr).toLocaleDateString('en-US', {year:'numeric',month:'short',day:'numeric'});
}

// ---- LOAD USER IN TOPBAR ----
function loadTopbarUser() {
  const user = Auth.getUser();
  if (!user) return;
  const av = document.getElementById('topAvatar');
  const info = document.getElementById('avatarInfo');
  if (av) {
    if (user.profilePicture) {
      av.style.backgroundImage = `url(${user.profilePicture})`;
      av.style.backgroundSize = 'cover';
      av.textContent = '';
    } else {
      av.textContent = (user.firstName || user.username || '?')[0].toUpperCase();
    }
  }
  if (info) info.textContent = user.fullName || user.username;
}

// ---- QUICK SEARCH NAV ----
function quickSearchDocs(e) {
  if (e.key === 'Enter') {
    const val = e.target.value.trim();
    if (val) window.location.href = `/search.html?q=${encodeURIComponent(val)}`;
  }
}

// ---- TOGGLE PASSWORD ----
function togglePw(inputId, iconId) {
  const input = document.getElementById(inputId);
  const icon = document.getElementById(iconId);
  if (!input) return;
  if (input.type === 'password') {
    input.type = 'text';
    if (icon) icon.className = 'fa-solid fa-eye-slash';
  } else {
    input.type = 'password';
    if (icon) icon.className = 'fa-solid fa-eye';
  }
}

// ---- INIT ----
document.addEventListener('DOMContentLoaded', () => {
  applyTheme();
  const isAuthPage = window.location.pathname.includes('login') ||
                     window.location.pathname.includes('register') ||
                     window.location.pathname === '/';
  if (!isAuthPage) {
    if (!requireAuth()) return;
    loadTopbarUser();
  }
  // Apply saved dark mode from user prefs
  const user = Auth.getUser();
  if (user && user.darkMode) {
    document.documentElement.setAttribute('data-theme', 'dark');
    localStorage.setItem('theme', 'dark');
  }
});
