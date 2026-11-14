(function () {
    if (window !== window.top) { return; }
    if (window.__renaInspector) { return; }
    window.__renaInspector = true;
    var seq = 0;

    function bridge() { return window.RenaInspector; }

    function send(kind, payload) {
        try {
            var b = bridge();
            if (b) { b.push(kind, JSON.stringify(payload)); }
        } catch (e) { }
    }

    function format(value, depth) {
        depth = depth || 0;
        try {
            if (value === null) { return 'null'; }
            if (value === undefined) { return 'undefined'; }
            var type = typeof value;
            if (type === 'string') { return depth === 0 ? value : JSON.stringify(value); }
            if (type === 'number' || type === 'boolean' || type === 'bigint') { return String(value); }
            if (type === 'function') { return 'function ' + (value.name || '') + '()'; }
            if (type === 'symbol') { return value.toString(); }
            if (value instanceof Error) { return value.name + ': ' + value.message; }
            if (value instanceof Element) {
                return '<' + value.tagName.toLowerCase() + (value.id ? '#' + value.id : '') +
                    (value.className && value.className.toString ? '.' + value.className.toString().trim().split(/\s+/).join('.') : '') + '>';
            }
            if (value instanceof Node) { return '[Node ' + value.nodeName + ']'; }
            if (depth > 2) { return Array.isArray(value) ? '[...]' : '{...}'; }
            if (Array.isArray(value)) {
                var items = [];
                for (var i = 0; i < value.length && i < 20; i++) { items.push(format(value[i], depth + 1)); }
                return '[' + items.join(', ') + (value.length > 20 ? ', \u2026' : '') + ']';
            }
            var keys = Object.keys(value);
            var parts = [];
            for (var k = 0; k < keys.length && k < 20; k++) {
                parts.push(keys[k] + ': ' + format(value[keys[k]], depth + 1));
            }
            return '{' + parts.join(', ') + (keys.length > 20 ? ', \u2026' : '') + '}';
        } catch (e) {
            return '[unprintable]';
        }
    }
    window.__renaFmt = format;

    if (window.fetch) {
        var originalFetch = window.fetch;
        window.fetch = function (input, init) {
            var id = ++seq;
            var started = Date.now();
            var url = typeof input === 'string' ? input : (input && input.url) || '';
            var method = (init && init.method) || (input && input.method) || 'GET';
            return originalFetch.apply(this, arguments).then(function (response) {
                send('net', { id: id, via: 'fetch', method: method, url: url, status: response.status, ms: Date.now() - started, type: response.headers.get('content-type') || '', size: parseInt(response.headers.get('content-length') || '-1', 10) });
                return response;
            }, function (error) {
                send('net', { id: id, via: 'fetch', method: method, url: url, status: 0, ms: Date.now() - started, type: '', size: -1, error: String(error) });
                throw error;
            });
        };
    }

    var originalOpen = XMLHttpRequest.prototype.open;
    var originalSend = XMLHttpRequest.prototype.send;
    XMLHttpRequest.prototype.open = function (method, url) {
        this.__rena = { method: method, url: String(url) };
        return originalOpen.apply(this, arguments);
    };
    XMLHttpRequest.prototype.send = function () {
        var xhr = this;
        var info = xhr.__rena || { method: 'GET', url: '' };
        var id = ++seq;
        var started = Date.now();
        xhr.addEventListener('loadend', function () {
            var length = -1;
            try { length = parseInt(xhr.getResponseHeader('content-length') || '-1', 10); } catch (e) { }
            var type = '';
            try { type = xhr.getResponseHeader('content-type') || ''; } catch (e) { }
            send('net', { id: id, via: 'xhr', method: info.method, url: info.url, status: xhr.status, ms: Date.now() - started, type: type, size: length });
        });
        return originalSend.apply(this, arguments);
    };

    function describe(node, depth) {
        if (node.nodeType === 3) {
            var text = node.nodeValue.replace(/\s+/g, ' ').trim();
            return text ? { t: 'text', v: text.substring(0, 200) } : null;
        }
        if (node.nodeType === 8) { return { t: 'comment', v: (node.nodeValue || '').substring(0, 120) }; }
        if (node.nodeType !== 1) { return null; }
        var attrs = [];
        for (var i = 0; i < node.attributes.length && i < 12; i++) {
            attrs.push(node.attributes[i].name + '="' + String(node.attributes[i].value).substring(0, 80) + '"');
        }
        var out = { t: 'el', n: node.tagName.toLowerCase(), a: attrs.join(' '), c: [] };
        if (depth < 0) { out.more = node.childNodes.length; return out; }
        var skip = out.n === 'script' || out.n === 'style' || out.n === 'svg' || out.n === 'noscript';
        if (!skip) {
            var limit = Math.min(node.childNodes.length, 60);
            for (var k = 0; k < limit; k++) {
                var child = describe(node.childNodes[k], depth - 1);
                if (child) { out.c.push(child); }
            }
            if (node.childNodes.length > limit) { out.more = node.childNodes.length - limit; }
        } else if (node.childNodes.length) {
            out.more = node.childNodes.length;
        }
        return out;
    }

    window.__renaDom = function (depth) {
        try { return JSON.stringify(describe(document.documentElement, depth || 6)); } catch (e) { return 'null'; }
    };

    window.__renaStorage = function () {
        var result = { cookies: document.cookie || '', local: {}, session: {} };
        try {
            for (var i = 0; i < localStorage.length; i++) {
                var key = localStorage.key(i);
                result.local[key] = String(localStorage.getItem(key)).substring(0, 600);
            }
        } catch (e) { }
        try {
            for (var j = 0; j < sessionStorage.length; j++) {
                var skey = sessionStorage.key(j);
                result.session[skey] = String(sessionStorage.getItem(skey)).substring(0, 600);
            }
        } catch (e) { }
        return JSON.stringify(result);
    };

    window.__renaInfo = function () {
        var p = performance && performance.timing ? performance.timing : null;
        return JSON.stringify({
            url: location.href,
            title: document.title,
            ua: navigator.userAgent,
            platform: navigator.platform,
            lang: navigator.language,
            viewport: window.innerWidth + 'x' + window.innerHeight,
            screen: screen.width + 'x' + screen.height,
            dpr: window.devicePixelRatio,
            charset: document.characterSet,
            readyState: document.readyState,
            cookiesEnabled: navigator.cookieEnabled,
            load: p ? (p.loadEventEnd - p.navigationStart) : -1,
            dom: p ? (p.domContentLoadedEventEnd - p.navigationStart) : -1,
            nodes: document.getElementsByTagName('*').length
        });
    };

    window.__renaEval = function (code) {
        try {
            var result = (0, eval)(code);
            if (result && typeof result.then === 'function') {
                result.then(function (v) { send('console', { level: 'result', text: format(v, 0), t: Date.now() }); },
                    function (e) { send('console', { level: 'error', text: 'Uncaught (in promise) ' + format(e, 0), t: Date.now() }); });
                return 'Promise {<pending>}';
            }
            return format(result, 0);
        } catch (e) {
            return 'Uncaught ' + (e && e.name ? e.name + ': ' + e.message : String(e));
        }
    };
})();
