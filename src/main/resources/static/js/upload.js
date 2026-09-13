/* UPLOAD PAGE */
let fileQueue = [];
document.addEventListener('DOMContentLoaded', () => {
  if (!requireAuth()) return;
  loadFolders();
  setupDragDrop();
  document.getElementById('uploadTags')?.addEventListener('input', updateTagPreview);
});

async function loadFolders() {
  try {
    const res = await api.get('/folders');
    const sel = document.getElementById('uploadFolder');
    if (!sel) return;
    (res.data || []).forEach(f => {
      const opt = document.createElement('option');
      opt.value = f.id; opt.textContent = '📁 ' + f.name;
      sel.appendChild(opt);
    });
  } catch(e) { console.warn('Could not load folders'); }
}

function setupDragDrop() {
  const zone = document.getElementById('dropZone');
  if (!zone) return;
  zone.addEventListener('dragover', e => { e.preventDefault(); zone.classList.add('dragging'); });
  zone.addEventListener('dragleave', () => zone.classList.remove('dragging'));
  zone.addEventListener('drop', e => {
    e.preventDefault(); zone.classList.remove('dragging');
    handleFileSelect(e.dataTransfer.files);
  });
}

function handleFileSelect(files) {
  const allowed = ['pdf','docx','doc','xlsx','xls','pptx','ppt','txt','jpg','jpeg','png','gif','zip','rar'];
  Array.from(files).forEach(file => {
    const ext = file.name.split('.').pop().toLowerCase();
    if (!allowed.includes(ext)) { toast(`File type .${ext} not allowed`, 'warning'); return; }
    if (file.size > 52428800) { toast(`${file.name} exceeds 50MB limit`, 'warning'); return; }
    if (fileQueue.some(f => f.file.name === file.name && f.file.size === file.size)) return;
    fileQueue.push({ file, status:'pending', progress:0 });
  });
  renderQueue();
}

function renderQueue() {
  const queueDiv = document.getElementById('fileQueue');
  const listDiv = document.getElementById('queueList');
  const countEl = document.getElementById('queueCount');
  if (!queueDiv || !listDiv) return;
  if (!fileQueue.length) { queueDiv.classList.add('hidden'); return; }
  queueDiv.classList.remove('hidden');
  if (countEl) countEl.textContent = fileQueue.length;
  listDiv.innerHTML = fileQueue.map((item, i) => {
    const fi = getFileIcon(item.file.name.split('.').pop());
    const fc = getFileClass(item.file.name.split('.').pop());
    const statusIcon = item.status === 'done' ? '✅' : item.status === 'error' ? '❌' : '';
    return `<div class="queue-item" id="qitem${i}">
      <div class="doc-list-icon ${fc}" style="width:36px;height:36px;font-size:18px;flex-shrink:0"><i class="fa-solid ${fi}"></i></div>
      <div class="queue-item-info">
        <div class="queue-item-name">${escHtml(item.file.name)} ${statusIcon}</div>
        <div class="queue-item-size">${formatBytes(item.file.size)}</div>
        ${item.status === 'uploading' ? `<div class="queue-progress"><div class="progress-bar"><div class="progress-fill" id="pfill${i}" style="width:${item.progress}%"></div></div><div class="progress-text" id="ptext${i}">${item.progress}%</div></div>` : ''}
      </div>
      ${item.status === 'pending' ? `<button class="queue-item-remove" onclick="removeQueueItem(${i})"><i class="fa-solid fa-xmark"></i></button>` : ''}
    </div>`;
  }).join('');
}

function removeQueueItem(i) { fileQueue.splice(i, 1); renderQueue(); }
function clearQueue() { fileQueue = []; renderQueue(); }

async function uploadAll() {
  const pending = fileQueue.filter(f => f.status === 'pending');
  if (!pending.length) { toast('No files to upload', 'warning'); return; }
  const btn = document.getElementById('uploadAllBtn');
  if (btn) { btn.disabled = true; btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Uploading...'; }
  const desc = document.getElementById('uploadDesc')?.value || '';
  const folderId = document.getElementById('uploadFolder')?.value || '';
  const category = document.getElementById('uploadCategory')?.value || '';
  const tags = document.getElementById('uploadTags')?.value || '';
  const encrypt = document.getElementById('uploadEncrypt')?.checked || false;
  let successCount = 0;
  for (let i = 0; i < fileQueue.length; i++) {
    const item = fileQueue[i];
    if (item.status !== 'pending') continue;
    item.status = 'uploading';
    renderQueue();
    try {
      const formData = new FormData();
      formData.append('file', item.file);
      const metadata = { description: desc, folderId: folderId || null, category, tags, encrypt };
      formData.append('metadata', new Blob([JSON.stringify(metadata)], {type:'application/json'}));
      // Simulate progress with XHR
      await uploadWithProgress(formData, i);
      item.status = 'done';
      item.progress = 100;
      successCount++;
    } catch(err) {
      item.status = 'error';
      toast(`Failed to upload ${item.file.name}: ${err.message}`, 'error');
    }
    renderQueue();
  }
  if (btn) { btn.disabled = false; btn.innerHTML = '<i class="fa-solid fa-upload"></i> Upload All'; }
  if (successCount > 0) {
    toast(`${successCount} file(s) uploaded successfully!`, 'success');
    setTimeout(() => window.location.href = '/documents.html', 1500);
  }
}

function uploadWithProgress(formData, queueIndex) {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    const token = Auth.getToken();
    xhr.open('POST', '/api/documents/upload');
    if (token) xhr.setRequestHeader('Authorization', 'Bearer ' + token);
    xhr.upload.onprogress = (e) => {
      if (e.lengthComputable) {
        const pct = Math.round(e.loaded / e.total * 100);
        fileQueue[queueIndex].progress = pct;
        const fill = document.getElementById('pfill' + queueIndex);
        const text = document.getElementById('ptext' + queueIndex);
        if (fill) fill.style.width = pct + '%';
        if (text) text.textContent = pct + '%';
      }
    };
    xhr.onload = () => {
      try {
        const data = JSON.parse(xhr.responseText);
        if (xhr.status >= 200 && xhr.status < 300) resolve(data);
        else reject(new Error(data.message || 'Upload failed'));
      } catch { reject(new Error('Upload failed')); }
    };
    xhr.onerror = () => reject(new Error('Network error'));
    xhr.send(formData);
  });
}

function updateTagPreview() {
  const val = document.getElementById('uploadTags')?.value || '';
  const preview = document.getElementById('tagPreview');
  if (!preview) return;
  const tags = val.split(',').map(t => t.trim()).filter(Boolean);
  preview.innerHTML = tags.map(t => `<span class="tag-chip">${escHtml(t)}</span>`).join('');
}

function escHtml(str) { const d=document.createElement('div'); d.textContent=str||''; return d.innerHTML; }
