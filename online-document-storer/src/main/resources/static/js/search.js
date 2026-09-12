/* SEARCH PAGE */
let searchPage = 0, searchTotalPages = 0;
document.addEventListener('DOMContentLoaded', () => {
  if (!requireAuth()) return;
  const params = new URLSearchParams(location.search);
  const q = params.get('q');
  if (q) { document.getElementById('searchInput').value = q; performSearch(); }
});

function handleSearchKey(e) { if (e.key === 'Enter') performSearch(); }
function toggleAdvanced() {
  const body = document.getElementById('advBody');
  const icon = document.getElementById('advIcon');
  body.classList.toggle('hidden');
  if (icon) icon.className = body.classList.contains('hidden') ? 'fa-solid fa-chevron-down' : 'fa-solid fa-chevron-up';
}
function clearFilters() {
  document.getElementById('filterType').value = '';
  document.getElementById('filterStart').value = '';
  document.getElementById('filterEnd').value = '';
}

async function performSearch(page = 0) {
  searchPage = page;
  const keyword = document.getElementById('searchInput').value.trim();
  const fileType = document.getElementById('filterType')?.value || '';
  const startDate = document.getElementById('filterStart')?.value || '';
  const endDate = document.getElementById('filterEnd')?.value || '';
  const params = new URLSearchParams({ page, size: 12 });
  if (keyword) params.set('keyword', keyword);
  if (fileType) params.set('fileType', fileType);
  if (startDate) params.set('startDate', startDate);
  if (endDate) params.set('endDate', endDate);
  const status = document.getElementById('searchStatus');
  if (status) { status.classList.remove('hidden'); status.textContent = 'Searching...'; }
  try {
    const res = await api.get('/documents/search?' + params.toString());
    const data = res.data;
    searchTotalPages = data.totalPages;
    if (status) {
      status.textContent = `Found ${data.totalElements} result(s)${keyword?' for "'+keyword+'"':''}`;
    }
    renderSearchResults(data.content || []);
    renderSearchPagination(data.totalPages, page);
  } catch(err) {
    if (status) { status.textContent = 'Search failed: ' + err.message; }
    toast(err.message, 'error');
  }
}

function renderSearchResults(docs) {
  const container = document.getElementById('searchResults');
  if (!container) return;
  if (!docs.length) {
    container.innerHTML = `<div style="grid-column:1/-1"><div class="empty-state"><i class="fa-solid fa-magnifying-glass" style="font-size:50px"></i><p>No results found. Try a different search term.</p></div></div>`;
    return;
  }
  container.className = 'docs-grid';
  container.innerHTML = docs.map(d => {
    const fc = getFileClass(d.fileType), fi = getFileIcon(d.fileType);
    return `<div class="doc-card">
      <div class="doc-card-thumb ${fc}"><i class="fa-solid ${fi}"></i></div>
      <div class="doc-card-body">
        <div class="doc-card-name" title="${escHtml(d.originalName)}">${escHtml(d.originalName)}</div>
        <div class="doc-card-meta"><span>${(d.fileType||'').toUpperCase()}</span><span>${d.fileSizeFormatted||''}</span></div>
        <div class="doc-card-meta" style="margin-top:2px"><span>${formatDate(d.createdAt)}</span></div>
        <div class="doc-card-actions">
          <button onclick="downloadDoc(${d.id},'${escHtml(d.originalName)}')"><i class="fa-solid fa-download"></i> Download</button>
        </div>
      </div>
    </div>`;
  }).join('');
}

async function downloadDoc(id, name) {
  try {
    const token = Auth.getToken();
    const res = await fetch('/api/documents/' + id + '/download', { headers: {'Authorization':'Bearer '+token} });
    if (!res.ok) throw new Error('Download failed');
    const blob = await res.blob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a'); a.href = url; a.download = name; a.click();
    URL.revokeObjectURL(url);
  } catch(err) { toast(err.message || 'Download failed', 'error'); }
}

function renderSearchPagination(total, current) {
  const el = document.getElementById('pagination');
  if (!el || total <= 1) { if(el) el.innerHTML=''; return; }
  let html = '';
  if (current > 0) html += `<button onclick="performSearch(${current-1})"><i class="fa-solid fa-chevron-left"></i></button>`;
  for (let i=0; i<total; i++) html += `<button class="${i===current?'active':''}" onclick="performSearch(${i})">${i+1}</button>`;
  if (current < total-1) html += `<button onclick="performSearch(${current+1})"><i class="fa-solid fa-chevron-right"></i></button>`;
  el.innerHTML = html;
}

function escHtml(str) { const d=document.createElement('div'); d.textContent=str||''; return d.innerHTML; }
