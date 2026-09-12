/* DASHBOARD PAGE */
let dashData = null;
document.addEventListener('DOMContentLoaded', async () => {
  if (!requireAuth()) return;
  await loadDashboard();
});

async function loadDashboard() {
  try {
    const res = await api.get('/users/me/dashboard');
    dashData = res.data;
    renderStats(dashData);
    renderRecentDocs(dashData.recentDocuments || []);
    renderActivities(dashData.recentActivities || []);
    renderDocTypes(dashData.documentsByType || {});
    updateStorageSidebar(dashData);
  } catch (err) { console.error('Dashboard load failed', err); }
  const user = Auth.getUser();
  if (user) {
    const hour = new Date().getHours();
    const greeting = hour < 12 ? 'Good morning' : hour < 18 ? 'Good afternoon' : 'Good evening';
    const wm = document.getElementById('welcomeMsg');
    if (wm) wm.textContent = `${greeting}, ${user.firstName || user.username} 👋`;
  }
}

function renderStats(d) {
  setText('statDocs', d.totalDocuments || 0);
  setText('statStorage', d.storageUsedFormatted || '0 B');
  setText('statFolders', d.totalFolders || 0);
  setText('statShared', d.totalShared || 0);
  setText('storageUsed', d.storageUsedFormatted || '0 B');
  setText('storageLimit', formatBytes(d.storageLimit || 5368709120));
  const pct = Math.min(d.storageUsedPercent || 0, 100).toFixed(1);
  setText('storagePct', pct + '%');
  const fill = document.getElementById('storageFill');
  if (fill) fill.style.width = pct + '%';
}

function renderRecentDocs(docs) {
  const el = document.getElementById('recentDocsList');
  if (!el) return;
  if (!docs.length) { el.innerHTML = `<div class="empty-state"><i class="fa-solid fa-file-circle-plus"></i><p>No documents yet. <a href="/upload.html">Upload one</a></p></div>`; return; }
  el.innerHTML = docs.map(d => `
    <div class="doc-list-item" onclick="window.location='/documents.html'">
      <div class="doc-list-icon ${getFileClass(d.fileType)}"><i class="fa-solid ${getFileIcon(d.fileType)}"></i></div>
      <div class="doc-list-info">
        <div class="doc-list-name">${escHtml(d.originalName)}</div>
        <div class="doc-list-meta">${d.fileType?.toUpperCase() || ''} &bull; ${timeAgo(d.createdAt)}</div>
      </div>
      <div class="doc-list-size">${d.fileSizeFormatted || ''}</div>
    </div>`).join('');
}

function renderActivities(acts) {
  const el = document.getElementById('activityList');
  if (!el) return;
  if (!acts.length) { el.innerHTML = `<div class="empty-state"><i class="fa-solid fa-chart-line"></i><p>No activity yet</p></div>`; return; }
  const actionIcons = { UPLOAD_DOCUMENT:'fa-upload', DOWNLOAD_DOCUMENT:'fa-download', DELETE_DOCUMENT:'fa-trash', LOGIN:'fa-right-to-bracket', SHARE_DOCUMENT:'fa-share-nodes', CREATE_FOLDER:'fa-folder-plus', REGISTER:'fa-user-plus' };
  el.innerHTML = acts.map(a => `
    <div class="activity-item">
      <div class="activity-icon"><i class="fa-solid ${actionIcons[a.action]||'fa-circle-dot'}"></i></div>
      <div class="activity-text">${escHtml(a.description || a.action)}</div>
      <div class="activity-time">${timeAgo(a.createdAt)}</div>
    </div>`).join('');
}

function renderDocTypes(types) {
  const el = document.getElementById('docTypesChart');
  if (!el) return;
  const colors = ['#6366f1','#8b5cf6','#10b981','#f59e0b','#ef4444','#3b82f6','#ec4899'];
  const entries = Object.entries(types);
  if (!entries.length) { el.innerHTML = '<p style="color:var(--text-muted);font-size:13px">No documents yet</p>'; return; }
  el.innerHTML = entries.map(([type, count], i) => `
    <div class="doc-type-item">
      <div class="doc-type-dot" style="background:${colors[i % colors.length]}"></div>
      <span>${type.toUpperCase()}</span>
      <strong>${count}</strong>
    </div>`).join('');
}

function updateStorageSidebar(d) {
  const fill = document.getElementById('sideStorageFill');
  const text = document.getElementById('sideStorageText');
  const pct = Math.min(d.storageUsedPercent || 0, 100);
  if (fill) fill.style.width = pct + '%';
  if (text) text.textContent = `${d.storageUsedFormatted || '0 B'} of ${formatBytes(d.storageLimit || 5368709120)}`;
}

function setText(id, val) { const el = document.getElementById(id); if (el) el.textContent = val; }
function escHtml(str) { const d = document.createElement('div'); d.textContent = str || ''; return d.innerHTML; }
