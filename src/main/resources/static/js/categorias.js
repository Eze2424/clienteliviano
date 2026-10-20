(function () {
    function cargarSubcategorias(bloque) {
        const categoria = bloque.querySelector('[data-categoria]');
        const subcategoria = bloque.querySelector('[data-subcategoria]');
        if (!categoria || !subcategoria) return;
        const elegida = (window.CATEGORIAS || []).find((c) => String(c.id) === categoria.value);

        subcategoria.innerHTML = '<option value="">Elegí una subcategoría</option>';
        (elegida ? elegida.subcategorias : []).forEach((s) => {
            const opt = document.createElement('option');
            opt.value = s.id;
            opt.textContent = s.nombre;
            subcategoria.appendChild(opt);
        });
        subcategoria.disabled = !elegida;
    }

    window.cargarSubcategorias = cargarSubcategorias;

    document.addEventListener('change', (e) => {
        if (e.target.matches('[data-categoria]')) {
            cargarSubcategorias(e.target.closest('[data-bloque], form'));
        }
    });
})();
