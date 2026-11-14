(function () {
    if (window !== window.top) { return; }
    var STYLE = '__rena_theme_style';
    var META = '__rena_theme_meta';
    var cfg = window.__RENA_CFG || {};
    var state = { mode: cfg.theme || 'dark' };
    if (window.__renaThemeInit) {
        if (window.__renaSetTheme) { window.__renaSetTheme(state.mode); }
        return;
    }
    window.__renaThemeInit = true;

    function isWhatsApp() { return location.hostname === 'web.whatsapp.com'; }

    function parse(color) {
        var m = /rgba?\((\d+),\s*(\d+),\s*(\d+)(?:,\s*([\d.]+))?\)/.exec(color || '');
        if (!m) { return null; }
        return [parseInt(m[1], 10), parseInt(m[2], 10), parseInt(m[3], 10), m[4] === undefined ? 1 : parseFloat(m[4])];
    }

    function luminance(rgb) {
        return (0.2126 * rgb[0] + 0.7152 * rgb[1] + 0.0722 * rgb[2]) / 255;
    }

    function pageIsLight() {
        var nodes = [document.body, document.documentElement];
        for (var i = 0; i < nodes.length; i++) {
            if (!nodes[i]) { continue; }
            var rgb = parse(getComputedStyle(nodes[i]).backgroundColor);
            if (rgb && rgb[3] > 0.5) { return luminance(rgb) > 0.55; }
        }
        return true;
    }

    function remove() {
        var style = document.getElementById(STYLE);
        if (style && style.parentNode) { style.parentNode.removeChild(style); }
        var meta = document.getElementById(META);
        if (meta && meta.parentNode) { meta.parentNode.removeChild(meta); }
    }

    function whatsappTheme(value) {
        try { localStorage.setItem('theme', JSON.stringify(value)); } catch (e) { }
        var body = document.body;
        if (body) {
            if (value === 'dark') { body.classList.add('dark'); } else { body.classList.remove('dark'); }
        }
    }

    function extraFilter(mode) {
        if (mode === 'amoled') { return ' contrast(1.28)'; }
        if (mode === 'pure') { return ' contrast(1.6) brightness(0.96)'; }
        return '';
    }

    function compensation(mode) {
        if (mode === 'amoled') { return ' contrast(0.78)'; }
        if (mode === 'pure') { return ' contrast(0.625) brightness(1.04)'; }
        return '';
    }

    function apply(settled) {
        remove();
        var mode = state.mode;
        var root = document.documentElement;
        if (!root) { return; }
        var css = '';
        if (mode === 'light') {
            if (isWhatsApp()) { whatsappTheme('light'); }
            css = ':root{color-scheme:light}';
        } else {
            var extra = extraFilter(mode);
            var comp = compensation(mode);
            if (isWhatsApp()) {
                whatsappTheme('dark');
                css = ':root{color-scheme:dark}';
                if (extra) {
                    css += 'html{filter:' + extra.trim() + ' !important}img,video,canvas{filter:' + comp.trim() + '}';
                }
            } else if (!settled || pageIsLight()) {
                css = ':root{color-scheme:dark}html{background:#fff;filter:invert(.92) hue-rotate(180deg)' + extra + ' !important}' +
                    'img,video,picture,canvas,iframe,embed,object,svg image,[style*="background-image"]{filter:invert(1) hue-rotate(180deg)' + comp + ' !important}';
            } else {
                css = ':root{color-scheme:dark}';
                if (extra) { css += 'html{filter:' + extra.trim() + ' !important}img,video,canvas{filter:' + comp.trim() + '}'; }
            }
            if (mode === 'pure' && !isWhatsApp()) {
                css += 'body{background-color:#000 !important}';
            }
        }
        var style = document.createElement('style');
        style.id = STYLE;
        style.textContent = css;
        (document.head || root).appendChild(style);
        var meta = document.createElement('meta');
        meta.id = META;
        meta.name = 'color-scheme';
        meta.content = mode === 'light' ? 'light' : 'dark';
        (document.head || root).appendChild(meta);
    }

    window.__renaSetTheme = function (mode) {
        state.mode = mode;
        apply(document.readyState !== 'loading');
    };

    apply(false);
    function settle() {
        apply(true);
        setTimeout(function () { apply(true); }, 900);
    }
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', settle);
    } else {
        settle();
    }
})();
