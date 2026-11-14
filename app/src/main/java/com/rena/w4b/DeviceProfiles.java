package com.rena.w4b;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.webkit.WebSettings;
import android.webkit.WebView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class DeviceProfiles {
    public static final String DEFAULT_DEVICE = "ubuntu2404";
    public static final String DEFAULT_RATIO = "auto";
    public static final String MOBILE = "android";
    private static int cachedMajor;
    private static String cachedFullVersion;

    private DeviceProfiles() {
    }

    public static final class Device {
        public final String id;
        public final String label;
        public final String group;
        public final String template;
        public final String platform;
        public final String uaPlatform;
        public final String platformVersion;
        public final boolean edge;

        Device(String id, String label, String group, String template, String platform, String uaPlatform,
               String platformVersion, boolean edge) {
            this.id = id;
            this.label = label;
            this.group = group;
            this.template = template;
            this.platform = platform;
            this.uaPlatform = uaPlatform;
            this.platformVersion = platformVersion;
            this.edge = edge;
        }
    }

    public static final class Ratio {
        public final String id;
        public final String label;
        public final int width;
        public final int height;
        public final int layoutWidth;

        Ratio(String id, String label, int width, int height, int layoutWidth) {
            this.id = id;
            this.label = label;
            this.width = width;
            this.height = height;
            this.layoutWidth = layoutWidth;
        }
    }

    private static final String WIN = "Mozilla/5.0 (Windows NT %NT%; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/%M.0.0.0 Safari/537.36";
    private static final String LINUX = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/%M.0.0.0 Safari/537.36";
    private static final String MAC = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/%M.0.0.0 Safari/537.36";
    private static final String CROS = "Mozilla/5.0 (X11; CrOS x86_64 14541.0.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/%M.0.0.0 Safari/537.36";

    private static final Device[] DEVICES = new Device[]{
            new Device("win11", "Windows 11 \u00B7 Chrome", "Windows", WIN.replace("%NT%", "10.0"), "Win32", "Windows", "15.0.0", false),
            new Device("win11edge", "Windows 11 \u00B7 Edge", "Windows", WIN.replace("%NT%", "10.0") + " Edg/%M.0.0.0", "Win32", "Windows", "15.0.0", true),
            new Device("win10", "Windows 10 \u00B7 Chrome", "Windows", WIN.replace("%NT%", "10.0"), "Win32", "Windows", "10.0.0", false),
            new Device("win81", "Windows 8.1 \u00B7 Chrome", "Windows", WIN.replace("%NT%", "6.3"), "Win32", "Windows", "0.3.0", false),
            new Device("ubuntu2404", "Ubuntu 24.04 \u00B7 Chrome", "Linux", LINUX, "Linux x86_64", "Linux", "6.8.0", false),
            new Device("ubuntu2204", "Ubuntu 22.04 \u00B7 Chrome", "Linux", LINUX, "Linux x86_64", "Linux", "6.5.0", false),
            new Device("debian12", "Debian 12 \u00B7 Chrome", "Linux", LINUX, "Linux x86_64", "Linux", "6.1.0", false),
            new Device("fedora40", "Fedora 40 \u00B7 Chrome", "Linux", LINUX, "Linux x86_64", "Linux", "6.9.0", false),
            new Device("arch", "Arch Linux \u00B7 Chrome", "Linux", LINUX, "Linux x86_64", "Linux", "6.10.0", false),
            new Device("macsonoma", "macOS Sonoma \u00B7 Chrome", "macOS", MAC, "MacIntel", "macOS", "14.5.0", false),
            new Device("macventura", "macOS Ventura \u00B7 Chrome", "macOS", MAC, "MacIntel", "macOS", "13.6.0", false),
            new Device("chromeos", "ChromeOS \u00B7 Chrome", "ChromeOS", CROS, "Linux x86_64", "Chrome OS", "14541.0.0", false)
    };

    private static final Ratio[] RATIOS = new Ratio[]{
            new Ratio("auto", "Auto", 0, 0, 0),
            new Ratio("16_9", "16:9 \u00B7 1920\u00D71080", 1920, 1080, 1280),
            new Ratio("16_10", "16:10 \u00B7 1440\u00D7900", 1440, 900, 1200),
            new Ratio("4_3", "4:3 \u00B7 1024\u00D7768", 1024, 768, 1024),
            new Ratio("3_2", "3:2 \u00B7 1500\u00D71000", 1500, 1000, 1100),
            new Ratio("21_9", "21:9 \u00B7 2560\u00D71080", 2560, 1080, 1366),
            new Ratio("1_1", "1:1 \u00B7 1000\u00D71000", 1000, 1000, 1000)
    };

    public static Device[] devices() {
        return DEVICES;
    }

    public static Ratio[] ratios() {
        return RATIOS;
    }

    public static Device byId(String id) {
        for (Device device : DEVICES) {
            if (device.id.equals(id)) {
                return device;
            }
        }
        return DEVICES[4];
    }

    public static Ratio ratioById(String id) {
        for (Ratio ratio : RATIOS) {
            if (ratio.id.equals(id)) {
                return ratio;
            }
        }
        return RATIOS[0];
    }

    public static Device current(Context context) {
        return byId(RenaPrefs.str(context, RenaPrefs.DEVICE_ID, DEFAULT_DEVICE));
    }

    public static Ratio currentRatio(Context context) {
        return ratioById(RenaPrefs.str(context, RenaPrefs.RATIO_ID, DEFAULT_RATIO));
    }

    public static synchronized int chromeMajor(Context context) {
        if (cachedMajor > 0) {
            return cachedMajor;
        }
        int major = 134;
        String full = "134.0.0.0";
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                android.content.pm.PackageInfo info = WebView.getCurrentWebViewPackage();
                if (info != null && !TextUtils.isEmpty(info.versionName)) {
                    String version = info.versionName;
                    int dot = version.indexOf('.');
                    int parsed = Integer.parseInt(dot > 0 ? version.substring(0, dot) : version);
                    if (parsed >= 80 && parsed < 400) {
                        major = parsed;
                        full = version;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        cachedMajor = major;
        cachedFullVersion = full;
        return major;
    }

    public static String fullVersion(Context context) {
        chromeMajor(context);
        return cachedFullVersion == null ? "134.0.0.0" : cachedFullVersion;
    }

    public static String userAgent(Context context, Device device) {
        return device.template.replace("%M", String.valueOf(chromeMajor(context)));
    }

    public static String mobileUserAgent(Context context, String defaultAgent) {
        String agent = defaultAgent == null ? "" : defaultAgent;
        agent = agent.replace("; wv)", ")").replace(" Version/4.0", "");
        if (TextUtils.isEmpty(agent)) {
            agent = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/" +
                    chromeMajor(context) + ".0.0.0 Mobile Safari/537.36";
        }
        return agent;
    }

    public static JSONObject configFor(Context context, Device device, Ratio ratio) {
        JSONObject object = new JSONObject();
        try {
            int major = chromeMajor(context);
            JSONArray brands = new JSONArray();
            brands.put(brand("Chromium", String.valueOf(major)));
            brands.put(brand(device.edge ? "Microsoft Edge" : "Google Chrome", String.valueOf(major)));
            brands.put(brand("Not.A/Brand", "24"));
            JSONArray full = new JSONArray();
            full.put(brand("Chromium", fullVersion(context)));
            full.put(brand(device.edge ? "Microsoft Edge" : "Google Chrome", fullVersion(context)));
            full.put(brand("Not.A/Brand", "24.0.0.0"));
            object.put("ua", userAgent(context, device));
            object.put("platform", device.platform);
            object.put("uaPlatform", device.uaPlatform);
            object.put("platformVersion", device.platformVersion);
            object.put("arch", "x86");
            object.put("bitness", "64");
            object.put("fullVersion", fullVersion(context));
            object.put("brands", brands);
            object.put("fullVersionList", full);
            if (ratio != null && ratio.width > 0) {
                JSONObject screen = new JSONObject();
                screen.put("w", ratio.width);
                screen.put("h", ratio.height);
                object.put("screen", screen);
            }
        } catch (Throwable ignored) {
        }
        return object;
    }

    private static JSONObject brand(String name, String version) throws Exception {
        JSONObject object = new JSONObject();
        object.put("brand", name);
        object.put("version", version);
        return object;
    }

    public static void applyUserAgent(Context context, WebSettings settings, Device device) {
        if (settings == null || device == null) {
            return;
        }
        settings.setUserAgentString(userAgent(context, device));
        applyMetadata(context, settings, device);
    }

    public static void applyMobile(Context context, WebSettings settings, String defaultAgent) {
        settings.setUserAgentString(mobileUserAgent(context, defaultAgent));
        try {
            Class<?> compat = Class.forName("androidx.webkit.WebSettingsCompat");
            Class<?> metaClass = Class.forName("androidx.webkit.UserAgentMetadata");
            Method set = compat.getMethod("setUserAgentMetadata", WebSettings.class, metaClass);
            if (supports("USER_AGENT_METADATA")) {
                Class<?> builderClass = Class.forName("androidx.webkit.UserAgentMetadata$Builder");
                Object builder = builderClass.getConstructor().newInstance();
                builderClass.getMethod("setMobile", boolean.class).invoke(builder, true);
                builderClass.getMethod("setPlatform", String.class).invoke(builder, "Android");
                Object meta = builderClass.getMethod("build").invoke(builder);
                set.invoke(null, settings, meta);
            }
        } catch (Throwable ignored) {
        }
    }

    private static boolean supports(String feature) {
        try {
            Class<?> featureClass = Class.forName("androidx.webkit.WebViewFeature");
            Method method = featureClass.getMethod("isFeatureSupported", String.class);
            return Boolean.TRUE.equals(method.invoke(null, feature));
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void applyMetadata(Context context, WebSettings settings, Device device) {
        try {
            if (!supports("USER_AGENT_METADATA")) {
                return;
            }
            Class<?> compat = Class.forName("androidx.webkit.WebSettingsCompat");
            Class<?> metaClass = Class.forName("androidx.webkit.UserAgentMetadata");
            Class<?> builderClass = Class.forName("androidx.webkit.UserAgentMetadata$Builder");
            Class<?> brandClass = Class.forName("androidx.webkit.UserAgentMetadata$BrandVersion");
            Class<?> brandBuilderClass = Class.forName("androidx.webkit.UserAgentMetadata$BrandVersion$Builder");
            String major = String.valueOf(chromeMajor(context));
            String full = fullVersion(context);
            List<Object> brands = new ArrayList<Object>();
            brands.add(brandVersion(brandBuilderClass, "Chromium", major, full));
            brands.add(brandVersion(brandBuilderClass, device.edge ? "Microsoft Edge" : "Google Chrome", major, full));
            brands.add(brandVersion(brandBuilderClass, "Not.A/Brand", "24", "24.0.0.0"));
            Constructor<?> ctor = builderClass.getConstructor();
            Object builder = ctor.newInstance();
            builderClass.getMethod("setBrandVersionList", List.class).invoke(builder, brands);
            builderClass.getMethod("setFullVersion", String.class).invoke(builder, full);
            builderClass.getMethod("setPlatform", String.class).invoke(builder, device.uaPlatform);
            builderClass.getMethod("setPlatformVersion", String.class).invoke(builder, device.platformVersion);
            builderClass.getMethod("setArchitecture", String.class).invoke(builder, "x86");
            builderClass.getMethod("setBitness", int.class).invoke(builder, 64);
            builderClass.getMethod("setModel", String.class).invoke(builder, "");
            builderClass.getMethod("setMobile", boolean.class).invoke(builder, false);
            builderClass.getMethod("setWow64", boolean.class).invoke(builder, false);
            Object meta = builderClass.getMethod("build").invoke(builder);
            compat.getMethod("setUserAgentMetadata", WebSettings.class, metaClass).invoke(null, settings, meta);
            if (brandClass == null) {
                return;
            }
        } catch (Throwable ignored) {
        }
    }

    private static Object brandVersion(Class<?> builderClass, String brand, String major, String full) throws Exception {
        Object builder = builderClass.getConstructor().newInstance();
        builderClass.getMethod("setBrand", String.class).invoke(builder, brand);
        builderClass.getMethod("setMajorVersion", String.class).invoke(builder, major);
        builderClass.getMethod("setFullVersion", String.class).invoke(builder, full);
        return builderClass.getMethod("build").invoke(builder);
    }
}
