
// ══════════════════════════════════════════════════════════════════════════════
// GESTIÓN DE SESIÓN — Aviso de expiración próxima
// ══════════════════════════════════════════════════════════════════════════════

(function initSessionWarning() {
    const SESSION_TIMEOUT_MS  = 2 * 60 * 60 * 1000; // 2 horas (igual que application.properties)
    const WARN_BEFORE_MS      = 5 * 60 * 1000;       // avisar 5 minutos antes
    const sessionStart = parseInt(sessionStorage.getItem('obratechSessionStart') || Date.now(), 10);
    sessionStorage.setItem('obratechSessionStart', sessionStart);

    const timeUntilWarn = (sessionStart + SESSION_TIMEOUT_MS - WARN_BEFORE_MS) - Date.now();

    if (timeUntilWarn > 0) {
        setTimeout(function () {
            const toast = document.getElementById('session-warning-toast');
            if (toast) {
                toast.classList.remove('translate-y-full', 'opacity-0');
                toast.classList.add('translate-y-0', 'opacity-100');
            }
        }, timeUntilWarn);
    }
})();

// ══════════════════════════════════════════════════════════════════════════════
// VALIDACIÓN DE FORMULARIO DE LOGIN 
// ══════════════════════════════════════════════════════════════════════════════

document.addEventListener('DOMContentLoaded', function () {
    const loginForm = document.getElementById('login-form');
    if (!loginForm) return;

    loginForm.addEventListener('submit', function (e) {
        const username = loginForm.querySelector('[name="username"]').value.trim();
        const password = loginForm.querySelector('[name="password"]').value;
        const errorBox = document.getElementById('client-validation-error');

        if (!username || !password) {
            e.preventDefault();
            showClientError(errorBox, 'Por favor completa todos los campos.');
            return;
        }
        if (!isValidEmail(username)) {
            e.preventDefault();
            showClientError(errorBox, 'Ingresa un correo electrónico válido.');
            return;
        }


        // Mostrar spinner en el botón
        const btn = loginForm.querySelector('[type="submit"]');
        if (btn) {
            btn.disabled = true;
            btn.innerHTML = '<span class="inline-block animate-spin mr-2">⏳</span> Ingresando...';
        }
    });
});

// ══════════════════════════════════════════════════════════════════════════════
//  VALIDACIÓN DE FORMULARIO DE REGISTRO 
// ══════════════════════════════════════════════════════════════════════════════

document.addEventListener('DOMContentLoaded', function () {
    const regForm = document.getElementById('registro-form');
    if (!regForm) return;

    // Barra de fortaleza de contraseña
    const passwordInput = regForm.querySelector('[name="password"]');
    const strengthBar    = document.getElementById('password-strength-bar');
    const strengthText   = document.getElementById('password-strength-text');

    if (passwordInput && strengthBar) {
        passwordInput.addEventListener('input', function () {
            const score = calcPasswordStrength(this.value);
            updateStrengthBar(strengthBar, strengthText, score);
        });
    }

    regForm.addEventListener('submit', function (e) {
        const username = regForm.querySelector('[name="username"]').value.trim();
        const password = regForm.querySelector('[name="password"]').value;
        const role     = regForm.querySelector('[name="role"]').value;
        const errorBox = document.getElementById('registro-error');

        clearClientError(errorBox);

        if (!username || !isValidEmail(username)) {
            e.preventDefault();
            showClientError(errorBox, 'Ingresa un correo electrónico válido.');
            return;
        }
        if (!password || password.length < 8) {
            e.preventDefault();
            showClientError(errorBox, 'La contraseña debe tener al menos 8 caracteres.');
            return;
        }
        if (role === 'none' || !role) {
            e.preventDefault();
            showClientError(errorBox, 'Selecciona un tipo de usuario.');
            return;
        }

        const btn = regForm.querySelector('[type="submit"]');
        if (btn) {
            btn.disabled = true;
            btn.innerHTML = '<span class="inline-block animate-spin mr-2">⏳</span> Registrando...';
        }
    });
});

// ══════════════════════════════════════════════════════════════════════════════
//  TOGGLE VISIBILIDAD DE CONTRASEÑA
// ══════════════════════════════════════════════════════════════════════════════

function togglePasswordVisibility(button) {
    const wrapper = button.closest('.password-wrapper') || button.parentElement;
    const input   = wrapper.querySelector('input[type="password"], input[type="text"]');
    const icon    = button.querySelector('span');
    if (!input) return;
    if (input.type === 'password') {
        input.type = 'text';
        if (icon) icon.textContent = 'visibility_off';
    } else {
        input.type = 'password';
        if (icon) icon.textContent = 'visibility';
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  UTILIDADES INTERNAS
// ══════════════════════════════════════════════════════════════════════════════

function isValidEmail(email) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

function showClientError(box, message) {
    if (!box) return;
    box.textContent = message;
    box.classList.remove('hidden');
    box.scrollIntoView({ behavior: 'smooth', block: 'center' });
}

function clearClientError(box) {
    if (!box) return;
    box.textContent = '';
    box.classList.add('hidden');
}

function calcPasswordStrength(password) {
    let score = 0;
    if (password.length >= 8)  score++;
    if (password.length >= 12) score++;
    if (/[A-Z]/.test(password)) score++;
    if (/[0-9]/.test(password)) score++;
    if (/[^A-Za-z0-9]/.test(password)) score++;
    return score; // 0-5
}

function updateStrengthBar(bar, text, score) {
    const levels = [
        { label: '',          color: 'bg-gray-600',  width: '0%' },
        { label: 'Muy débil', color: 'bg-red-500',   width: '20%' },
        { label: 'Débil',     color: 'bg-orange-500',width: '40%' },
        { label: 'Regular',   color: 'bg-yellow-400',width: '60%' },
        { label: 'Fuerte',    color: 'bg-green-400', width: '80%' },
        { label: 'Muy fuerte',color: 'bg-green-600', width: '100%'},
    ];
    const lvl = levels[Math.min(score, 5)];
    bar.style.width = lvl.width;
    bar.className   = `h-full rounded-full transition-all duration-300 ${lvl.color}`;
    if (text) text.textContent = lvl.label;
}

// ══════════════════════════════════════════════════════════════════════════════
// AUTO-DISMISS de alertas de éxito
// ══════════════════════════════════════════════════════════════════════════════

document.addEventListener('DOMContentLoaded', function () {
    const successAlerts = document.querySelectorAll('[data-auto-dismiss]');
    successAlerts.forEach(function (el) {
        const delay = parseInt(el.dataset.autoDismiss || '4000', 10);
        setTimeout(function () {
            el.style.transition = 'opacity 0.5s';
            el.style.opacity = '0';
            setTimeout(() => el.remove(), 500);
        }, delay);
    });
});

// ══════════════════════════════════════════════════════════════════════════════
//  CONFIRMAR ACCIONES DESTRUCTIVAS
// ══════════════════════════════════════════════════════════════════════════════

document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('[data-confirm]').forEach(function (el) {
        el.addEventListener('click', function (e) {
            const msg = el.dataset.confirm || '¿Estás seguro de esta acción?';
            if (!confirm(msg)) e.preventDefault();
        });
    });
});
