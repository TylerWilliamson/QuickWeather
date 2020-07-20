'use strict';

(function() {

    /*Based on https://github.com/mwasil/Leaflet.Rainviewer/*/
    L.Control.Rainviewer = L.Control.extend({
        options: {
            position: 'topleft',
            animationInterval: 500
        },

        onAdd: function (map) {
            this.timestamps = [];

            this.map = map;
            this.radarLayers = L.featureGroup().addTo(map);
            this.radarLayers.options = { attribution: '<a href="https://rainviewer.com" target="_blank">RainViewer</a>' };

            this.container = L.DomUtil.create('div', 'leaflet-control-rainviewer leaflet-bar leaflet-control');

            this.playPauseControl = L.DomUtil.create('a', 'leaflet-control-rainviewer-button leaflet-bar-part', this.container);
            this.playPauseControl.title = 'Play';
            L.DomEvent.on(this.playPauseControl, 'click', this.playStop, this);

            let playPauseButtonContainer = L.DomUtil.create('div', '', this.playPauseControl);
            playPauseButtonContainer.style = 'padding:4px';

            this.playPauseButton = L.DomUtil.create('img', '', playPauseButtonContainer);
            this.playPauseButton.src = 'play_arrow.svg';
            this.playPauseButton.style = 'height:22px';

            return this.container;
        },

        load: function() {
            this.stop();
            this.animationPosition = 0;

            this.addedLayers = [];
            this.radarLayers.eachLayer(function (l) {
                l.remove();
            });

            for (let [k,v] of Object.entries(this.map.getPanes())) {
                if (v.style.display === "block") {
                    v.style.display = "none";
                }
            }

            let apiRequest = new XMLHttpRequest();
            apiRequest.open("GET", "https://tilecache.rainviewer.com/api/maps.json", true);
            apiRequest.onload = function (e) {
                this.timestamps = JSON.parse(apiRequest.response);
                this.changeRadarPosition(-1);
                this.preload();
            }.bind(this);
            apiRequest.send();
        },

        addLayer: function(ts) {
            let tsStr = 't'+ts;
            if (!this.map.getPane(tsStr)) {
                this.map.createPane(tsStr).style.display='none';
            }

            if (!this.addedLayers.includes(tsStr)) {
                this.addedLayers.push(tsStr);
                this.radarLayers.addLayer(new L.TileLayer('https://tilecache.rainviewer.com/v2/radar/' + ts + '/512/{z}/{x}/{y}/2/1_1.png', {
                    detectRetina: true,
                    opacity: 0.5,
                    transparent: true,
                    zIndex: 300,
                    pane: tsStr
                }));
            }
        },

        changeRadarPosition: function(position, preloadOnly) {
            while (position >= this.timestamps.length) {
                position -= this.timestamps.length;
            }

            while (position < 0) {
                position += this.timestamps.length;
            }

            this.currentTimestamp = this.timestamps[this.animationPosition];
            this.nextTimestamp = this.timestamps[position];

            this.addLayer(this.nextTimestamp);

            if (preloadOnly) {
                return;
            }

            this.animationPosition = position;

            this.map.getPane('t' + this.nextTimestamp).style.display = 'block';

            if (this.addedLayers.includes('t' + this.currentTimestamp)) {
                this.map.getPane('t' + this.currentTimestamp).style.display = 'none';
            }
        },

        preload: function() {
            for (let i=0;i<this.timestamps.length;i++) {
                this.changeRadarPosition(i, true);
            }
        },

        stop: function() {
            this.playPauseControl.title = 'Play';
            this.playPauseButton.src = 'play_arrow.svg';
            if (this.animationTimer) {
                clearTimeout(this.animationTimer);
                this.animationTimer = false;
                return true;
            }
            return false;
        },

        play: function() {
            this.playPauseControl.title = 'Pause';
            this.playPauseButton.src = 'pause.svg';
            this.changeRadarPosition(this.animationPosition + 1);

            this.animationTimer = setTimeout(function(){ this.play() }.bind(this), this.options.animationInterval);
        },

        playStop: function() {
            if (!this.stop()) {
               this.play();
            }
        }
    });

    var radar = {
        getParams: function () {
            this.params = {};

            if (window.location.href.indexOf('#') > -1) {
                window.location.href.split('#')[1].split('&').forEach(function (v) { let t = v.split('='); this.params[t[0]] = t[1]; }.bind(this));
            }

            this.params.lat = this.params.lat || 34;
            this.params.lon = this.params.lon || -84;
            this.params.theme = this.params.theme || "light";
            this.params.ts = this.params.ts || 1;
        },
        updateMap: function () {
            var cachedParams = this.params;

            this.getParams();

            if (this.params.theme !== cachedParams.theme) {
                this.updateTheme();
            }

            this.map.setView([radar.params.lat,radar.params.lon], 8);

            this.radarMap.load();
        },
        updateTheme: function () {
            this.map.getContainer().classList.remove("light");
            this.map.getContainer().classList.remove("dark");
            this.map.getContainer().classList.add(radar.params.theme);

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

        radar.updateTheme();

        radar.radarMap = new L.Control.Rainviewer().addTo(radar.map);

        let zoomControl = radar.map.zoomControl.getContainer().getElementsByTagName("a");
        for (let i = 0; i < zoomControl.length; i++) {
            zoomControl[i].removeAttribute("href");
        }

        radar.updateMap();
    });

    window.addEventListener('popstate', () => { radar.updateMap(); });
})();