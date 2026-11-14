(function () {
    if (window !== window.top) { return; }
    if (window.__renaWa) { return; }
    window.__renaWa = true;

    function press(target) {
        var options = { key: 'Escape', code: 'Escape', keyCode: 27, which: 27, bubbles: true, cancelable: true };
        try {
            target.dispatchEvent(new KeyboardEvent('keydown', options));
            target.dispatchEvent(new KeyboardEvent('keyup', options));
        } catch (e) { }
    }

    window.__renaBack = function () {
        try {
            var dialog = document.querySelector('[role="dialog"],[data-animate-modal-popup="true"],[data-testid="popup"],[data-animate-media-viewer]');
            if (dialog) {
                press(document.activeElement || document.body);
                press(document);
                return true;
            }
            var panel = document.querySelector('[data-testid="drawer-right"],[data-animate-drawer-right]');
            if (panel) {
                press(document.activeElement || document.body);
                press(document);
                return true;
            }
            var main = document.querySelector('#main');
            if (main && main.offsetParent !== null && main.getBoundingClientRect().width > 0) {
                press(document.activeElement || document.body);
                press(document);
                return true;
            }
        } catch (e) { }
        return false;
    };
})();
