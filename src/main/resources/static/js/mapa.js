(function () {
    const CENTRO_POR_DEFECTO = [-34.72, -58.28];
    const ZOOM_POR_DEFECTO = 10;
    const ZOOM_UN_PUNTO = 14;
    const ZOOM_ENFOCADO = 15;

    const marcadores = new Map();
    let enfocado = null;

    function posicionValida(lat, lon) {
        return Number.isFinite(lat) && Number.isFinite(lon) && !(lat === 0 && lon === 0)
            && Math.abs(lat) <= 90 && Math.abs(lon) <= 180;
    }

    function iniciarMapa(contenedor) {
        const pines = Array.from(contenedor.querySelectorAll('.map-pin[data-lat][data-lon]'));

        const lienzo = document.createElement('div');
        lienzo.className = 'map-leaflet';
        contenedor.prepend(lienzo);

        const mapa = L.map(lienzo, { zoomControl: false });
        L.control.zoom({ position: 'topright' }).addTo(mapa);
        L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
            maxZoom: 19,
            attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
        }).addTo(mapa);

        const puntos = [];
        pines.forEach((pin) => {
            const lat = parseFloat(pin.dataset.lat);
            const lon = parseFloat(pin.dataset.lon);
            pin.remove();
            if (!posicionValida(lat, lon)) return;

            pin.hidden = false;
            const icono = L.divIcon({ className: 'map-marker', html: pin.outerHTML, iconSize: [0, 0] });
            const marcador = L.marker([lat, lon], { icon: icono, interactive: false, keyboard: false }).addTo(mapa);
            if (pin.dataset.id) {
                marcadores.set(pin.dataset.id, { mapa, marcador, contenedor });
            }
            puntos.push([lat, lon]);
        });

        if (puntos.length === 0) {
            mapa.setView(CENTRO_POR_DEFECTO, ZOOM_POR_DEFECTO);
        } else if (puntos.length === 1) {
            mapa.setView(puntos[0], ZOOM_UN_PUNTO);
        } else {
            mapa.fitBounds(puntos, { padding: [48, 48], maxZoom: ZOOM_UN_PUNTO });
        }
    }

    function enfocar(id) {
        const destino = marcadores.get(id);
        if (!destino) return;
        const sinAnimacion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

        if (enfocado) {
            enfocado.getElement()?.querySelector('.map-pin')?.classList.remove('map-pin-active');
            enfocado.setZIndexOffset(0);
        }
        destino.marcador.getElement()?.querySelector('.map-pin')?.classList.add('map-pin-active');
        destino.marcador.setZIndexOffset(1000);
        enfocado = destino.marcador;

        destino.contenedor.scrollIntoView({ behavior: sinAnimacion ? 'auto' : 'smooth', block: 'nearest' });
        if (sinAnimacion) {
            destino.mapa.setView(destino.marcador.getLatLng(), ZOOM_ENFOCADO);
        } else {
            destino.mapa.flyTo(destino.marcador.getLatLng(), ZOOM_ENFOCADO, { duration: 0.6 });
        }
    }

    function iniciar() {
        if (typeof L === 'undefined') return;
        document.querySelectorAll('[data-mapa]').forEach(iniciarMapa);

        document.addEventListener('click', (e) => {
            const boton = e.target.closest('[data-mapa-enfocar]');
            if (boton) enfocar(boton.dataset.mapaEnfocar);
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', iniciar);
    } else {
        iniciar();
    }
})();
