'use strict';

(function() {
    var radar = {
        getParams: function () {
            this.params = {};

            if (window.location.href.indexOf('#') > -1) {
                window.location.href.split('#')[1].split('&').forEach(function (v) { let t = v.split('='); this.params[t[0]] = t[1]; }.bind(this));
            }

            this.params.lat = this.params.lat || 34;
            this.params.lon = this.params.lon || -84;
            this.params.theme = this.params.theme || 'light';
            this.params.ts = this.params.ts || 1;
            this.params.tz = this.params.tz || 'America/New_York';
            this.params.mc = this.params.mc || 'D81B60';
        },
        updateMap: function () {
            var cachedParams = this.params;

            this.getParams();

            if (this.params.theme !== cachedParams.theme) {
                this.updateTheme();
            }

            this.map.setView([radar.params.lat,radar.params.lon], 1);
        },
        updateTheme: function () {
            this.map.getContainer().classList.remove("light");
            this.map.getContainer().classList.remove("dark");
            this.map.getContainer().classList.add(radar.params.theme);

            document.getElementsByTagName('body')[0].classList.remove("light");
            document.getElementsByTagName('body')[0].classList.remove("dark");
            document.getElementsByTagName('body')[0].classList.add(radar.params.theme);

            if (this.baseMap) {
                this.map.removeLayer(this.baseMap);
            }

            this.baseMap = L.mapboxGL({
                style: 'https://tiles.stadiamaps.com/styles/' + (this.params.theme == 'dark' ? 'alidade_smooth_dark' : 'alidade_smooth') + '.json',
                attribution: '&copy; <a href="https://stadiamaps.com/">Stadia Maps</a>, &copy; <a href="https://openmaptiles.org/">OpenMapTiles</a>, &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors'
            });

            this.baseMap.on('add',() => {
                this.baseMap.getMapboxMap().on('load',() => {
                     this.baseMap.getMapboxMap().autodetectLanguage();
                     this.setMapTextSize(1.2 * this.params.ts);
                });
            });
            this.baseMap.addTo(this.map);

            this.markerIcon.className = radar.params.theme;
        },
        setMapTextSize: function (ratio) {
            this.baseMap.getMapboxMap().getStyle().layers.forEach((v) => {
                if (v.type === "symbol") {
                    this.baseMap.getMapboxMap().setPaintProperty(v.id,"text-color", this.params.theme == "dark" ? "rgba(255,255,255,0.9)" : "rgba(0,0,0,0.9)");

                    let ts = this.baseMap.getMapboxMap().getLayoutProperty(v.id,"text-size");

                    if (isNaN(ts)) {
                        ts.stops.forEach((v,i,arr) => {
                            arr[i][1] = Math.round(v[1] * ratio);
                        });
                        this.baseMap.getMapboxMap().setLayoutProperty(v.id,"text-size",ts);
                    } else {
                        this.baseMap.getMapboxMap().setLayoutProperty(v.id,"text-size",Math.round(ts * ratio));
                    }
                }
            });
        }
    };

    window.addEventListener('load', () => {
        radar.getParams();

        radar.map = L.map('mapid', {});

        let zoomControl = radar.map.zoomControl.getContainer().getElementsByTagName("a");
        for (let i = 0; i < zoomControl.length; i++) {
            zoomControl[i].removeAttribute("href");
        }

        radar.updateMap();

        radar.map.on('click', function(e) {
            radar.marker.setLatLng(e.latlng);

            Android.setWeatherLocation(e.latlng.lat + "," + e.latlng.lng);
        });

        radar.markerIcon = L.divIcon({
            className: 'marker',
            iconSize: [48, 48],
            iconAnchor: [24, 48],
            html: '<svg xmlns="http://www.w3.org/2000/svg" height="48" width="48"><path d="M22.5 27.5H25.5V21.5H31.5V18.5H25.5V12.5H22.5V18.5H16.5V21.5H22.5ZM24 44Q15.95 37.15 11.975 31.275Q8 25.4 8 20.4Q8 12.9 12.825 8.45Q17.65 4 24 4Q30.35 4 35.175 8.45Q40 12.9 40 20.4Q40 25.4 36.025 31.275Q32.05 37.15 24 44Z"/></svg>'
        });

        radar.marker = new L.marker([0,-30]).addTo(radar.map);
        radar.marker.setIcon(radar.markerIcon)

        document.getElementsByClassName('marker')[0].style.fill = '#' + radar.params.mc;
        radar.updateTheme();
    });

    window.addEventListener('popstate', () => { radar.updateMap(); });
})();