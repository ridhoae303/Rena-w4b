package com.rena.w4b;

import android.net.Uri;
import android.text.TextUtils;

import java.util.HashSet;
import java.util.Locale;

public final class AdBlocker {
    private static final HashSet<String> AD_HOSTS = new HashSet<String>();
    private static final HashSet<String> TRACKER_HOSTS = new HashSet<String>();
    private static final HashSet<String> DETECTOR_HOSTS = new HashSet<String>();
    private static final String[] AD_PATHS = new String[]{
            "/pagead/", "/adview", "/ads/ga-audiences", "/gampad/", "/doubleclick/", "/adserver", "/adframe", "/advert.",
            "/ads.js", "/ads/show", "/prebid", "/adsbygoogle", "/show_ads", "/banner/ads", "/popunder", "/pop.js", "/adsense"
    };
    private static final String[] TRACKER_PATHS = new String[]{
            "/analytics.js", "/gtag/js", "/gtm.js", "/ga.js", "/fbevents.js", "/pixel.gif", "/collect?v=", "/__utm.gif",
            "/piwik.js", "/matomo.js", "/hotjar-", "/clarity.js", "/beacon.js", "/track/event", "/tr?id="
    };

    static {
        String ads = "doubleclick.net googlesyndication.com googleadservices.com adservice.google.com pagead2.googlesyndication.com "
                + "tpc.googlesyndication.com partner.googleadservices.com securepubads.g.doubleclick.net g.doubleclick.net "
                + "adnxs.com adsrvr.org adform.net ads.pubmatic.com pubmatic.com openx.net rubiconproject.com casalemedia.com "
                + "criteo.com criteo.net taboola.com outbrain.com revcontent.com mgid.com zemanta.com sharethrough.com "
                + "advertising.com ads.yahoo.com adtechus.com amazon-adsystem.com media.net contextweb.com lijit.com "
                + "indexww.com smartadserver.com serving-sys.com 33across.com sovrn.com yieldmo.com triplelift.com "
                + "bidswitch.net bidr.io adsymptotic.com moatads.com adsafeprotected.com doubleverify.com "
                + "popads.net popcash.net propellerads.com propellerclick.com exoclick.com exosrv.com juicyads.com "
                + "trafficjunky.net trafficfactory.biz adcash.com clickadu.com hilltopads.net adsterra.com "
                + "adskeeper.com admaven.com ad-maven.com richpush.co onclkds.com monetag.com onclickads.net "
                + "ero-advertising.com plugrush.com adxpansion.com tsyndicate.com a-ads.com coinzilla.com bitmedia.io "
                + "ads.twitter.com ads-api.twitter.com ads.linkedin.com ads.tiktok.com business-api.tiktok.com "
                + "an.facebook.com adservice.facebook.com ads.reddit.com adsmoloco.com inmobi.com applovin.com "
                + "unityads.unity3d.com ads.mopub.com mopub.com chartboost.com vungle.com ironsrc.com supersonicads.com "
                + "adcolony.com startappservice.com admob.com adtng.com adbrite.com adblade.com adroll.com "
                + "nativeroll.tv spotxchange.com spotx.tv teads.tv teads.com adition.com improvedigital.com "
                + "undertone.com conversantmedia.com gumgum.com rhythmone.com tremorhub.com freewheel.tv fwmrm.net "
                + "stickyadstv.com adsco.re admixer.net yandexadexchange.net an.yandex.ru awaps.yandex.net "
                + "ads.vk.com target.my.com mc.yandex.ru.ads ad.360yield.com yieldlab.net e-planning.net "
                + "adpushup.com ezoic.net ezoic.com ezodn.com mediavine.com adthrive.com raptive.com";
        String trackers = "google-analytics.com analytics.google.com googletagmanager.com googletagservices.com stats.g.doubleclick.net "
                + "connect.facebook.net graph.facebook.com pixel.facebook.com facebook.net analytics.twitter.com static.ads-twitter.com "
                + "analytics.tiktok.com bat.bing.com clarity.ms hotjar.com hotjar.io static.hotjar.com script.hotjar.com "
                + "mixpanel.com api.mixpanel.com cdn.mxpnl.com segment.com segment.io api.segment.io cdn.segment.com "
                + "amplitude.com api.amplitude.com cdn.amplitude.com fullstory.com rs.fullstory.com heap.io heapanalytics.com "
                + "mouseflow.com crazyegg.com luckyorange.com luckyorange.net inspectlet.com smartlook.com "
                + "quantserve.com scorecardresearch.com sb.scorecardresearch.com comscore.com chartbeat.com chartbeat.net "
                + "newrelic.com nr-data.net js-agent.newrelic.com bam.nr-data.net optimizely.com cdn.optimizely.com "
                + "branch.io app.link adjust.com adjust.io appsflyer.com onelink.me kochava.com singular.net "
                + "braze.com appboy.com onesignal.com pushwoosh.com bugsnag.com sentry.io browser.sentry-cdn.com "
                + "hs-analytics.net hsforms.com hubspot.com track.hubspot.com marketo.net munchkin.marketo.net pardot.com "
                + "demdex.net omtrdc.net everesttech.net 2o7.net adobedtm.com tiqcdn.com tealiumiq.com bluekai.com "
                + "krxd.net exelator.com agkn.com mathtag.com rlcdn.com liadm.com id5-sync.com pippio.com tapad.com "
                + "yandex.ru/metrika mc.yandex.ru metrika.yandex.ru top-fwz1.mail.ru counter.yadro.ru liveinternet.ru "
                + "statcounter.com c.statcounter.com histats.com whos.amung.us tracking.wpvip.com stats.wp.com pixel.wp.com "
                + "analytics.yahoo.com sp.analytics.yahoo.com ads.linkedin.com px.ads.linkedin.com snap.licdn.com "
                + "sc-static.net tr.snapchat.com pixel.quantserve.com ct.pinterest.com analytics.pinterest.com "
                + "log.pinterest.com t.co/i/adsct ads-twitter.com pagead2.googleadservices.com";
        String detectors = "blockadblock.com fuckadblock.com detectadblock.com adblockanalytics.com adblock-detector.com "
                + "getadmiral.com admiral.mgr.consensu.org eu-cmp.admiral.mgr cdn.admiral.mgr "
                + "adblockdetector.netlify.app anti-adblock-scripts.com pagefair.com pagefair.net "
                + "sourcepoint.mgr.consensu.org adrecover.com adrecover.net adblockrecovery.com "
                + "blockthrough.com btloader.com adthrive.recover";
        for (String host : ads.split(" ")) {
            if (host.length() > 0) {
                AD_HOSTS.add(host.toLowerCase(Locale.US));
            }
        }
        for (String host : trackers.split(" ")) {
            if (host.length() > 0) {
                TRACKER_HOSTS.add(host.toLowerCase(Locale.US));
            }
        }
        for (String host : detectors.split(" ")) {
            if (host.length() > 0) {
                DETECTOR_HOSTS.add(host.toLowerCase(Locale.US));
            }
        }
    }

    private AdBlocker() {
    }

    private static boolean matches(HashSet<String> set, String host) {
        if (TextUtils.isEmpty(host)) {
            return false;
        }
        String h = host.toLowerCase(Locale.US);
        while (true) {
            if (set.contains(h)) {
                return true;
            }
            int dot = h.indexOf('.');
            if (dot < 0 || h.indexOf('.', dot + 1) < 0) {
                return false;
            }
            h = h.substring(dot + 1);
        }
    }

    public static boolean isAd(Uri uri) {
        if (uri == null) {
            return false;
        }
        String host = uri.getHost();
        if (matches(AD_HOSTS, host)) {
            return true;
        }
        String path = uri.getEncodedPath();
        if (path != null) {
            String lower = path.toLowerCase(Locale.US);
            for (String fragment : AD_PATHS) {
                if (lower.contains(fragment)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isTracker(Uri uri) {
        if (uri == null) {
            return false;
        }
        String host = uri.getHost();
        if (matches(TRACKER_HOSTS, host)) {
            return true;
        }
        String full = uri.toString().toLowerCase(Locale.US);
        for (String fragment : TRACKER_PATHS) {
            if (full.contains(fragment)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isDetector(Uri uri) {
        if (uri == null) {
            return false;
        }
        if (matches(DETECTOR_HOSTS, uri.getHost())) {
            return true;
        }
        String lower = uri.toString().toLowerCase(Locale.US);
        return lower.contains("fuckadblock") || lower.contains("blockadblock") || lower.contains("adblock-detect") ||
                lower.contains("detect-adblock") || lower.contains("anti-adblock") || lower.contains("adblock.js") ||
                lower.contains("adblockdetect");
    }

    private static final String[] TRACKING_PARAMS = new String[]{"utm_source", "utm_medium", "utm_campaign", "utm_term",
            "utm_content", "utm_id", "fbclid", "gclid", "gclsrc", "dclid", "msclkid", "mc_eid", "mc_cid", "igshid",
            "yclid", "_hsenc", "_hsmi", "mkt_tok", "oly_enc_id", "oly_anon_id", "vero_id", "ref_src", "ref_url",
            "wickedid", "twclid", "ttclid", "li_fat_id", "s_cid", "spm"};

    public static String stripTracking(String url) {
        if (TextUtils.isEmpty(url)) {
            return url;
        }
        try {
            Uri uri = Uri.parse(url);
            if (uri.getQuery() == null || !uri.isHierarchical()) {
                return url;
            }
            boolean changed = false;
            Uri.Builder builder = uri.buildUpon().clearQuery();
            for (String name : uri.getQueryParameterNames()) {
                boolean drop = false;
                for (String tracking : TRACKING_PARAMS) {
                    if (tracking.equalsIgnoreCase(name)) {
                        drop = true;
                        break;
                    }
                }
                if (drop) {
                    changed = true;
                    continue;
                }
                for (String value : uri.getQueryParameters(name)) {
                    builder.appendQueryParameter(name, value);
                }
            }
            return changed ? builder.build().toString() : url;
        } catch (Throwable ignored) {
            return url;
        }
    }
}
