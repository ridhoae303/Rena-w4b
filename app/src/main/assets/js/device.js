(function () {
    var cfg = window.__RENA_CFG;
    if (!cfg || !cfg.device || window.__renaDeviceDone) { return; }
    window.__renaDeviceDone = true;
    var d = cfg.device;

    function define(target, name, getter) {
        try {
            Object.defineProperty(target, name, { get: getter, set: undefined, configurable: true, enumerable: true });
        } catch (e) { }
    }

    var nav = Navigator.prototype;
    define(nav, 'platform', function () { return d.platform; });
    define(nav, 'maxTouchPoints', function () { return 0; });
    define(nav, 'vendor', function () { return 'Google Inc.'; });
    define(nav, 'userAgent', function () { return d.ua; });
    define(nav, 'appVersion', function () { return d.ua.replace(/^Mozilla\//, ''); });

    var brands = d.brands || [];
    var high = {
        brands: brands,
        mobile: false,
        platform: d.uaPlatform,
        architecture: d.arch,
        bitness: d.bitness,
        model: '',
        platformVersion: d.platformVersion,
        uaFullVersion: d.fullVersion,
        fullVersionList: d.fullVersionList || brands,
        wow64: false,
        formFactors: ['Desktop']
    };
    var uaData = {
        brands: brands,
        mobile: false,
        platform: d.uaPlatform,
        getHighEntropyValues: function (hints) {
            var out = { brands: brands, mobile: false, platform: d.uaPlatform };
            (hints || []).forEach(function (hint) {
                if (hint in high) { out[hint] = high[hint]; }
            });
            return Promise.resolve(out);
        },
        toJSON: function () { return { brands: brands, mobile: false, platform: d.uaPlatform }; }
    };
    define(nav, 'userAgentData', function () { return uaData; });

    ['ontouchstart', 'ontouchend', 'ontouchmove', 'ontouchcancel'].forEach(function (name) {
        [Window.prototype, Document.prototype, HTMLElement.prototype, Element.prototype].forEach(function (proto) {
            try { delete proto[name]; } catch (e) { }
        });
        try { delete window[name]; } catch (e) { }
    });
    try { delete window.orientation; } catch (e) { }
    define(window, 'orientation', function () { return undefined; });

    if (d.screen) {
        var s = d.screen;
        define(Screen.prototype, 'width', function () { return s.w; });
        define(Screen.prototype, 'height', function () { return s.h; });
        define(Screen.prototype, 'availWidth', function () { return s.w; });
        define(Screen.prototype, 'availHeight', function () { return s.h - 40; });
        define(Screen.prototype, 'colorDepth', function () { return 24; });
        define(Screen.prototype, 'pixelDepth', function () { return 24; });
    }

    var originalMatch = window.matchMedia;
    if (originalMatch) {
        window.matchMedia = function (query) {
            var result = originalMatch.call(window, query);
            try {
                var q = String(query);
                var coarse = /(any-)?pointer\s*:\s*coarse/.test(q) || /(any-)?hover\s*:\s*none/.test(q);
                var fine = /(any-)?pointer\s*:\s*fine/.test(q) || /(any-)?hover\s*:\s*hover/.test(q);
                if (coarse || fine) {
                    var fake = originalMatch.call(window, 'all');
                    Object.defineProperty(fake, 'matches', { value: fine && !coarse });
                    Object.defineProperty(fake, 'media', { value: q });
                    return fake;
                }
            } catch (e) { }
            return result;
        };
    }
})();
