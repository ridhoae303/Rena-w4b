(function () {
    if (window !== window.top) { return; }
    if (location.hostname !== 'web.whatsapp.com') { return; }
    var cfg = window.__RENA_CFG || {};
    if (!cfg.antiDelete) { return; }
    if (window.__renaAD) { return; }
    window.__renaAD = true;

    var labels = cfg.adLabels || { deleted: 'Deleted message', media: 'Media' };
    var store = {};
    var previews = {};
    var outbox = [];
    var flushTimer = null;
    var DELETED = /(this message was deleted|you deleted this message|pesan ini telah dihapus|anda menghapus pesan ini|pesan ini dihapus|se elimin\u00f3 este mensaje|esta mensagem foi apagada|ce message a \u00e9t\u00e9 supprim\u00e9|diese nachricht wurde gel\u00f6scht|questo messaggio \u00e8 stato eliminato)/i;

    function bridge() { return window.RenaAntiDelete; }

    try {
        var raw = bridge() && bridge().load();
        if (raw) {
            JSON.parse(raw).forEach(function (record) { store[record.id] = record; });
        }
    } catch (e) { }

    function queue(record) {
        outbox.push(record);
        if (!flushTimer) {
            flushTimer = setTimeout(function () {
                flushTimer = null;
                var batch = outbox;
                outbox = [];
                try { bridge().putAll(JSON.stringify(batch)); } catch (e) { }
            }, 900);
        }
    }

    function chatTitle() {
        var header = document.querySelector('#main header span[title]') || document.querySelector('#main header [title]');
        return header ? (header.getAttribute('title') || '') : '';
    }

    function kindOf(row) {
        if (row.querySelector('[data-icon="audio-play"],[data-icon="ptt-status"],[data-icon="audio-download"]')) { return 'voice'; }
        if (row.querySelector('video,[data-icon="video-pip"],[data-icon="media-play"]')) { return 'video'; }
        if (row.querySelector('[data-icon="document"],[data-icon^="document-"]')) { return 'document'; }
        if (row.querySelector('img[src^="blob:"],img[src^="data:image"]')) { return 'image'; }
        if (row.querySelector('[data-icon="sticker"]')) { return 'sticker'; }
        return '';
    }

    function thumbOf(row) {
        var img = row.querySelector('img[src^="data:image"]');
        if (img && img.src.length < 90000) { return img.src; }
        return '';
    }

    function textOf(row) {
        var parts = [];
        var nodes = row.querySelectorAll('.selectable-text');
        for (var i = 0; i < nodes.length; i++) {
            var node = nodes[i];
            if (node.closest('[data-rena-restored]')) { continue; }
            var parent = node.parentElement && node.parentElement.closest('.selectable-text');
            if (parent && parent !== node) { continue; }
            var value = ((node.innerText !== undefined ? node.innerText : node.textContent) || '').trim();
            if (value) { parts.push(value); }
        }
        return parts.join('\n');
    }

    function isDeleted(row) {
        if (row.querySelector('[data-icon="recalled"],[data-icon="recalled-by-me"],[data-testid="recalled"]')) { return true; }
        if (row.querySelector('[data-pre-plain-text]')) { return false; }
        var text = ((row.innerText !== undefined ? row.innerText : row.textContent) || '').trim();
        return text.length < 90 && DELETED.test(text);
    }

    function restore(row, record) {
        if (row.querySelector('[data-rena-restored]')) { return; }
        var box = document.createElement('div');
        box.setAttribute('data-rena-restored', '1');
        box.setAttribute('data-rena-ui', '1');
        box.style.cssText = 'margin:6px 0 2px;padding:6px 10px;border-left:3px solid #ff5a5f;background:rgba(255,90,95,.14);border-radius:6px;font-size:13px;line-height:1.35;white-space:pre-wrap;word-break:break-word;color:inherit;user-select:text;-webkit-user-select:text';
        var title = document.createElement('div');
        title.textContent = '\uD83D\uDDD1 ' + labels.deleted;
        title.style.cssText = 'font-weight:700;font-size:11px;opacity:.85;margin-bottom:3px';
        box.appendChild(title);
        if (record.thumb) {
            var image = document.createElement('img');
            image.src = record.thumb;
            image.style.cssText = 'max-width:220px;max-height:220px;border-radius:6px;display:block;margin-bottom:4px';
            box.appendChild(image);
        }
        if (record.text) {
            var body = document.createElement('div');
            body.textContent = record.text;
            box.appendChild(body);
        } else if (record.kind) {
            var kind = document.createElement('div');
            kind.textContent = labels.media + ': ' + record.kind;
            box.appendChild(kind);
        }
        var icon = row.querySelector('[data-icon="recalled"],[data-icon="recalled-by-me"]');
        var anchor = icon ? (icon.closest('span') || icon.parentElement) : null;
        if (anchor && anchor.parentElement) {
            anchor.parentElement.insertBefore(box, anchor.nextSibling);
        } else {
            row.appendChild(box);
        }
    }

    function scanRow(row) {
        var id = row.getAttribute('data-id');
        if (!id || id.indexOf('_') < 0 || id.indexOf('false_') !== 0) { return; }
        var record = store[id];
        if (isDeleted(row)) {
            if (!record) { return; }
            if (!record.deleted) {
                record.deleted = true;
                record.deletedAt = Date.now();
                queue(record);
            }
            restore(row, record);
            return;
        }
        var text = textOf(row);
        var kind = kindOf(row);
        var thumb = thumbOf(row);
        if (!text && !kind) { return; }
        var pre = row.querySelector('[data-pre-plain-text]');
        if (!record) {
            record = { id: id, chat: chatTitle(), t: Date.now(), deleted: false };
        }
        if (record.text !== text || (thumb && !record.thumb) || record.kind !== kind) {
            record.text = text;
            record.kind = kind;
            if (thumb) { record.thumb = thumb; }
            record.pre = pre ? pre.getAttribute('data-pre-plain-text') : (record.pre || '');
            if (!record.chat) { record.chat = chatTitle(); }
            store[id] = record;
            queue(record);
        }
    }

    function scanPreviews() {
        var rows = document.querySelectorAll('#pane-side [role="listitem"],#pane-side [role="row"]');
        for (var i = 0; i < rows.length; i++) {
            var row = rows[i];
            var titleEl = row.querySelector('span[title]');
            if (!titleEl) { continue; }
            var title = titleEl.getAttribute('title') || '';
            var spans = row.querySelectorAll('span[dir="ltr"],span[dir="auto"]');
            var target = null;
            for (var s = spans.length - 1; s >= 0; s--) {
                var value = (spans[s].textContent || '').trim();
                if (!value || value === title) { continue; }
                if (/^\d{1,2}[:.]\d{2}/.test(value) || /^(yesterday|kemarin|today|hari ini)$/i.test(value)) { continue; }
                target = spans[s];
                break;
            }
            if (!target) { continue; }
            var preview = (target.textContent || '').trim();
            if (target.getAttribute('data-rena-restored-preview')) { continue; }
            if (DELETED.test(preview) && preview.length < 90) {
                var previous = previews[title];
                if (previous && !previous.reported) {
                    previous.reported = true;
                    var record = { id: 'preview:' + title + ':' + previous.t, chat: title, text: previous.text, kind: '', t: previous.t, deleted: true, deletedAt: Date.now(), source: 'preview' };
                    store[record.id] = record;
                    queue(record);
                }
                if (previous) {
                    target.textContent = '\uD83D\uDDD1 ' + previous.text;
                    target.setAttribute('data-rena-restored-preview', '1');
                }
            } else if (preview && !DELETED.test(preview)) {
                var known = previews[title];
                if (!known || known.text !== preview) {
                    previews[title] = { text: preview, t: Date.now(), reported: false };
                }
            }
        }
    }

    var dirty = new Set();
    var timer = null;
    function schedule(node) {
        var el = node && node.nodeType === 1 ? node : (node && node.parentElement);
        if (!el) { return; }
        var holder = el.closest ? el.closest('[data-id]') : null;
        if (holder) { dirty.add(holder); }
        if (el.querySelectorAll) {
            var inner = el.querySelectorAll('[data-id]');
            for (var i = 0; i < inner.length && i < 200; i++) { dirty.add(inner[i]); }
        }
        if (!timer) {
            timer = setTimeout(function () {
                timer = null;
                var batch = Array.from(dirty);
                dirty.clear();
                batch.forEach(function (row) { try { scanRow(row); } catch (e) { } });
                try { scanPreviews(); } catch (e) { }
            }, 250);
        }
    }

    function start() {
        if (!document.body) { return; }
        new MutationObserver(function (mutations) {
            for (var i = 0; i < mutations.length; i++) {
                var m = mutations[i];
                if (m.target && m.target.closest && m.target.closest('[data-rena-restored]')) { continue; }
                if (m.type === 'childList') {
                    schedule(m.target);
                    for (var a = 0; a < m.addedNodes.length; a++) { schedule(m.addedNodes[a]); }
                } else {
                    schedule(m.target);
                }
            }
        }).observe(document.body, { childList: true, subtree: true, characterData: true });
        schedule(document.body);
        setInterval(function () { try { scanPreviews(); } catch (e) { } }, 2500);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', start);
    } else {
        start();
    }
})();
