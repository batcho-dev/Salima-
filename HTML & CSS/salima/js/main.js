// ─── SALIMA SHARED JAVASCRIPT ──────────────────────────

// Toast notification
function showToast(msg, duration = 2800) {
  let t = document.getElementById('toast');
  if (!t) {
    t = document.createElement('div');
    t.id = 'toast';
    t.className = 'toast';
    document.body.appendChild(t);
  }
  t.textContent = msg;
  t.classList.add('show');
  setTimeout(() => t.classList.remove('show'), duration);
}

// Language toggle
function toggleLanguage() {
  const btn = document.querySelector('.nav-lang');
  if (btn) {
    btn.textContent = btn.textContent.includes('EN') ? '🌐 FR' : '🌐 EN';
    showToast(btn.textContent.includes('FR') ? 'French mode (demo)' : 'English mode');
  }
}

// OTP auto-advance (for any page with .otp-box)
function initOtp() {
  const boxes = document.querySelectorAll('.otp-box');
  boxes.forEach((box, i) => {
    box.addEventListener('input', () => {
      if (box.value && i < boxes.length - 1) boxes[i + 1].focus();
    });
    box.addEventListener('keydown', (e) => {
      if (e.key === 'Backspace' && !box.value && i > 0) boxes[i - 1].focus();
    });
  });
}

// Animate gauge fill
function animateGauge(id, pct) {
  const el = document.getElementById(id);
  if (!el) return;
  setTimeout(() => { el.style.width = pct + '%'; }, 200);
}

// Animate counter
function animateCounter(id, target, suffix = '') {
  const el = document.getElementById(id);
  if (!el) return;
  const duration = 1400;
  const start = performance.now();
  const step = (now) => {
    const progress = Math.min((now - start) / duration, 1);
    const ease = 1 - Math.pow(1 - progress, 3);
    el.textContent = Math.floor(ease * target).toLocaleString() + suffix;
    if (progress < 1) requestAnimationFrame(step);
  };
  requestAnimationFrame(step);
}

// Scroll reveal
function initScrollReveal() {
  const items = document.querySelectorAll('[data-reveal]');
  if (!items.length) return;
  const observer = new IntersectionObserver((entries) => {
    entries.forEach(e => {
      if (e.isIntersecting) {
        e.target.classList.add('fade-up');
        observer.unobserve(e.target);
      }
    });
  }, { threshold: 0.1 });
  items.forEach(el => observer.observe(el));
}

// Format currency
function formatFCFA(amount) {
  return amount.toLocaleString() + ' FCFA';
}

// Get current user
function getCurrentUser() {
  try {
    const user = JSON.parse(localStorage.getItem('salima_user'));
    if (user) return user;
  } catch(e) {}
  return {
    name: localStorage.getItem('salima_member') || 'Guest',
    phone: localStorage.getItem('salima_phone') || '',
    plan: localStorage.getItem('salima_plan') || 'standard'
  };
}

// Initialize on every page
document.addEventListener('DOMContentLoaded', () => {
  initOtp();
  initScrollReveal();
  
  // Initialize language toggle if element exists
  const langBtn = document.querySelector('.nav-lang');
  if (langBtn && !langBtn.hasListener) {
    langBtn.addEventListener('click', toggleLanguage);
    langBtn.hasListener = true;
  }
});