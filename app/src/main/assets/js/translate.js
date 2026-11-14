(function () {
    if (window !== window.top) { return; }
    if (window.__renaTr) {
        if (window.__renaTr.refresh) { window.__renaTr.refresh(); }
        return;
    }
    var cfg = window.__RENA_CFG || {};
    var labels = (cfg.translate && cfg.translate.labels) || { title: 'Translate', to: 'Translate to', close: 'Close' };
    var S = window.__renaTr = {
        on: false,
        lang: (cfg.translate && cfg.translate.lang) || 'en',
        originals: new WeakMap(),
        translated: new WeakMap(),
        callbacks: {},
        seq: 0,
        observer: null,
        timer: null,
        queue: [],
        host: null
    };

    var LANGS = [
        ['en', 'English'], ['id', 'Indonesia'], ['ms', 'Melayu'], ['ar', '\u0627\u0644\u0639\u0631\u0628\u064a\u0629'], ['zh-CN', '\u4e2d\u6587 (\u7b80\u4f53)'],
        ['zh-TW', '\u4e2d\u6587 (\u7e41\u9ad4)'], ['ja', '\u65e5\u672c\u8a9e'], ['ko', '\ud55c\uad6d\uc5b4'], ['hi', '\u0939\u093f\u0928\u094d\u0926\u0940'],
        ['bn', '\u09ac\u09be\u0982\u09b2\u09be'], ['ur', '\u0627\u0631\u062f\u0648'], ['fa', '\u0641\u0627\u0631\u0633\u06cc'], ['tr', 'T\u00fcrk\u00e7e'],
        ['ru', '\u0420\u0443\u0441\u0441\u043a\u0438\u0439'], ['uk', '\u0423\u043a\u0440\u0430\u0457\u043d\u0441\u044c\u043a\u0430'], ['fr', 'Fran\u00e7ais'],
        ['de', 'Deutsch'], ['es', 'Espa\u00f1ol'], ['pt', 'Portugu\u00eas'], ['it', 'Italiano'], ['nl', 'Nederlands'], ['pl', 'Polski'],
        ['sv', 'Svenska'], ['ro', 'Rom\u00e2n\u0103'], ['el', '\u0395\u03bb\u03bb\u03b7\u03bd\u03b9\u03ba\u03ac'], ['he', '\u05e2\u05d1\u05e8\u05d9\u05ea'],
        ['cs', '\u010ce\u0161tina'], ['hu', 'Magyar'], ['th', '\u0e44\u0e17\u0e22'], ['vi', 'Ti\u1ebfng Vi\u1ec7t'], ['tl', 'Filipino'],
        ['jw', 'Basa Jawa'], ['su', 'Basa Sunda']
    ];

    function bridge() { return window.RenaTranslate; }

    function blocked(node) {
        var p = node.parentNode;
        while (p && p !== document) {
            var name = p.nodeName;
            if (name === 'SCRIPT' || name === 'STYLE' || name === 'NOSCRIPT' || name === 'TEXTAREA' || name === 'INPUT' ||
                name === 'CODE' || name === 'PRE' || name === 'SELECT' || name === 'OPTION') { return true; }
            if (p.isContentEditable) { return true; }
            if (p.getAttribute && (p.getAttribute('translate') === 'no' || p.getAttribute('contenteditable') === 'true' ||
                p.hasAttribute('data-rena-ui'))) { return true; }
            p = p.parentNode;
        }
        return false;
    }

    function collect(root) {
        var found = [];
        if (!root) { return found; }
        if (root.nodeType === 3) {
            if (needs(root)) { found.push(root); }
            return found;
        }
        var walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT, null);
        var node;
        while ((node = walker.nextNode())) {
            if (needs(node)) { found.push(node); }
        }
        return found;
    }

    function needs(node) {
        var value = node.nodeValue;
        if (!value) { return false; }
        var trimmed = value.trim();
        if (trimmed.length < 2) { return false; }
        if (!/[A-Za-z\u00C0-\uFFFF]/.test(trimmed)) { return false; }
        if (S.translated.has(node) && S.translated.get(node) === value) { return false; }
        return !blocked(node);
    }

    function flush() {
        S.timer = null;
        if (!S.on) { S.queue = []; return; }
        var nodes = S.queue;
        S.queue = [];
        var batches = [];
        var current = [];
        var size = 0;
        nodes.forEach(function (node) {
            if (!node.parentNode || !needs(node)) { return; }
            var text = node.nodeValue;
            if (size + text.length > 3000 || current.length >= 25) {
                batches.push(current);
                current = [];
                size = 0;
            }
            current.push(node);
            size += text.length;
        });
        if (current.length) { batches.push(current); }
        batches.forEach(send);
    }

    function send(nodes) {
        var b = bridge();
        if (!b) { return; }
        var id = String(++S.seq);
        var texts = nodes.map(function (node) {
            var original = S.originals.has(node) && S.translated.has(node) && S.translated.get(node) === node.nodeValue ? S.originals.get(node) : node.nodeValue;
            return original;
        });
        S.callbacks[id] = { nodes: nodes, texts: texts, lang: S.lang };
        try { b.translate(id, JSON.stringify(texts), S.lang); } catch (e) { delete S.callbacks[id]; }
    }

    S.cb = function (id, json) {
        var pending = S.callbacks[id];
        delete S.callbacks[id];
        if (!pending || !S.on || pending.lang !== S.lang) { return; }
        var results;
        try { results = JSON.parse(json); } catch (e) { return; }
        pending.nodes.forEach(function (node, index) {
            var result = results[index];
            if (typeof result !== 'string' || !result || !node.parentNode) { return; }
            var source = pending.texts[index];
            if (node.nodeValue !== source && !(S.translated.has(node) && S.translated.get(node) === node.nodeValue)) { return; }
            var lead = (source.match(/^\s*/) || [''])[0];
            var tail = (source.match(/\s*$/) || [''])[0];
            var next = lead + result.trim() + tail;
            S.originals.set(node, source);
            S.translated.set(node, next);
            node.nodeValue = next;
        });
    };

    function enqueue(nodes) {
        if (!nodes.length) { return; }
        S.queue = S.queue.concat(nodes);
        if (!S.timer) { S.timer = setTimeout(flush, 350); }
    }

    function restore() {
        var walker = document.createTreeWalker(document.body || document.documentElement, NodeFilter.SHOW_TEXT, null);
        var node;
        while ((node = walker.nextNode())) {
            if (S.originals.has(node) && S.translated.has(node) && S.translated.get(node) === node.nodeValue) {
                node.nodeValue = S.originals.get(node);
                S.translated.delete(node);
            }
        }
    }

    function observe() {
        if (S.observer || !document.body) { return; }
        S.observer = new MutationObserver(function (mutations) {
            if (!S.on) { return; }
            var nodes = [];
            mutations.forEach(function (m) {
                if (m.type === 'characterData') {
                    if (needs(m.target)) { nodes.push(m.target); }
                } else {
                    for (var i = 0; i < m.addedNodes.length; i++) {
                        var added = m.addedNodes[i];
                        if (added.nodeType === 1 && added === S.host) { continue; }
                        nodes = nodes.concat(collect(added));
                    }
                }
            });
            enqueue(nodes);
        });
        S.observer.observe(document.body, { childList: true, subtree: true, characterData: true });
    }

    function widget() {
        if (S.host || !document.documentElement) { return; }
        var host = document.createElement('div');
        host.setAttribute('data-rena-ui', '1');
        host.style.cssText = 'all:initial;position:fixed;top:10px;left:50%;transform:translateX(-50%);z-index:2147483647;';
        var root = host.attachShadow ? host.attachShadow({ mode: 'open' }) : host;
        var options = LANGS.map(function (l) {
            return '<option value="' + l[0] + '"' + (l[0] === S.lang ? ' selected' : '') + '>' + l[1] + '</option>';
        }).join('');
        root.innerHTML = '<style>.p{display:flex;align-items:center;gap:8px;padding:6px 8px 6px 12px;border-radius:22px;background:rgba(32,44,51,.96);color:#e9edef;font:600 13px system-ui,sans-serif;box-shadow:0 4px 18px rgba(0,0,0,.4);border:1px solid rgba(255,255,255,.14)}' +
            '.g{width:18px;height:18px;border-radius:50%;background:#00a884;display:inline-flex;align-items:center;justify-content:center;font-size:11px;color:#fff}' +
            'select{background:#111b21;color:#e9edef;border:1px solid rgba(255,255,255,.2);border-radius:14px;padding:5px 8px;font:600 13px system-ui,sans-serif;max-width:150px}' +
            'button{all:unset;cursor:pointer;width:24px;height:24px;border-radius:50%;text-align:center;line-height:24px;color:#aebac1;font-size:15px}</style>' +
            '<div class="p"><span class="g">G</span><span>' + labels.to + '</span><select>' + options + '</select><button title="' + labels.close + '">\u00d7</button></div>';
        var select = root.querySelector('select');
        select.addEventListener('change', function () {
            S.setLang(select.value);
            try { bridge().setLang(select.value); } catch (e) { }
        });
        root.querySelector('button').addEventListener('click', function () {
            try { bridge().close(); } catch (e) { S.disable(); }
        });
        document.documentElement.appendChild(host);
        S.host = host;
    }

    S.enable = function (lang) {
        if (lang) { S.lang = lang; }
        S.on = true;
        var start = function () {
            widget();
            observe();
            enqueue(collect(document.body));
        };
        if (document.body) { start(); } else { document.addEventListener('DOMContentLoaded', start); }
    };

    S.setLang = function (lang) {
        S.lang = lang;
        if (!S.on) { return; }
        S.callbacks = {};
        restore();
        enqueue(collect(document.body));
    };

    S.disable = function () {
        S.on = false;
        S.callbacks = {};
        S.queue = [];
        if (S.timer) { clearTimeout(S.timer); S.timer = null; }
        if (S.observer) { S.observer.disconnect(); S.observer = null; }
        try { restore(); } catch (e) { }
        if (S.host && S.host.parentNode) { S.host.parentNode.removeChild(S.host); }
        S.host = null;
    };

    S.refresh = function () {
        if (S.on) { observe(); enqueue(collect(document.body)); }
    };

    if (cfg.translate && cfg.translate.enabled) { S.enable(S.lang); }
})();
