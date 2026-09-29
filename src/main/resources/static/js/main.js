/* =========================================================
   DonaTrack — Interacciones mínimas compartidas
   Sin frameworks. API declarativa por atributos.
   Los indicadores de carga reflejan esperas reales:
   no hay latencia simulada.
   ========================================================= */

/* ---------- Modal genérico ---------- */
function openModal(modalId) {
    const overlay = document.getElementById(modalId);
    if (!overlay) return;
    overlay.classList.add('is-open');
    const focusTarget = overlay.querySelector('[data-autofocus]') || overlay.querySelector('input, button, select, textarea');
    if (focusTarget) focusTarget.focus();
    document.addEventListener('keydown', handleModalEsc);
}

function closeModal(modalId) {
    const overlay = document.getElementById(modalId);
    if (!overlay) return;
    overlay.classList.remove('is-open');
    document.removeEventListener('keydown', handleModalEsc);
}

function handleModalEsc(e) {
    if (e.key === 'Escape') {
        document.querySelectorAll('.modal-overlay.is-open').forEach((m) => m.classList.remove('is-open'));
    }
}

document.addEventListener('click', (e) => {
    const opener = e.target.closest('[data-open-modal]');
    if (opener) {
        openModal(opener.getAttribute('data-open-modal'));
    }
    const closer = e.target.closest('[data-close-modal]');
    if (closer) {
        const overlay = closer.closest('.modal-overlay');
        if (overlay) closeModal(overlay.id);
    }
    // Cerrar al clickear el fondo
    if (e.target.classList.contains('modal-overlay')) {
        closeModal(e.target.id);
    }
});

/* ---------- Toasts no intrusivos ---------- */
function showToast({ message, type = 'success', duration = 4000 }) {
    let region = document.querySelector('.toast-region');
    if (!region) {
        region = document.createElement('div');
        region.className = 'toast-region';
        region.setAttribute('role', 'status');
        region.setAttribute('aria-live', 'polite');
        document.body.appendChild(region);
    }

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.innerHTML = `
    <span class="toast-icon" aria-hidden="true">${type === 'error' ? '⚠' : '✓'}</span>
    <span class="toast-message">${message}</span>
    <button class="toast-close" aria-label="Cerrar notificación">✕</button>
  `;
    region.appendChild(toast);

    requestAnimationFrame(() => toast.classList.add('is-visible'));

    const remove = () => {
        toast.classList.remove('is-visible');
        setTimeout(() => toast.remove(), 220);
    };

    toast.querySelector('.toast-close').addEventListener('click', remove);
    if (duration) setTimeout(remove, duration);
}

// Dispara toasts declarados por atributos, ej: <button data-toast-success="Cambios guardados">
document.addEventListener('click', (e) => {
    const trigger = e.target.closest('[data-toast-success], [data-toast-error]');
    if (!trigger) return;
    if (trigger.hasAttribute('data-toast-success')) {
        showToast({ message: trigger.getAttribute('data-toast-success'), type: 'success' });
    } else if (trigger.hasAttribute('data-toast-error')) {
        showToast({ message: trigger.getAttribute('data-toast-error'), type: 'error' });
    }
});

/* ---------- Flash toast dejado por el servidor ----------
   El controller agrega un flash attribute y redirige (patrón PRG);
   el fragmento componentes::flash-toast lo deja en el DOM y acá se muestra. */
document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('[data-flash-toast]').forEach((el) => {
        showToast({
            message: el.getAttribute('data-mensaje'),
            type: el.getAttribute('data-tipo') || 'success',
        });
    });
});

/* ---------- Estado de carga al enviar un formulario ----------
   Espera real: el navegador está esperando la respuesta del servidor.
   Además evita el doble envío. */
document.addEventListener('submit', (e) => {
    const form = e.target;
    if (!(form instanceof HTMLFormElement) || form.noValidate === false && !form.checkValidity()) return;
    form.querySelectorAll('[data-loading-on-submit]').forEach((btn) => {
        btn.setAttribute('aria-busy', 'true');
        btn.disabled = true;
        btn.dataset.textoOriginal = btn.textContent.trim();
        btn.innerHTML = '<span class="spinner" aria-hidden="true"></span> ' + (btn.getAttribute('data-loading-texto') || 'Enviando…');
    });
});

/* ---------- Fragmento diferido ----------
   Para secciones cuya API es lenta: la página se renderiza con un skeleton
   y la sección se pide aparte. Si falla, el error se muestra en la sección. */
async function cargarFragmento(el) {
    const url = el.getAttribute('data-lazy-fragment');
    el.setAttribute('aria-busy', 'true');
    try {
        const resp = await fetch(url, { headers: { 'X-Requested-With': 'fetch' } });
        if (!resp.ok) throw new Error(resp.status);
        el.innerHTML = await resp.text();
    } catch (err) {
        el.innerHTML = '<div class="banner banner-warning" role="alert">' +
            '<p><strong>No pudimos cargar esta sección</strong>' +
            'El servicio no respondió. Actualizá la página para reintentar.</p></div>';
    } finally {
        el.removeAttribute('aria-busy');
    }
}

document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('[data-lazy-fragment]').forEach(cargarFragmento);
});

/* ---------- Menú móvil (navbar pública / donante) ---------- */
document.addEventListener('click', (e) => {
    const toggle = e.target.closest('[data-nav-toggle]');
    if (!toggle) return;
    const links = document.querySelector('.navbar-links');
    if (links) links.classList.toggle('is-open-mobile');
});

/* ---------- Chips seleccionables (ej. storage suggestion) ---------- */
document.addEventListener('click', (e) => {
    const chip = e.target.closest('.select-chip');
    if (!chip) return;
    const group = chip.closest('.select-chip-group');
    if (group && group.hasAttribute('data-single-select')) {
        group.querySelectorAll('.select-chip').forEach((c) => c.setAttribute('aria-pressed', 'false'));
    }
    chip.setAttribute('aria-pressed', chip.getAttribute('aria-pressed') === 'true' ? 'false' : 'true');
});

/* ---------- Tabs simples ---------- */
document.addEventListener('click', (e) => {
    const tab = e.target.closest('.tab');
    if (!tab) return;
    const tabs = tab.closest('.tabs');
    if (!tabs) return;
    tabs.querySelectorAll('.tab').forEach((t) => t.setAttribute('aria-selected', 'false'));
    tab.setAttribute('aria-selected', 'true');

    const panelId = tab.getAttribute('aria-controls');
    if (!panelId) return;
    const panelGroup = document.querySelectorAll(`[data-tabpanel-group="${tabs.id}"]`);
    panelGroup.forEach((p) => { p.hidden = p.id !== panelId; });
});
