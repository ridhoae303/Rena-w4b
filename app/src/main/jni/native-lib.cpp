#include <jni.h>
#include <string>
#include <fstream>
#include <cstring>
#include <EasyObfuse.h>

static jstring js(JNIEnv* env, const char* value) {
    return env->NewStringUTF(value);
}

static std::string permissionName(JNIEnv* env, jstring permission) {
    const char* p = env->GetStringUTFChars(permission, 0);
    std::string out(p ? p : "");
    if (p) {
        env->ReleaseStringUTFChars(permission, p);
    }
    return out;
}

static bool is_traced() {
    std::ifstream f("/proc/self/status");
    if (!f.is_open()) {
        return false;
    }

    std::string line;
    while (std::getline(f, line)) {
        if (line.compare(0, 10, "TracerPid:") == 0) {
            std::string v = line.substr(10);
            while (!v.empty() && (v[0] == ' ' || v[0] == '\t')) {
                v.erase(0, 1);
            }
            return v != "0";
        }
    }

    return false;
}

#define JNI_METHOD(name) Java_com_rena_w4b_NativeConfig_##name

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(webUrl)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("https://web.whatsapp.com"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(desktopUserAgent)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE(
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 "
        "(KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36"
    ));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(developerName)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("ridhoae303"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(githubUrl)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("https://github.com/ridhoae303"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(telegramUrl)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("https://t.me/ridhoae303"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(communityUrl)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE(
        "https://chat.whatsapp.com/DcA3oplpxcbDr5vVqIfvE6"
    ));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(donateText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Donate us"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(donateUrl)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("https://sociabuzz.com/ridhoae303"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(expectedSignatureSha256)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE(
        "e4201e2e32724c1ba1ef1100d35ff9f75c5d3e888a58c68b7747808f4c87607b"
    ));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(menuDeveloperTitle)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Developer"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(menuGithub)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("GitHub"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(menuTelegram)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Telegram"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(menuCommunity)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Modding Community"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(menuContentDescription)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Open navigation"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(loadingText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Loading..."));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(zoomLabel)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Zoom/Out"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(refreshText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Refresh"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(refreshContentDescription)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Refresh WhatsApp Web"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(footerText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Made with \xE2\x9D\xA4\xEF\xB8\x8F by ridhoae303"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(appFallbackName)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Rena W4B"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(renaImageAssetName)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Rena.jpg"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(permissionTitle)(JNIEnv* env, jclass, jstring permission) {
    std::string p = permissionName(env, permission);

    if (p.find("CAMERA") != std::string::npos) {
        return js(env, OBFUSCATE("Camera access"));
    }
    if (p.find("RECORD_AUDIO") != std::string::npos) {
        return js(env, OBFUSCATE("Microphone access"));
    }
    if (p.find("POST_NOTIFICATIONS") != std::string::npos) {
        return js(env, OBFUSCATE("Notifications"));
    }
    if (p.find("READ_EXTERNAL_STORAGE") != std::string::npos) {
        return js(env, OBFUSCATE("File access"));
    }

    return js(env, OBFUSCATE("Permission needed"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(permissionMessage)(JNIEnv* env, jclass, jstring permission) {
    std::string p = permissionName(env, permission);

    if (p.find("CAMERA") != std::string::npos) {
        return js(env, OBFUSCATE(
            "Allow camera access so WhatsApp Web can use your camera for supported features."
        ));
    }

    if (p.find("RECORD_AUDIO") != std::string::npos) {
        return js(env, OBFUSCATE(
            "Allow microphone access so WhatsApp Web can use voice and video features."
        ));
    }

    if (p.find("POST_NOTIFICATIONS") != std::string::npos) {
        return js(env, OBFUSCATE(
            "Allow notifications so supported WhatsApp Web notifications can be shown."
        ));
    }

    if (p.find("READ_EXTERNAL_STORAGE") != std::string::npos) {
        return js(env, OBFUSCATE(
            "Allow file access on older Android versions when WhatsApp Web needs to choose files."
        ));
    }

    return js(env, OBFUSCATE(
        "Rena W4B needs this permission for a related WhatsApp Web feature."
    ));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(permissionAllowButton)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Continue"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(permissionDenyButton)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Not now"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(permissionDeniedTitle)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Access denied"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(permissionDeniedMessage)(JNIEnv* env, jclass, jstring permission) {
    std::string p = permissionName(env, permission);

    if (p.find("CAMERA") != std::string::npos) {
        return js(env, OBFUSCATE(
            "Camera access was denied. You can enable it later in Settings > Apps > Rena W4B > Permissions."
        ));
    }

    if (p.find("RECORD_AUDIO") != std::string::npos) {
        return js(env, OBFUSCATE(
            "Microphone access was denied. You can enable it later in Settings > Apps > Rena W4B > Permissions."
        ));
    }

    if (p.find("POST_NOTIFICATIONS") != std::string::npos) {
        return js(env, OBFUSCATE(
            "Notifications were denied. You can enable them later in Settings > Apps > Rena W4B > Notifications."
        ));
    }

    if (p.find("READ_EXTERNAL_STORAGE") != std::string::npos) {
        return js(env, OBFUSCATE(
            "File access was denied. You can enable it later in Settings > Apps > Rena W4B > Permissions."
        ));
    }

    return js(env, OBFUSCATE(
        "This permission was denied. You can enable it later in the app settings."
    ));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(permissionCloseButton)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Close"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(permissionSettingsButton)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Open Settings"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(integrityErrorMessage)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Application integrity check failed."));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(backPressToast)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Press back again to exit Rena W4B."));
}
