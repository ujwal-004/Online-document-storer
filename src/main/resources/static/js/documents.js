/* DOCUMENTS PAGE */
let currentPage = 0, totalPages = 0, currentView = 'grid', currentFilter = 'all', currentSort = 'createdAt,desc', selectedDocId = null;

document.addEventListener('DOMContentLoaded', () => {
  if (!requireAuth()) return;
  currentView = localStorage.getItem('docView') || 'grid';
  setView(currentView, false);
  loadDocuments();
});

async function loadDocuments(page = 0) {
  currentPage = page;
  const [sortBy, sortDir] = currentSort.split(',');
  try {
    const url = currentFilter === 'all'
      ? `/documents?page=${page}&size=12&sortBy=${sortBy}&sortDir=${sortDir}`
      : `/documents/search?fileType=${currentFilter}&page=${page}&size=12`;
    const res = await api.get(url);
    const pageData = res.data;
    totalPages = pageData.totalPages;
    renderDocuments(pageData.content || []);
    renderPagination(pageData.totalPages, page);
  } catch(err) { toast(err.message || 'Failed to load documents', 'error'); }
}

function renderDocuments(docs) {
  const container = document.getElementById('docsContainer');
  if (!container) return;
  if (!docs.length) {
    container.innerHTML = `<div style="grid-column:1/-1"><div class="empty-state"><i class="fa-solid fa-file-circle-plus" style="font-size:60px"></i><p style="font-size:16px">No documents found</p><a href="/upload.html" class="btn-primary" style="margin-top:16px;display:inline-flex">Upload your first document</a></div></div>`;
    return;
  }
  if (currentView === 'grid') {
    container.className = 'docs-grid';
    container.innerHTML = docs.map(doc => buildDocCard(doc)).join('');
  } else {
    container.className = 'docs-list';
    container.innerHTML = docs.map(doc => buildDocRow(doc)).join('');
  }
}

function buildDocCard(d) {
  const fc = getFileClass(d.fileType), fi = getFileIcon(d.fileType);
  return `<div class="doc-card">
    <div class="doc-card-thumb ${fc}" style="opacity:.9"><i class="fa-solid ${fi}"></i></div>
    <div class="doc-card-body">
      <div class="doc-card-name" title="${escHtml(d.originalName)}">${escHtml(d.originalName)}</div>
      <div class="doc-card-meta"><span>${(d.fileType||'').toUpperCase()}</span><span>${d.fileSizeFormatted||''}</span></div>
      <div class="doc-card-meta" style="margin-top:2px"><span>${timeAgo(d.createdAt)}</span>${d.isEncrypted ? '<span title="Encrypted"><i class="fa-solid fa-shield-halved" style="color:var(--success)"></i></span>' : ''}</div>
      <div class="doc-card-actions">
        <button onclick="viewDoc(${d.id})" title="View"><i class="fa-solid fa-eye"></i></button>
        <button onclick="downloadDoc(${d.id},'${escHtml(d.originalName)}')" title="Download"><i class="fa-solid fa-download"></i></button>
        <button onclick="openShareModal(${d.id})" title="Share"><i class="fa-solid fa-share-nodes"></i></button>
        <button class="danger" onclick="openDeleteModal(${d.id},'${escHtml(d.originalName)}')" title="Delete"><i class="fa-solid fa-trash"></i></button>
      </div>
    </div>
  </div>`;
}

function buildDocRow(d) {
  const fc = getFileClass(d.fileType), fi = getFileIcon(d.fileType);
  return `<div class="doc-list-row">
    <div class="doc-icon ${fc}"><i class="fa-solid ${fi}"></i></div>
    <div class="doc-name" title="${escHtml(d.originalName)}">${escHtml(d.originalName)}</div>
    <div class="doc-type">${(d.fileType||'—').toUpperCase()}</div>
    <div class="doc-size">${d.fileSizeFormatted||'—'}</div>
    <div class="doc-date">${formatDate(d.createdAt)}</div>
    <div class="doc-actions">
      <button class="icon-btn btn-sm" onclick="viewDoc(${d.id})" title="View"><i class="fa-solid fa-eye"></i></button>
      <button class="icon-btn btn-sm" onclick="downloadDoc(${d.id},'${escHtml(d.originalName)}')" title="Download"><i class="fa-solid fa-download"></i></button>
      <button class="icon-btn btn-sm" onclick="openShareModal(${d.id})" title="Share"><i class="fa-solid fa-share-nodes"></i></button>
      <button class="icon-btn btn-sm" style="border-color:var(--danger);color:var(--danger)" onclick="openDeleteModal(${d.id},'${escHtml(d.originalName)}')" title="Delete"><i class="fa-solid fa-trash"></i></button>
    </div>
  </div>`;
}

async function viewDoc(id) {
  try {
    const res = await api.get('/documents/' + id);
    const d = res.data;
    selectedDocId = id;
    document.getElementById('modalDocName').textContent = d.originalName;
    document.getElementById('modalDocBody').innerHTML = `
      <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;font-size:13px">
        <div><strong>Type:</strong> ${d.fileType?.toUpperCase()||'—'}</div>
        <div><strong>Size:</strong> ${d.fileSizeFormatted||'—'}</div>
        <div><strong>Downloads:</strong> ${d.downloadCount||0}</div>
        <div><strong>Views:</strong> ${d.viewCount||0}</div>
        <div><strong>Encrypted:</strong> ${d.isEncrypted?'<span style="color:var(--success)">Yes ✓</span>':'No'}</div>
        <div><strong>Shared:</strong> ${d.isShared ? '<span style="color:var(--info)">Yes</span>':'No'}</div>
        <div><strong>Folder:</strong> ${d.folderName||'Root'}</div>
        <div><strong>Version:</strong> v${d.version||1}</div>
        <div style="grid-column:1/-1"><strong>Uploaded:</strong> ${formatDate(d.createdAt)}</div>
        ${d.description?`<div style="grid-column:1/-1"><strong>Description:</strong> ${escHtml(d.description)}</div>`:''}
        ${d.tags?.length?`<div style="grid-column:1/-1"><strong>Tags:</strong> ${d.tags.map(t=>`<span class="tag-chip">${escHtml(t)}</span>`).join(' ')}</div>`:''}
        ${d.shareToken?`<div style="grid-column:1/-1"><strong>Share Link:</strong><br><code style="font-size:11px;word-break:break-all">${location.origin}/api/documents/shared/${d.shareToken}</code></div>`:''}
      </div>`;
    document.getElementById('modalDownloadBtn').onclick = () => downloadDoc(id, d.originalName);
    openModal('docModal');
  } catch(err) { toast(err.message, 'error'); }
}

async function downloadDoc(id, name) {
  try {
    const token = Auth.getToken();
    const res = await fetch('/api/documents/' + id + '/download', { headers: { 'Authorization': 'Bearer ' + token }});
    if (!res.ok) throw new Error('Download failed');
    const blob = await res.blob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url; a.download = name; a.click();
    URL.revokeObjectURL(url);
    toast('Download started', 'success');
  } catch(err) { toast(err.message || 'Download failed', 'error'); }
}

function openDeleteModal(id, name) {
  selectedDocId = id;
  document.getElementById('deleteDocName').textContent = name;
  openModal('deleteModal');
}

async function confirmDelete() {
  if (!selectedDocId) return;
  try {
    await api.delete('/documents/' + selectedDocId);
    closeModal('deleteModal');
    toast('Document deleted', 'success');
    loadDocuments(currentPage);
  } catch(err) { toast(err.message, 'error'); }
}

function openShareModal(id) {
  selectedDocId = id;
  document.getElementById('shareLinkBox').classList.add('hidden');
  document.getElementById('shareEmail').value = '';
  openModal('shareModal');
}

async function submitShare() {
  const email = document.getElementById('shareEmail').value.trim();
  const permission = document.getElementById('sharePermission').value;
  const expiry = document.getElementById('shareExpiry').value;
  if (!email) { toast('Please enter an email', 'warning'); return; }
  try {
    const res = await api.post('/documents/' + selectedDocId + '/share', {
      sharedWithEmail: email, permission, expiryDate: expiry || null
    });
    const shareToken = res.data.shareToken;
    if (shareToken) {
      document.getElementById('shareLink').value = `${location.origin}/api/documents/shared/${shareToken}`;
      document.getElementById('shareLinkBox').classList.remove('hidden');
    }
    toast('Document shared successfully', 'success');
  } catch(err) { toast(err.message, 'error'); }
}

function copyShareLink() {
  const input = document.getElementById('shareLink');
  input.select();
  navigator.clipboard.writeText(input.value).then(() => toast('Link copied!', 'success'));
}

function filterByType(type, btn) {
  currentFilter = type;
  document.querySelectorAll('.filter-tab').forEach(b => b.classList.remove('active'));
  btn.classList.add('active');
  loadDocuments(0);
}

function sortDocs(val) { currentSort = val; loadDocuments(0); }

function setView(view, save=true) {
  currentView = view;
  if (save) localStorage.setItem('docView', view);
  document.getElementById('gridViewBtn')?.classList.toggle('active', view==='grid');
  document.getElementById('listViewBtn')?.classList.toggle('active', view==='list');
  const c = document.getElementById('docsContainer');
  if (c) c.className = view === 'grid' ? 'docs-grid' : 'docs-list';
  if (save) loadDocuments(currentPage);
}

function renderPagination(total, current) {
  const el = document.getElementById('pagination');
  if (!el || total <= 1) { if (el) el.innerHTML = ''; return; }
  let html = '';
  if (current > 0) html += `<button onclick="loadDocuments(${current-1})"><i class="fa-solid fa-chevron-left"></i></button>`;
  for (let i = 0; i < total; i++) {
    if (total > 7 && Math.abs(i - current) > 2 && i !== 0 && i !== total-1) {
      if (i === 1 || i === total-2) html += `<button disabled>…</button>`;
      continue;
    }
    html += `<button class="${i===current?'active':''}" onclick="loadDocuments(${i})">${i+1}</button>`;
  }
  if (current < total-1) html += `<button onclick="loadDocuments(${current+1})"><i class="fa-solid fa-chevron-right"></i></button>`;
  el.innerHTML = html;
}

function escHtml(str) { const d=document.createElement('div'); d.textContent=str||''; return d.innerHTML; }
