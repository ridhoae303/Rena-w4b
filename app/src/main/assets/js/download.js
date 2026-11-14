(function () {
    if (window.__renaDl) { return; }
    window.__renaDl = true;
    var blobs = {};
    var anchors = {};
    var api = window.URL || window.webkitURL;

    function collect() {
        var now = Date.now();
        for (var key in blobs) {
            var entry = blobs[key];
            var age = now - entry.t;
            if ((!entry.pinned && age > 20000) || (entry.pinned && age > 900000)) {
                delete blobs[key];
            }
        }
    }

    if (api && api.createObjectURL && !api.__renaWrapped) {
        var originalCreate = api.createObjectURL;
        api.createObjectURL = function (object) {
            var url = originalCreate.apply(this, arguments);
            try {
                if (object && typeof object.size === 'number' && typeof object.slice === 'function') {
                    blobs[url] = { blob: object, t: Date.now(), pinned: false };
                    if (Object.keys(blobs).length > 40) { collect(); }
                }
            } catch (e) { }
            return url;
        };
        api.__renaWrapped = true;
    }

    function note(anchor) {
        try {
            var href = anchor.href || anchor.getAttribute('href') || '';
            if (href && (href.indexOf('blob:') === 0 || href.indexOf('data:') === 0)) {
                anchors[href] = {
                    name: anchor.getAttribute('download') || anchor.download || '',
                    type: anchor.type || anchor.getAttribute('type') || '',
                    t: Date.now()
                };
                var entry = blobs[href];
                if (entry) { entry.pinned = true; entry.t = Date.now(); }
            }
        } catch (e) { }
    }

    var originalClick = HTMLAnchorElement.prototype.click;
    HTMLAnchorElement.prototype.click = function () {
        note(this);
        return originalClick.apply(this, arguments);
    };

    var originalDispatch = EventTarget.prototype.dispatchEvent;
    EventTarget.prototype.dispatchEvent = function (event) {
        try {
            if (event && event.type === 'click' && this && this.tagName === 'A') { note(this); }
        } catch (e) { }
        return originalDispatch.apply(this, arguments);
    };

    document.addEventListener('click', function (event) {
        var node = event.target;
        while (node && node !== document && node.tagName !== 'A') { node = node.parentNode; }
        if (node && node !== document) { note(node); }
    }, true);

    window.__renaBlobMeta = function (url) {
        var entry = blobs[url];
        var anchor = anchors[url] || {};
        if (entry) { entry.pinned = true; entry.t = Date.now(); }
        return {
            name: anchor.name || '',
            type: (entry && entry.blob && entry.blob.type) || anchor.type || '',
            size: (entry && entry.blob && entry.blob.size) || -1,
            known: !!entry
        };
    };

    window.__renaReleaseBlob = function (url) {
        delete blobs[url];
        delete anchors[url];
    };

    function obtain(url) {
        var entry = blobs[url];
        if (entry && entry.blob) { return Promise.resolve(entry.blob); }
        return fetch(url).then(function (response) {
            if (!response.ok) { throw new Error('read'); }
            return response.blob();
        });
    }

    window.__renaReadBlob = function (id, url) {
        var bridge = window.RenaDownloads;
        if (!bridge) { return false; }
        obtain(url).then(function (blob) {
            if (!bridge.begin(id, blob.size, blob.type || '')) { return; }
            var offset = 0;
            var size = blob.size;
            var chunkSize = 262144;
            function pump() {
                if (bridge.isCancelled(id)) { return; }
                if (bridge.isPaused(id)) { setTimeout(pump, 300); return; }
                if (offset >= size) {
                    bridge.finish(id, true);
                    window.__renaReleaseBlob(url);
                    return;
                }
                var part = blob.slice(offset, Math.min(offset + chunkSize, size));
                var reader = new FileReader();
                reader.onload = function () {
                    try {
                        var text = String(reader.result || '');
                        var comma = text.indexOf(',');
                        var encoded = comma >= 0 ? text.substring(comma + 1) : text;
                        if (!bridge.write(id, encoded)) {
                            if (bridge.isCancelled(id)) { return; }
                            if (bridge.isPaused(id)) { setTimeout(pump, 300); return; }
                            bridge.finish(id, false);
                            return;
                        }
                        offset += part.size;
                        setTimeout(pump, 0);
                    } catch (e) {
                        bridge.finish(id, false);
                    }
                };
                reader.onerror = function () { bridge.finish(id, false); };
                reader.readAsDataURL(part);
            }
            pump();
        }).catch(function () {
            try { bridge.finish(id, false); } catch (e) { }
        });
        return true;
    };
})();
