/* PROFILE PAGE */
document.addEventListener('DOMContentLoaded', async () => {
  if (!requireAuth()) return;
  await loadProfile();
  const hash = location.hash;
  if (hash === '#settings') showTab('tabSecurity', document.querySelector('.stab:nth-child(2)'));
});

async function loadProfile() {
  try {
    const [profileRes, dashRes] = await Promise.all([api.get('/users/me'), api.get('/users/me/dashboard')]);
    const user = profileRes.data, stats = dashRes.data;
    document.getElementById('profileName').textContent = user.fullName || user.username;
    document.getElementById('profileEmail').textContent = user.email;
    document.getElementById('profileRole').textContent = user.role;
    const av = document.getElementById('profileAvatar');
    if (av) {
      if (user.profilePicture) { av.innerHTML = `<img src="${user.profilePicture}" alt="avatar"/>`; }
      else { av.textContent = (user.firstName||user.username||'?')[0].toUpperCase(); }
    }
    setText('editFirstName', user.firstName, 'value');
    setText('editLastName', user.lastName, 'value');
    setText('editPhone', user.phoneNumber, 'value');
    setText('editUsername', user.username, 'value');
    setText('editEmail', user.email, 'value');
    setText('pStatDocs', stats.totalDocuments || 0);
    setText('pStatStorage', stats.storageUsedFormatted || '0 B');
    setText('pStatJoined', new Date(user.createdAt).getFullYear());
    const dm = document.getElementById('darkModeToggle');
    if (dm) dm.checked = user.darkMode || false;
  } catch(err) { toast('Failed to load profile', 'error'); }
}

async function saveProfile() {
  const alert = document.getElementById('profileAlert');
  try {
    const res = await api.put('/users/me', {
      firstName: document.getElementById('editFirstName').value,
      lastName: document.getElementById('editLastName').value,
      phoneNumber: document.getElementById('editPhone').value
    });
    localStorage.setItem('user', JSON.stringify(res.data));
    showFormAlert(alert, 'Profile updated successfully!', 'success');
    toast('Profile saved', 'success');
  } catch(err) { showFormAlert(alert, err.message, 'error'); }
}

async function changePassword() {
  const alert = document.getElementById('securityAlert');
  const oldPw = document.getElementById('oldPassword').value;
  const newPw = document.getElementById('newPassword').value;
  const confirmPw = document.getElementById('confirmNewPassword').value;
  if (!oldPw || !newPw || !confirmPw) { showFormAlert(alert, 'All fields required', 'error'); return; }
  if (newPw !== confirmPw) { showFormAlert(alert, 'New passwords do not match', 'error'); return; }
  if (newPw.length < 8) { showFormAlert(alert, 'Password min 8 characters', 'error'); return; }
  try {
    await api.put('/users/me/password', { oldPassword: oldPw, newPassword: newPw });
    showFormAlert(alert, 'Password changed successfully!', 'success');
    document.getElementById('oldPassword').value = '';
    document.getElementById('newPassword').value = '';
    document.getElementById('confirmNewPassword').value = '';
  } catch(err) { showFormAlert(alert, err.message, 'error'); }
}

async function saveDarkMode(checkbox) {
  try {
    await api.put('/users/me', { darkMode: checkbox.checked });
    const user = Auth.getUser();
    if (user) { user.darkMode = checkbox.checked; localStorage.setItem('user', JSON.stringify(user)); }
    document.documentElement.setAttribute('data-theme', checkbox.checked ? 'dark' : 'light');
    localStorage.setItem('theme', checkbox.checked ? 'dark' : 'light');
    toast('Preference saved', 'success');
  } catch(err) { toast(err.message, 'error'); }
}

function saveViewPref(val) { localStorage.setItem('docView', val); toast('View preference saved', 'success'); }

async function uploadAvatar(input) {
  const file = input.files[0];
  if (!file) return;
  const fd = new FormData(); fd.append('avatar', file);
  try {
    const token = Auth.getToken();
    const res = await fetch('/api/users/me/avatar', { method:'POST', headers:{'Authorization':'Bearer '+token}, body:fd });
    const data = await res.json();
    if (!res.ok) throw new Error(data.message);
    const av = document.getElementById('profileAvatar');
    if (av && data.data.profilePicture) av.innerHTML = `<img src="${data.data.profilePicture}" alt="avatar"/>`;
    toast('Avatar updated', 'success');
  } catch(err) { toast(err.message, 'error'); }
}

function showTab(tabId, btn) {
  document.querySelectorAll('.tab-content').forEach(t => t.classList.remove('active'));
  document.querySelectorAll('.stab').forEach(b => b.classList.remove('active'));
  document.getElementById(tabId)?.classList.add('active');
  if (btn) btn.classList.add('active');
}

function showFormAlert(el, msg, type) {
  if (!el) return;
  el.className = `alert alert-${type}`;
  el.textContent = msg;
  setTimeout(() => { el.className = 'alert hidden'; }, 4000);
}
function setText(id, val, prop='textContent') {
  const el = document.getElementById(id);
  if (el) el[prop] = val || '';
}
