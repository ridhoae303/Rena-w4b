(function () {
    if (window !== window.top) { return; }
    if (window.__renaZoom) { return; }
    window.__renaZoom = true;
    var cfg = window.__RENA_CFG || {};
    var width = cfg.layoutWidth || 0;

    function rewrite(content) {
        var parts = {};
        String(content || '').split(',').forEach(function (item) {
            var pair = item.split('=');
            if (pair.length === 2) { parts[pair[0].trim().toLowerCase()] = pair[1].trim(); }
        });
        delete parts['maximum-scale'];
        delete parts['minimum-scale'];
        delete parts['user-scalable'];
        if (width) {
            parts.width = String(width);
            delete parts['initial-scale'];
        }
        var out = [];
        for (var key in parts) { out.push(key + '=' + parts[key]); }
        out.push('minimum-scale=0.2');
        out.push('maximum-scale=10');
        out.push('user-scalable=yes');
        return out.join(', ');
    }

    var applying = false;
    function fix() {
        if (applying) { return; }
        applying = true;
        try {
            var head = document.head || document.documentElement;
            var meta = document.querySelector('meta[name="viewport"]');
            if (!meta) {
                if (!width && !cfg.forceZoom) { applying = false; return; }
                meta = document.createElement('meta');
                meta.setAttribute('name', 'viewport');
                head.appendChild(meta);
                meta.setAttribute('content', rewrite(''));
            } else {
                var next = rewrite(meta.getAttribute('content'));
                if (meta.getAttribute('content') !== next) { meta.setAttribute('content', next); }
            }
        } catch (e) { }
        applying = false;
    }

    function start() {
        fix();
        try {
            new MutationObserver(function () { fix(); }).observe(document.head || document.documentElement, {
                childList: true, subtree: true, attributes: true, attributeFilter: ['content']
            });
        } catch (e) { }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', start);
    } else {
        start();
    }
})();
