(function () {
    var cfg = window.__RENA_CFG || {};
    if (window.__renaAdblockInit) { return; }
    window.__renaAdblockInit = true;

    var HIDE = [
        '.adsbygoogle', 'ins.adsbygoogle', '[id^="google_ads_"]', '[id^="div-gpt-ad"]', '[id^="ad-slot"]',
        'iframe[src*="doubleclick.net"]', 'iframe[src*="googlesyndication"]', 'iframe[id^="google_ads"]',
        'iframe[src*="adservice"]', 'iframe[src*="ads."]', '.ad-slot', '.ad-container', '.ad-banner', '.ad-wrapper',
        '.advert', '.advertisement', '.advertisements', '[aria-label="Advertisement"]', '[aria-label="advertisement"]',
        '[data-ad-slot]', '[data-ad-client]', '[data-ad-unit]', '[data-google-query-id]', '.sponsored-content',
        '.taboola', '#taboola-below-article-thumbnails', '[id^="taboola-"]', '[class*="outbrain"]', '.OUTBRAIN',
        '[id^="mgid_"]', '.mgid', '[class*="adsbox"]', '[class*="ad_unit"]', '[class*="adunit"]',
        '.pub_300x250', '.pub_728x90', '#ad_banner', '#adbanner', '#ad-banner', '#ads', '#adContainer', '.google-ad',
        '.dfp-ad', '.gpt-ad', '.promoted-link', '[class*="sponsor-ad"]', '.ad-leaderboard', '.top-ad', '.bottom-ad',
        '.sticky-ad', '.interstitial-ad', '[data-testid="ad"]'
    ];

    function injectStyle(id, css) {
        var existing = document.getElementById(id);
        if (existing) { existing.textContent = css; return; }
        var style = document.createElement('style');
        style.id = id;
        style.textContent = css;
        (document.head || document.documentElement).appendChild(style);
    }

    function cosmetic() {
        if (!cfg.adblock) { return; }
        injectStyle('__rena_adblock_css', HIDE.join(',') + '{display:none !important;visibility:hidden !important;height:0 !important;min-height:0 !important;margin:0 !important;padding:0 !important}');
    }

    function stubDetectors() {
        if (!cfg.antiDetector) { return; }
        var noop = function () { return this; };
        function Stub() { }
        Stub.prototype = {
            onDetected: noop, onNotDetected: function (fn) { try { fn(); } catch (e) { } return this; },
            on: function (detected, fn) { if (!detected) { try { fn(); } catch (e) { } } return this; },
            check: function () { return false; }, emitEvent: noop, clearEvent: noop, setOption: noop, _creatBait: noop,
            _destroyBait: noop, _checkBait: noop, _timeout: noop, _options: {}
        };
        var stub = new Stub();
        ['fuckAdBlock', 'blockAdBlock', 'FuckAdBlock', 'BlockAdBlock', 'sniffAdBlock', 'adBlockDetector'].forEach(function (name) {
            try {
                Object.defineProperty(window, name, { configurable: true, get: function () { return name.charAt(0) === name.charAt(0).toUpperCase() ? Stub : stub; }, set: function () { } });
            } catch (e) { }
        });
        window.canRunAds = true;
        window.adblock = false;
        window.adBlock = false;
        window.adblockDetected = false;
        window.isAdBlockActive = false;
        window.google_ad_status = 1;
        if (!window.adsbygoogle) { window.adsbygoogle = { loaded: true, push: function () { } }; }
    }

    var WALL = /(ad[\s\-_]?block|ad[\s\-_]?blocker|adblocker|disable[^.]{0,40}(blocker|extension)|turn off[^.]{0,30}(blocker|ads)|whitelist[^.]{0,40}(site|us)|pemblokir iklan|nonaktifkan[^.]{0,40}(pemblokir|ad)|matikan[^.]{0,30}(pemblokir|adblock))/i;

    function overlayish(el) {
        var style = getComputedStyle(el);
        if (style.position !== 'fixed' && style.position !== 'absolute' && style.position !== 'sticky') { return false; }
        var z = parseInt(style.zIndex, 10);
        if (!(z >= 100)) { return false; }
        var rect = el.getBoundingClientRect();
        return rect.width > window.innerWidth * 0.4 && rect.height > 60;
    }

    function scan() {
        if (!cfg.antiDetector || !document.body) { return; }
        var removed = false;
        var nodes = document.querySelectorAll('div,section,aside,dialog,[role="dialog"],[role="alertdialog"],[class*="modal"],[class*="overlay"],[class*="adblock"],[id*="adblock"],[class*="popup"]');
        for (var i = 0; i < nodes.length && i < 800; i++) {
            var el = nodes[i];
            if (el === document.body || el === document.documentElement || el.id === '__rena_adblock_css') { continue; }
            var label = (el.className && el.className.toString ? el.className.toString() : '') + ' ' + (el.id || '');
            var text = el.textContent || '';
            var byName = /ad[\s\-_]?block/i.test(label);
            if (text.length > 1800 && !byName) { continue; }
            if ((byName || WALL.test(text)) && (byName || overlayish(el))) {
                el.parentNode && el.parentNode.removeChild(el);
                removed = true;
            }
        }
        if (removed) {
            [document.body, document.documentElement].forEach(function (node) {
                node.style.setProperty('overflow', 'auto', 'important');
                node.style.setProperty('position', 'static', 'important');
                node.classList.remove('modal-open', 'no-scroll', 'noscroll', 'overflow-hidden', 'adblock', 'has-adblock');
            });
        }
    }

    cosmetic();
    stubDetectors();
    window.__renaAdblockRefresh = function (next) {
        cfg = next || cfg;
        window.__RENA_CFG = cfg;
        var existing = document.getElementById('__rena_adblock_css');
        if (!cfg.adblock && existing && existing.parentNode) { existing.parentNode.removeChild(existing); }
        cosmetic();
        stubDetectors();
        scan();
    };

    function start() {
        cosmetic();
        scan();
        var ticks = 0;
        var timer = setInterval(function () {
            ticks++;
            scan();
            if (ticks > 20) { clearInterval(timer); }
        }, 1000);
        try {
            var pending = false;
            new MutationObserver(function () {
                if (pending) { return; }
                pending = true;
                setTimeout(function () { pending = false; scan(); }, 700);
            }).observe(document.documentElement, { childList: true, subtree: true });
        } catch (e) { }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', start);
    } else {
        start();
    }
})();
