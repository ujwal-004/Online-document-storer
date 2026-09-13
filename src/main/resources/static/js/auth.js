/* =============================================
   DOCSTORER — AUTH PAGES
   ============================================= */
document.addEventListener('DOMContentLoaded', () => {
  applyTheme();
  if (Auth.isLoggedIn()) { window.location.href = '/dashboard.html'; return; }
  const lf = document.getElementById('loginForm');
  const rf = document.getElementById('registerForm');
  if (lf) lf.addEventListener('submit', handleLogin);
  if (rf) rf.addEventListener('submit', handleRegister);
  const pwInput = document.getElementById('password');
  if (pwInput && document.getElementById('pwStrength')) pwInput.addEventListener('input', checkPasswordStrength);
  const tagsInput = document.getElementById('uploadTags');
  if (tagsInput) tagsInput.addEventListener('input', updateTagPreview);
});

async function handleLogin(e) {
  e.preventDefault();
  clearErrors();
  const email = document.getElementById('emailOrUsername').value.trim();
  const password = document.getElementById('password').value;
  if (!email) { showError('emailErr','Email or username is required'); return; }
  if (!password) { showError('pwErr','Password is required'); return; }
  setLoading('loginBtn', true);
  try {
    const res = await fetch('/api/auth/login', {
      method:'POST',
      headers:{'Content-Type':'application/json'},
      body: JSON.stringify({ emailOrUsername: email, password, rememberMe: document.getElementById('rememberMe')?.checked })
    });
    const data = await readJsonResponse(res);
    if (!res.ok) throw new Error(data?.message || `Login failed (HTTP ${res.status})`);
    if (!data?.data?.accessToken) throw new Error('Login response did not include a session token. Please try again.');
    Auth.save(data.data);
    showAlert('Login successful! Redirecting...', 'success');
    setTimeout(() => window.location.href = '/dashboard.html', 800);
  } catch (err) {
    showAlert(err.message || 'Login failed. Please try again.', 'error');
  } finally { setLoading('loginBtn', false); }
}

async function handleRegister(e) {
  e.preventDefault();
  clearErrors();
  const firstName = document.getElementById('firstName').value.trim();
  const lastName = document.getElementById('lastName').value.trim();
  const username = document.getElementById('username').value.trim();
  const email = document.getElementById('email').value.trim();
  const password = document.getElementById('password').value;
  const confirm = document.getElementById('confirmPassword').value;
  const agree = document.getElementById('agreeTerms').checked;
  let valid = true;
  if (!firstName) { showError('firstNameErr','First name required'); valid=false; }
  if (!lastName)  { showError('lastNameErr','Last name required'); valid=false; }
  if (!username || username.length < 3) { showError('usernameErr','Username min 3 chars'); valid=false; }
  if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) { showError('emailErr','Valid email required'); valid=false; }
  if (!password || password.length < 8) { showError('pwErr','Password must be at least 8 characters'); valid=false; }
  else if (!/(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/.test(password)) {
    showError('pwErr','Password needs an uppercase letter, lowercase letter, and number'); valid=false;
  }
  if (password !== confirm) { showError('confirmErr','Passwords do not match'); valid=false; }
  if (!agree) { showAlert('You must agree to terms','error'); valid=false; }
  if (!valid) return;
  setLoading('registerBtn', true);
  try {
    const res = await fetch('/api/auth/register', {
      method:'POST',
      headers:{'Content-Type':'application/json'},
      body: JSON.stringify({ firstName, lastName, username, email, password })
    });
    const data = await readJsonResponse(res);
    if (!res.ok) {
      if (data?.data && typeof data.data === 'object') {
        showValidationErrors(data.data);
        throw new Error('Please correct the highlighted fields.');
      }
      throw new Error(data?.message || `Registration failed (HTTP ${res.status})`);
    }
    if (!data?.data?.accessToken) throw new Error('Registration response did not include a session token. Please sign in.');
    Auth.save(data.data);
    showAlert('Account created! Redirecting...', 'success');
    setTimeout(() => window.location.href = '/dashboard.html', 800);
  } catch (err) {
    showAlert(err.message || 'Registration failed.', 'error');
  } finally { setLoading('registerBtn', false); }
}

function checkPasswordStrength() {
  const pw = document.getElementById('password').value;
  const bar = document.getElementById('pwStrength');
  if (!bar) return;
  let score = 0;
  if (pw.length >= 8) score++;
  if (/[A-Z]/.test(pw)) score++;
  if (/[0-9]/.test(pw)) score++;
  if (/[^A-Za-z0-9]/.test(pw)) score++;
  const levels = ['#ef4444','#f97316','#eab308','#10b981'];
  const widths = ['25%','50%','75%','100%'];
  bar.style.background = levels[score-1] || '#e5e7eb';
  bar.style.width = widths[score-1] || '0%';
  bar.style.height = '4px';
}

function showError(id, msg) {
  const el = document.getElementById(id);
  if (el) el.textContent = msg;
}
function clearErrors() {
  document.querySelectorAll('.field-error').forEach(el => el.textContent = '');
  const alert = document.getElementById('authAlert');
  if (alert) { alert.className = 'alert hidden'; alert.textContent = ''; }
}
function showValidationErrors(errors) {
  const fieldIds = {
    firstName: 'firstNameErr', lastName: 'lastNameErr', username: 'usernameErr',
    email: 'emailErr', password: 'pwErr'
  };
  Object.entries(errors).forEach(([field, message]) => {
    const id = fieldIds[field];
    if (id) showError(id, message);
  });
}
function showAlert(msg, type='info') {
  const el = document.getElementById('authAlert');
  if (!el) return;
  el.className = `alert alert-${type}`;
  el.textContent = msg;
}
function setLoading(btnId, loading) {
  const btn = document.getElementById(btnId);
  if (!btn) return;
  btn.disabled = loading;
  btn.querySelector('.btn-text').classList.toggle('hidden', loading);
  btn.querySelector('.btn-spinner').classList.toggle('hidden', !loading);
}

async function readJsonResponse(response) {
  const body = await response.text();
  if (!body) return null;
  try {
    return JSON.parse(body);
  } catch {
    throw new Error(`The server returned an invalid response (HTTP ${response.status})`);
  }
}
function applyTheme() {
  const t = localStorage.getItem('theme') || 'light';
  document.documentElement.setAttribute('data-theme', t);
}
