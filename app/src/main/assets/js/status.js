(function () {
    if (window !== window.top) { return; }
    if (location.hostname !== 'web.whatsapp.com') { return; }
    var cfg = window.__RENA_CFG || {};
    if (!cfg.statusSaver) { return; }
    if (window.__renaStatus) { return; }
    window.__renaStatus = true;
    var labels = cfg.statusLabels || { save: 'Save', saved: 'Saved' };
    var seen = {};
    var button = null;
    var current = null;

    function bridge() { return window.RenaStatus; }

    function overlayOf(el) {
        var node = el;
        for (var depth = 0; node && node !== document.body && depth < 14; depth++) {
            var style = getComputedStyle(node);
            if (style.position === 'fixed' || style.position === 'absolute') {
                var rect = node.getBoundingClientRect();
                if (rect.width >= window.innerWidth * 0.6 && rect.height >= window.innerHeight * 0.6) { return node; }
            }
            node = node.parentElement;
        }
        return null;
    }

    function candidate() {
        var media = document.querySelectorAll('video[src^="blob:"],img[src^="blob:"],video source[src^="blob:"]');
        var best = null;
        for (var i = 0; i < media.length; i++) {
            var el = media[i];
            var holder = el.tagName === 'SOURCE' ? el.parentElement : el;
            var rect = holder.getBoundingClientRect();
            if (rect.width < 220 || rect.height < 220 || rect.bottom < 0 || rect.top > window.innerHeight) { continue; }
            var overlay = overlayOf(holder);
            if (!overlay) { continue; }
            var url = el.getAttribute('src') || holder.currentSrc || holder.src;
            if (!url) { continue; }
            if (!best || rect.width * rect.height > best.area) {
                best = { url: url, overlay: overlay, area: rect.width * rect.height, video: holder.tagName === 'VIDEO', holder: holder };
            }
        }
        return best;
    }

    function ensureButton() {
        if (button) { return button; }
        button = document.createElement('button');
        button.setAttribute('data-rena-ui', '1');
        button.style.cssText = 'all:initial;position:fixed;right:14px;bottom:86px;z-index:2147483647;padding:10px 16px;border-radius:24px;background:#00a884;color:#fff;font:700 14px system-ui,sans-serif;box-shadow:0 4px 16px rgba(0,0,0,.45);cursor:pointer';
        button.addEventListener('click', function () {
            if (!current) { return; }
            try {
                bridge().save(current.url, current.video ? 'video/mp4' : 'image/jpeg');
                seen[current.url] = true;
                button.textContent = labels.saved;
                setTimeout(hideButton, 1200);
            } catch (e) { }
        });
        return button;
    }

    function hideButton() {
        if (button && button.parentNode) { button.parentNode.removeChild(button); }
        current = null;
    }

    function showButton(found) {
        var b = ensureButton();
        current = found;
        b.textContent = seen[found.url] ? labels.saved : labels.save;
        if (!b.parentNode) { document.documentElement.appendChild(b); }
    }

    function looksLikeStatus(overlay) {
        var text = (overlay.innerText || '').slice(0, 400);
        return /status/i.test(text);
    }

    function tick() {
        var found = candidate();
        if (!found) { hideButton(); return; }
        showButton(found);
        if (cfg.statusAuto && !seen[found.url] && looksLikeStatus(found.overlay)) {
            seen[found.url] = true;
            try { bridge().save(found.url, found.video ? 'video/mp4' : 'image/jpeg'); } catch (e) { }
            if (button) { button.textContent = labels.saved; }
        }
    }

    setInterval(tick, 1200);
})();
