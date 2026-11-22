(function () {
    const CENTRO_POR_DEFECTO = [-34.72, -58.28];
    const ZOOM_POR_DEFECTO = 10;
    const ZOOM_UN_PUNTO = 14;
    const ZOOM_ENFOCADO = 15;

    const marcadores = new Map();
    const marcadoresPendientes = new Map();
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
            const pinClone = pin.cloneNode(true);
            pin.remove();

            if (!posicionValida(lat, lon)) {
                if (pin.dataset.id) {
                    marcadoresPendientes.set(pin.dataset.id, { mapa, pinElement: pinClone, contenedor });
                }
                return;
            }

            pinClone.hidden = false;
            const icono = L.divIcon({ className: 'map-marker', html: pinClone.outerHTML, iconSize: [0, 0] });
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

        activarSincronizacion(contenedor);
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

    function actualizarElemento(id, lat, lon, textoFecha) {
        if (!id) return;
        const idStr = String(id);
        let registro = marcadores.get(idStr);

        if (!registro && marcadoresPendientes.has(idStr) && posicionValida(lat, lon)) {
            const pend = marcadoresPendientes.get(idStr);
            pend.pinElement.hidden = false;
            const icono = L.divIcon({ className: 'map-marker', html: pend.pinElement.outerHTML, iconSize: [0, 0] });
            const marcador = L.marker([lat, lon], { icon: icono, interactive: false, keyboard: false }).addTo(pend.mapa);
            registro = { mapa: pend.mapa, marcador, contenedor: pend.contenedor };
            marcadores.set(idStr, registro);
            marcadoresPendientes.delete(idStr);
        }

        if (registro && posicionValida(lat, lon)) {
            const nuevaPos = [lat, lon];
            registro.marcador.setLatLng(nuevaPos);
            if (enfocado === registro.marcador) {
                const sinAnimacion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
                if (sinAnimacion) {
                    registro.mapa.setView(nuevaPos, registro.mapa.getZoom());
                } else {
                    registro.mapa.panTo(nuevaPos, { animate: true, duration: 0.5 });
                }
            }
        }

        if (textoFecha) {
            document.querySelectorAll(`[data-mapa-actualizacion="${idStr}"]`).forEach((el) => {
                const sufijo = el.textContent.includes('hs') && !textoFecha.includes('hs') ? ' hs' : '';
                el.textContent = textoFecha + sufijo;
            });
        }

        if (posicionValida(lat, lon)) {
            document.querySelectorAll(`[data-mapa-coord="${idStr}"]`).forEach((el) => {
                el.textContent = `${lat.toFixed(4)}, ${lon.toFixed(4)}`;
            });
        }
    }

    function procesarItems(items) {
        if (!Array.isArray(items)) return;
        items.forEach((item) => {
            const id = item.id !== undefined ? item.id : item.idCamion;
            const lat = Number(item.latitud);
            const lon = Number(item.longitud);
            const fecha = item.ultimaActualizacionTexto || (item.horario ? item.horario.replace('T', ' ').substring(0, 16) : null);
            actualizarElemento(id, lat, lon, fecha);
        });
    }

    function activarSincronizacion(contenedor) {
        const url = contenedor.dataset.mapaUrl;
        if (!url) return;

        async function sincronizar() {
            try {
                const res = await fetch(url, { headers: { 'Accept': 'application/json' } });
                if (res.ok) {
                    const data = await res.json();
                    procesarItems(data);
                }
            } catch (e) {
                // Silencioso ante fallas de red puntuales
            }
        }

        setInterval(sincronizar, 2500);

        try {
            const host = window.location.hostname || 'localhost';
            const wsUrl = `ws://${host}:8083/logistica`;
            const ws = new WebSocket(wsUrl);

            ws.onopen = () => {
                ws.send('CONNECT\naccept-version:1.1,1.2\nheart-beat:10000,10000\n\n\0');
            };

            ws.onmessage = (event) => {
                const mensaje = event.data;
                if (typeof mensaje !== 'string') return;

                if (mensaje.startsWith('CONNECTED')) {
                    ws.send('SUBSCRIBE\nid:sub-0\ndestination:/monitoreo/camiones\n\n\0');
                } else if (mensaje.startsWith('MESSAGE')) {
                    const partes = mensaje.split('\n\n');
                    if (partes.length >= 2) {
                        const cuerpo = partes.slice(1).join('\n\n').replace(/\0.*$/, '').trim();
                        try {
                            const dato = JSON.parse(cuerpo);
                            if (dato) {
                                procesarItems(Array.isArray(dato) ? dato : [dato]);
                                sincronizar();
                            }
                        } catch (err) {}
                    }
                }
            };

            ws.onerror = () => {};
            ws.onclose = () => {};
        } catch (err) {}
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
