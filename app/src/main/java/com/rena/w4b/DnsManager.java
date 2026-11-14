package com.rena.w4b;

import android.content.Context;
import android.text.TextUtils;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

public final class DnsManager {
    public static final String SYSTEM = "system";

    public static final class Provider {
        public final String id;
        public final String label;
        public final String note;
        public final String[] endpoints;

        Provider(String id, String label, String note, String... endpoints) {
            this.id = id;
            this.label = label;
            this.note = note;
            this.endpoints = endpoints;
        }
    }

    private static final Provider[] PROVIDERS = new Provider[]{
            new Provider(SYSTEM, "System default", "", new String[0]),
            new Provider("cloudflare", "Cloudflare", "1.1.1.1", "https://1.1.1.1/dns-query", "https://cloudflare-dns.com/dns-query"),
            new Provider("cloudflare_family", "Cloudflare Family", "1.1.1.3", "https://1.1.1.3/dns-query", "https://family.cloudflare-dns.com/dns-query"),
            new Provider("google", "Google Public DNS", "8.8.8.8", "https://8.8.8.8/dns-query", "https://dns.google/dns-query"),
            new Provider("quad9", "Quad9", "9.9.9.9", "https://9.9.9.9/dns-query", "https://dns.quad9.net/dns-query"),
            new Provider("adguard", "AdGuard DNS", "94.140.14.14", "https://94.140.14.14/dns-query", "https://dns.adguard-dns.com/dns-query"),
            new Provider("opendns", "OpenDNS", "208.67.222.222", "https://208.67.222.222/dns-query", "https://doh.opendns.com/dns-query"),
            new Provider("mullvad", "Mullvad DNS", "dns.mullvad.net", "https://dns.mullvad.net/dns-query"),
            new Provider("controld", "Control D", "freedns.controld.com", "https://freedns.controld.com/p0"),
            new Provider("dnssb", "DNS.SB", "doh.dns.sb", "https://doh.dns.sb/dns-query"),
            new Provider("cleanbrowsing", "CleanBrowsing", "doh.cleanbrowsing.org", "https://doh.cleanbrowsing.org/doh/security-filter/")
    };

    private static boolean overrideActive;

    private static final String[] BYPASS = new String[]{"*.whatsapp.com", "*.whatsapp.net", "whatsapp.com", "whatsapp.net",
            "*.wa.me", "localhost", "127.0.0.1", "<local>"};

    private DnsManager() {
    }

    public static Provider[] providers() {
        return PROVIDERS;
    }

    public static Provider byId(String id) {
        for (Provider provider : PROVIDERS) {
            if (provider.id.equals(id)) {
                return provider;
            }
        }
        return PROVIDERS[0];
    }

    public static Provider current(Context context) {
        return byId(RenaPrefs.str(context, RenaPrefs.BR_DNS, SYSTEM));
    }

    public static boolean supported() {
        try {
            Class<?> featureClass = Class.forName("androidx.webkit.WebViewFeature");
            Method method = featureClass.getMethod("isFeatureSupported", String.class);
            return Boolean.TRUE.equals(method.invoke(null, "PROXY_OVERRIDE"));
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean choose(Context context, String id) {
        Provider provider = byId(id);
        if (!SYSTEM.equals(provider.id) && !supported()) {
            return false;
        }
        RenaPrefs.setStr(context, RenaPrefs.BR_DNS, provider.id);
        apply(context);
        return true;
    }

    public static synchronized void apply(Context context) {
        Provider provider = current(context);
        if (SYSTEM.equals(provider.id) || !supported()) {
            LocalDnsProxy.stop();
            if (overrideActive) {
                clearOverride();
                overrideActive = false;
            }
            return;
        }
        int port = LocalDnsProxy.start(provider);
        if (port > 0) {
            setOverride(port);
            overrideActive = true;
        }
    }

    private static Executor directExecutor() {
        return new Executor() {
            @Override
            public void execute(Runnable command) {
                command.run();
            }
        };
    }

    private static void setOverride(int port) {
        try {
            Class<?> controllerClass = Class.forName("androidx.webkit.ProxyController");
            Class<?> configClass = Class.forName("androidx.webkit.ProxyConfig");
            Class<?> builderClass = Class.forName("androidx.webkit.ProxyConfig$Builder");
            Object builder = builderClass.getConstructor().newInstance();
            builderClass.getMethod("addProxyRule", String.class).invoke(builder, "127.0.0.1:" + port);
            Method bypass = builderClass.getMethod("addBypassRule", String.class);
            for (String rule : BYPASS) {
                bypass.invoke(builder, rule);
            }
            Object config = builderClass.getMethod("build").invoke(builder);
            Object controller = controllerClass.getMethod("getInstance").invoke(null);
            controllerClass.getMethod("setProxyOverride", configClass, Executor.class, Runnable.class)
                    .invoke(controller, config, directExecutor(), new Runnable() {
                        @Override
                        public void run() {
                        }
                    });
        } catch (Throwable ignored) {
        }
    }

    private static void clearOverride() {
        try {
            Class<?> controllerClass = Class.forName("androidx.webkit.ProxyController");
            Object controller = controllerClass.getMethod("getInstance").invoke(null);
            controllerClass.getMethod("clearProxyOverride", Executor.class, Runnable.class)
                    .invoke(controller, directExecutor(), new Runnable() {
                        @Override
                        public void run() {
                        }
                    });
        } catch (Throwable ignored) {
        }
    }

    public static String describe(Context context, Provider provider) {
        if (SYSTEM.equals(provider.id)) {
            return context.getString(R.string.dns_system);
        }
        return TextUtils.isEmpty(provider.note) ? provider.label : provider.label + " \u00B7 " + provider.note;
    }
}
