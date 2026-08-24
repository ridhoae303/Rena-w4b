#include <jni.h>
#include <string>
#include <fstream>
#include <cstring>
#include <EasyObfuse.h>

static bool clearJniException(JNIEnv* env) {
    if (env && env->ExceptionCheck()) {
        env->ExceptionClear();
        return true;
    }
    return false;
}

static jstring js(JNIEnv* env, const char* value) {
    if (!env || !value) return nullptr;
    jstring out = env->NewStringUTF(value);
    if (env->ExceptionCheck()) {
        env->ExceptionClear();
        return nullptr;
    }
    return out;
}

static std::string permissionName(JNIEnv* env, jstring permission) {
    if (!env || !permission) return std::string();
    const char* p = env->GetStringUTFChars(permission, nullptr);
    if (!p || clearJniException(env)) return std::string();
    std::string out(p);
    env->ReleaseStringUTFChars(permission, p);
    if (clearJniException(env)) return std::string();
    return out;
}

static bool is_traced() {
    const char* procPath = OBFUSCATE("/proc/self/status");
    std::ifstream f(procPath);
    if (!f.is_open()) {
        return false;
    }

    std::string line;
    while (std::getline(f, line)) {
        const char* tracerPrefix = OBFUSCATE("TracerPid:");
        if (line.compare(0, 10, tracerPrefix) == 0) {
            std::string v = line.substr(10);
            while (!v.empty() && (v[0] == ' ' || v[0] == '\t')) {
                v.erase(0, 1);
            }
            const char* zero = OBFUSCATE("0");
        return v != zero;
        }
    }

    return false;
}


static bool nativeCheckString(JNIEnv* env, jobject value, const char* expected) {
    if (!env || !value || !expected) return false;
    jclass stringCls = env->FindClass("java/lang/String");
    if (!stringCls || clearJniException(env)) return false;
    jmethodID toString = env->GetMethodID(
        stringCls, "toString", "()Ljava/lang/String;");
    if (!toString || clearJniException(env)) return false;
    jstring text = static_cast<jstring>(env->CallObjectMethod(value, toString));
    if (!text || clearJniException(env)) return false;
    const char* actual = env->GetStringUTFChars(text, nullptr);
    if (!actual || clearJniException(env)) return false;
    bool ok = std::strcmp(actual, expected) == 0;
    env->ReleaseStringUTFChars(text, actual);
    env->DeleteLocalRef(text);
    return !clearJniException(env) && ok;
}

static bool verifyIntegrityNative(JNIEnv* env, jobject context) {
    if (!env || !context) return false;

    try {
        clearJniException(env);

        jclass contextCls = env->GetObjectClass(context);
        if (!contextCls || clearJniException(env)) return false;

        jmethodID getPackageName = env->GetMethodID(
            contextCls, "getPackageName", "()Ljava/lang/String;");
        jmethodID getPackageManager = env->GetMethodID(
            contextCls, "getPackageManager",
            "()Landroid/content/pm/PackageManager;");
        if (!getPackageName || !getPackageManager || clearJniException(env)) return false;

        jstring packageName = static_cast<jstring>(
            env->CallObjectMethod(context, getPackageName));
        if (!packageName || clearJniException(env)) return false;

        const char* packageUtf = env->GetStringUTFChars(packageName, nullptr);
        if (!packageUtf || clearJniException(env)) return false;
        bool packageOk = std::strcmp(
            packageUtf, static_cast<char*>(OBFUSCATE("com.rena.w4b"))
        ) == 0;
        env->ReleaseStringUTFChars(packageName, packageUtf);
        if (!packageOk || clearJniException(env)) return false;

        jobject pm = env->CallObjectMethod(context, getPackageManager);
        if (!pm || clearJniException(env)) return false;

        jclass pmCls = env->GetObjectClass(pm);
        if (!pmCls || clearJniException(env)) return false;

        jmethodID getAppInfo = env->GetMethodID(
            pmCls, "getApplicationInfo",
            "(Ljava/lang/String;I)Landroid/content/pm/ApplicationInfo;");
        if (!getAppInfo || clearJniException(env)) return false;

        jobject appInfo = env->CallObjectMethod(pm, getAppInfo, packageName, 0);
        if (!appInfo || clearJniException(env)) return false;

        jclass appInfoCls = env->GetObjectClass(appInfo);
        if (!appInfoCls || clearJniException(env)) return false;
        jfieldID classNameField = env->GetFieldID(
            appInfoCls, "className", "Ljava/lang/String;");
        if (!classNameField || clearJniException(env)) return false;

        jstring className = static_cast<jstring>(
            env->GetObjectField(appInfo, classNameField));
        if (!className || clearJniException(env)) return false;

        if (!nativeCheckString(env, className, OBFUSCATE("com.rena.w4b.RenaApplication"))
                || clearJniException(env)) {
            return false;
        }

        jclass applicationBaseCls = env->FindClass("android/app/Application");
        if (!applicationBaseCls || clearJniException(env)) return false;

        jclass renaApplicationCls = env->FindClass("com/rena/w4b/RenaApplication");
        if (!renaApplicationCls || clearJniException(env)) return false;

        jclass renaSuperclass = env->GetSuperclass(renaApplicationCls);
        if (!renaSuperclass || clearJniException(env)) return false;
        bool directApplication = env->IsSameObject(renaSuperclass, applicationBaseCls);
        env->DeleteLocalRef(renaSuperclass);
        if (!directApplication || clearJniException(env)) return false;

        jmethodID getApplicationContext = env->GetMethodID(
            contextCls, "getApplicationContext", "()Landroid/content/Context;");
        if (!getApplicationContext || clearJniException(env)) return false;

        jobject runtimeApplication = env->CallObjectMethod(
            context, getApplicationContext);
        if (!runtimeApplication || clearJniException(env)) return false;

        jclass runtimeApplicationCls = env->GetObjectClass(runtimeApplication);
        if (!runtimeApplicationCls || clearJniException(env)) return false;
        bool exactRuntimeClass = env->IsSameObject(
            runtimeApplicationCls, renaApplicationCls);
        bool isApplication = env->IsInstanceOf(
            runtimeApplication, applicationBaseCls);
        env->DeleteLocalRef(runtimeApplicationCls);
        env->DeleteLocalRef(runtimeApplication);
        if (!exactRuntimeClass || !isApplication || clearJniException(env)) return false;

        jclass buildVersionCls = env->FindClass("android/os/Build$VERSION");
        if (!buildVersionCls || clearJniException(env)) return false;
        jfieldID sdkField = env->GetStaticFieldID(buildVersionCls, "SDK_INT", "I");
        if (!sdkField || clearJniException(env)) return false;
        jint sdk = env->GetStaticIntField(buildVersionCls, sdkField);
        if (clearJniException(env)) return false;

        jmethodID getPackageInfo = env->GetMethodID(
            pmCls, "getPackageInfo",
            "(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;");
        if (!getPackageInfo || clearJniException(env)) return false;

        jint flags;
        if (sdk >= 28) {
            jfieldID f = env->GetStaticFieldID(
                pmCls, "GET_SIGNING_CERTIFICATES", "I");
            if (!f || clearJniException(env)) return false;
            flags = env->GetStaticIntField(pmCls, f);
        } else {
            jfieldID f = env->GetStaticFieldID(pmCls, "GET_SIGNATURES", "I");
            if (!f || clearJniException(env)) return false;
            flags = env->GetStaticIntField(pmCls, f);
        }
        if (clearJniException(env)) return false;

        jobject packageInfo = env->CallObjectMethod(pm, getPackageInfo, packageName, flags);
        if (!packageInfo || clearJniException(env)) return false;

        jclass packageInfoCls = env->GetObjectClass(packageInfo);
        if (!packageInfoCls || clearJniException(env)) return false;
        jobject signature = nullptr;

        if (sdk >= 28) {
            jfieldID signingInfoField = env->GetFieldID(
                packageInfoCls, "signingInfo", "Landroid/content/pm/SigningInfo;");
            if (!signingInfoField || clearJniException(env)) return false;
            jobject signingInfo = env->GetObjectField(packageInfo, signingInfoField);
            if (!signingInfo || clearJniException(env)) return false;
            jclass signingInfoCls = env->GetObjectClass(signingInfo);
            if (!signingInfoCls || clearJniException(env)) return false;

            jmethodID hasMultiple = env->GetMethodID(
                signingInfoCls, "hasMultipleSigners", "()Z");
            jmethodID getApkSigners = env->GetMethodID(
                signingInfoCls, "getApkContentsSigners",
                "()[Landroid/content/pm/Signature;");
            jmethodID getHistory = env->GetMethodID(
                signingInfoCls, "getSigningCertificateHistory",
                "()[Landroid/content/pm/Signature;");
            if (!hasMultiple || !getApkSigners || !getHistory || clearJniException(env)) return false;

            jboolean multiple = env->CallBooleanMethod(signingInfo, hasMultiple);
            if (clearJniException(env)) return false;
            jobjectArray signatures = static_cast<jobjectArray>(
                env->CallObjectMethod(signingInfo, multiple ? getApkSigners : getHistory));
            if (!signatures || clearJniException(env)) return false;
            jsize count = env->GetArrayLength(signatures);
            if (count <= 0 || clearJniException(env)) return false;
            signature = env->GetObjectArrayElement(signatures, 0);
        } else {
            jfieldID signaturesField = env->GetFieldID(
                packageInfoCls, "signatures", "[Landroid/content/pm/Signature;");
            if (!signaturesField || clearJniException(env)) return false;
            jobjectArray signatures = static_cast<jobjectArray>(
                env->GetObjectField(packageInfo, signaturesField));
            if (!signatures || clearJniException(env)) return false;
            jsize count = env->GetArrayLength(signatures);
            if (count <= 0 || clearJniException(env)) return false;
            signature = env->GetObjectArrayElement(signatures, 0);
        }
        if (!signature || clearJniException(env)) return false;

        jclass digestCls = env->FindClass("java/security/MessageDigest");
        if (!digestCls || clearJniException(env)) return false;
        jmethodID getInstance = env->GetStaticMethodID(
            digestCls, "getInstance",
            "(Ljava/lang/String;)Ljava/security/MessageDigest;");
        jmethodID digestMethod = env->GetMethodID(
            digestCls, "digest", "([B)[B");
        if (!getInstance || !digestMethod || clearJniException(env)) return false;

        jstring shaName = env->NewStringUTF(static_cast<char*>(OBFUSCATE("SHA-256")));
        if (!shaName || clearJniException(env)) return false;
        jobject md = env->CallStaticObjectMethod(digestCls, getInstance, shaName);
        if (!md || clearJniException(env)) return false;

        jclass sigCls = env->GetObjectClass(signature);
        if (!sigCls || clearJniException(env)) return false;
        jmethodID toByteArray = env->GetMethodID(sigCls, "toByteArray", "()[B");
        if (!toByteArray || clearJniException(env)) return false;
        jbyteArray certBytes = static_cast<jbyteArray>(
            env->CallObjectMethod(signature, toByteArray));
        if (!certBytes || clearJniException(env)) return false;
        jbyteArray digest = static_cast<jbyteArray>(
            env->CallObjectMethod(md, digestMethod, certBytes));
        if (!digest || clearJniException(env)) return false;
        jsize len = env->GetArrayLength(digest);
        if (len != 32 || clearJniException(env)) return false;

        jbyte* raw = env->GetByteArrayElements(digest, nullptr);
        if (!raw || clearJniException(env)) return false;
        char hex[65] = {};
        const char* kHex = OBFUSCATE("0123456789abcdef");
        for (int i = 0; i < 32; ++i) {
            unsigned char b = static_cast<unsigned char>(raw[i]);
            hex[i * 2] = kHex[(b >> 4) & 0x0F];
            hex[i * 2 + 1] = kHex[b & 0x0F];
        }
        env->ReleaseByteArrayElements(digest, raw, JNI_ABORT);
        if (clearJniException(env)) return false;

        bool signatureOk = std::strcmp(
            hex,
            OBFUSCATE("e4201e2e32724c1ba1ef1100d35ff9f75c5d3e888a58c68b7747808f4c87607b")
        ) == 0;

        bool debuggerAttached = false;
        jclass debugCls = env->FindClass("android/os/Debug");
        if (debugCls && !clearJniException(env)) {
            jmethodID connected = env->GetStaticMethodID(
                debugCls, "isDebuggerConnected", "()Z");
            jmethodID waiting = env->GetStaticMethodID(
                debugCls, "waitingForDebugger", "()Z");
            if (connected && waiting && !clearJniException(env)) {
                debuggerAttached = env->CallStaticBooleanMethod(debugCls, connected) ||
                        env->CallStaticBooleanMethod(debugCls, waiting);
                if (clearJniException(env)) return false;
            } else {
                clearJniException(env);
            }
        } else {
            clearJniException(env);
        }

        std::ifstream proc(static_cast<char*>(OBFUSCATE("/proc/self/status")));
        bool tracerClean = true;
        if (proc.is_open()) {
            std::string line;
            while (std::getline(proc, line)) {
                const char* tracerPrefix = OBFUSCATE("TracerPid:");
                if (line.compare(0, 10, tracerPrefix) == 0) {
                    std::string value = line.substr(10);
                    while (!value.empty() && (value[0] == ' ' || value[0] == '\t')) {
                        value.erase(0, 1);
                    }
                    tracerClean = value == static_cast<char*>(OBFUSCATE("0"));
                    break;
                }
            }
        }

        return signatureOk && !debuggerAttached && tracerClean;
    } catch (...) {
        clearJniException(env);
        return false;
    }
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
    return js(env, OBFUSCATE("Donate to Support the Developer"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(donateUrl)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("https://sociabuzz.com/ridhoae303"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(developerAvatarUrl)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("https://avatars.githubusercontent.com/u/173559040?v=4"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(githubRequestFailedText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("GitHub request failed."));
}


extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(easterEggFoundText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Congratulations! You found this simple and silly Easter egg."));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(teamDeveloperText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Developer & Contributor"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(officialTeamText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("The Official Team"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(contributorText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Contributor"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(teamLoadingText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Loading contributors..."));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(teamRequestFailedText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Unable to load contributors right now."));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(teamNoContributorsText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("No contributors were returned by GitHub."));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(teamGithubContentDescription)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Open GitHub profile"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(teamBackContentDescription)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Back"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(appLockText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("App Lock"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(appLockTitle)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("App Lock"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(appLockBackText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Back"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(appLockBackSymbol)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("‹"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(appLockFingerprintUnavailable)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Fingerprint unlock is unavailable on this device."));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(appLockHeroTitle)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Protect your app"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(appLockHeroMessage)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Keep your WhatsApp Web sessions and app data untouched while adding a local access lock."));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(appLockSwitchText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("PIN Lock"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(appLockTimeoutLabel)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Lock timing"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(appLockTimeoutText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Lock the app after"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(appLockFingerprintText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Fingerprint unlock"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(appLockEnableFirstText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Enable PIN Lock first."));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(appLockSecurityNote)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Protect your private chats and keep your WhatsApp Web conversations behind a local app lock."));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(changePinLabel)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Change PIN"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(changePinText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Change your PIN"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(changePinArrow)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("›"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(currentPinTitle)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Verify your current PIN"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(currentPinHint)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Current PIN"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(newPinTitle)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Set a new PIN"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(newPinMessage)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Change the PIN used to protect your private chats and app data."));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(pinChangedText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("PIN changed successfully."));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(currentPinInvalidText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Current PIN is incorrect."));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(appLockTimeoutDialogTitle)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Lock timing"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(timeoutImmediate)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Immediately"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(timeoutOneMinute)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("After 1 minute"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(timeoutFiveMinutes)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("After 5 minutes"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(timeoutFifteenMinutes)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("After 15 minutes"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(timeoutThirtyMinutes)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("After 30 minutes"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(timeoutOneHour)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("After 1 hour"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(timeoutFiveHours)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("After 5 hours"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(timeoutTenHours)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("After 10 hours"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(createPinTitle)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Create your PIN"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(createPinMessage)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Create a 4-digit PIN used only to unlock Rena W4B. Your PIN is stored as a salted PBKDF2 hash."));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(pinHint)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Enter PIN"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(confirmPinHint)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Confirm PIN"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(savePinText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Save PIN"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(pinLengthText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Your PIN must contain exactly 4 digits."));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(pinMismatchText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("The PINs do not match."));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(pinSaveFailedText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("The PIN could not be saved securely."));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(lockScreenTitle)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Tell me who you are"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(lockScreenMessage)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Unlock Rena W4B to continue."));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(lockPinHint)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Enter your PIN"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(unlockText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Unlock"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(fingerprintUnlockText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Use fingerprint"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(lockScreenFooter)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("App Lock protects access to Rena W4B. It does not clear WhatsApp Web cookies or session data."));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(incorrectPinText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Incorrect PIN."));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(fingerprintFailedText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Fingerprint not recognized."));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(fingerprintCanceledText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Fingerprint authentication was canceled."));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(fingerprintEnabledText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Fingerprint unlock enabled."));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(hideNotificationLabel)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Disable Notification"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(hideNotificationEnabledText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Notifications are now silent."));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(hideNotificationDisabledText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Notifications are now audible."));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(hideBarNotificationLabel)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Hide Bar Notification"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(hideBarNotificationEnabledText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Android notification bar hidden."));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(hideBarNotificationDisabledText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Android notification bar visible."));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(reduceAnimationLabel)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Reduce Animation"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(reduceAnimationEnabledText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Reduced animations enabled."));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(reduceAnimationDisabledText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Full animations enabled."));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(notificationSilentChannelName)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("WhatsApp Web (Silent)"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(notificationSilentChannelDescription)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Silent WhatsApp Web notifications."));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(contributorsApiUrl)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("https://api.github.com/repos/ridhoae303/Rena-w4b/contributors?anon=1&per_page=100"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(githubAvatarUrl)(JNIEnv* env, jclass, jstring username) {
    if (!env) return nullptr;
    const char* user = username ? env->GetStringUTFChars(username, nullptr) : nullptr;
    if (username && (!user || clearJniException(env))) return nullptr;

    std::string url = std::string(OBFUSCATE("https://github.com/"))
        + (user ? user : "")
        + std::string(OBFUSCATE(".png?size=256"));

    if (user) env->ReleaseStringUTFChars(username, user);
    if (clearJniException(env)) return nullptr;
    return js(env, url.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(githubProfileUrl)(JNIEnv* env, jclass, jstring username) {
    if (!env) return nullptr;
    const char* user = username ? env->GetStringUTFChars(username, nullptr) : nullptr;
    if (username && (!user || clearJniException(env))) return nullptr;

    std::string url = std::string(OBFUSCATE("https://github.com/"))
        + (user ? user : "");

    if (user) env->ReleaseStringUTFChars(username, user);
    if (clearJniException(env)) return nullptr;
    return js(env, url.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(officialTeamName1)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Mohammed Ridho"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(officialTeamUsername1)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("ridhoae303"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(officialTeamRole1)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Main Developer"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(officialTeamName2)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Hatta"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(officialTeamUsername2)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("hajacmyk"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(officialTeamRole2)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Official Team"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(officialTeamName3)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Dimas"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(officialTeamUsername3)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("syputraa572"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(officialTeamRole3)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Official Team"));
}


static std::string signingSha256FromPackageInfo(JNIEnv* env, jobject packageInfo, jint sdk) {
    if (!env || !packageInfo) return std::string();

    try {
        clearJniException(env);
        jclass packageInfoCls = env->GetObjectClass(packageInfo);
        if (!packageInfoCls || clearJniException(env)) return std::string();

        jobject signature = nullptr;

        if (sdk >= 28) {
            jfieldID signingInfoField = env->GetFieldID(
                packageInfoCls, "signingInfo", "Landroid/content/pm/SigningInfo;");
            if (!signingInfoField || clearJniException(env)) return std::string();

            jobject signingInfo = env->GetObjectField(packageInfo, signingInfoField);
            if (!signingInfo || clearJniException(env)) return std::string();

            jclass signingInfoCls = env->GetObjectClass(signingInfo);
            if (!signingInfoCls || clearJniException(env)) return std::string();

            jmethodID hasMultiple = env->GetMethodID(signingInfoCls, "hasMultipleSigners", "()Z");
            jmethodID getApkSigners = env->GetMethodID(
                signingInfoCls, "getApkContentsSigners", "()[Landroid/content/pm/Signature;");
            jmethodID getHistory = env->GetMethodID(
                signingInfoCls, "getSigningCertificateHistory", "()[Landroid/content/pm/Signature;");
            if (!hasMultiple || !getApkSigners || !getHistory || clearJniException(env)) return std::string();

            jboolean multiple = env->CallBooleanMethod(signingInfo, hasMultiple);
            if (clearJniException(env)) return std::string();

            jobjectArray signatures = static_cast<jobjectArray>(
                env->CallObjectMethod(signingInfo, multiple ? getApkSigners : getHistory));
            if (!signatures || clearJniException(env)) return std::string();

            jsize count = env->GetArrayLength(signatures);
            if (count <= 0 || clearJniException(env)) return std::string();
            signature = env->GetObjectArrayElement(signatures, 0);
            if (!signature || clearJniException(env)) return std::string();
        } else {
            jfieldID signaturesField = env->GetFieldID(
                packageInfoCls, "signatures", "[Landroid/content/pm/Signature;");
            if (!signaturesField || clearJniException(env)) return std::string();

            jobjectArray signatures = static_cast<jobjectArray>(
                env->GetObjectField(packageInfo, signaturesField));
            if (!signatures || clearJniException(env)) return std::string();

            jsize count = env->GetArrayLength(signatures);
            if (count <= 0 || clearJniException(env)) return std::string();
            signature = env->GetObjectArrayElement(signatures, 0);
            if (!signature || clearJniException(env)) return std::string();
        }

        jclass digestCls = env->FindClass("java/security/MessageDigest");
        if (!digestCls || clearJniException(env)) return std::string();

        jmethodID getInstance = env->GetStaticMethodID(
            digestCls, "getInstance", "(Ljava/lang/String;)Ljava/security/MessageDigest;");
        jmethodID digestMethod = env->GetMethodID(digestCls, "digest", "([B)[B");
        if (!getInstance || !digestMethod || clearJniException(env)) return std::string();

        jstring shaName = env->NewStringUTF(static_cast<char*>(OBFUSCATE("SHA-256")));
        if (!shaName || clearJniException(env)) return std::string();

        jobject md = env->CallStaticObjectMethod(digestCls, getInstance, shaName);
        if (!md || clearJniException(env)) return std::string();

        jclass sigCls = env->GetObjectClass(signature);
        if (!sigCls || clearJniException(env)) return std::string();

        jmethodID toByteArray = env->GetMethodID(sigCls, "toByteArray", "()[B");
        if (!toByteArray || clearJniException(env)) return std::string();

        jbyteArray certBytes = static_cast<jbyteArray>(
            env->CallObjectMethod(signature, toByteArray));
        if (!certBytes || clearJniException(env)) return std::string();

        jbyteArray digest = static_cast<jbyteArray>(
            env->CallObjectMethod(md, digestMethod, certBytes));
        if (!digest || clearJniException(env)) return std::string();

        jsize len = env->GetArrayLength(digest);
        if (len != 32 || clearJniException(env)) return std::string();

        jbyte* raw = env->GetByteArrayElements(digest, nullptr);
        if (!raw || clearJniException(env)) return std::string();

        std::string hex(64, '0');
        const char* kHex = OBFUSCATE("0123456789abcdef");
        for (int i = 0; i < 32; ++i) {
            unsigned char b = static_cast<unsigned char>(raw[i]);
            hex[i * 2] = kHex[(b >> 4) & 0x0F];
            hex[i * 2 + 1] = kHex[b & 0x0F];
        }

        env->ReleaseByteArrayElements(digest, raw, JNI_ABORT);
        if (clearJniException(env)) return std::string();
        return hex;
    } catch (...) {
        clearJniException(env);
        return std::string();
    }
}

static bool verifyArchiveSignerNative(JNIEnv* env, jobject context, jstring apkPath) {
    if (!env || !context || !apkPath) return false;

    try {
        clearJniException(env);

        jclass contextCls = env->GetObjectClass(context);
        if (!contextCls || clearJniException(env)) return false;

        jmethodID getPackageName = env->GetMethodID(
            contextCls, "getPackageName", "()Ljava/lang/String;");
        jmethodID getPackageManager = env->GetMethodID(
            contextCls, "getPackageManager", "()Landroid/content/pm/PackageManager;");
        if (!getPackageName || !getPackageManager || clearJniException(env)) return false;

        jstring packageName = static_cast<jstring>(
            env->CallObjectMethod(context, getPackageName));
        if (!packageName || clearJniException(env)) return false;

        jobject pm = env->CallObjectMethod(context, getPackageManager);
        if (!pm || clearJniException(env)) return false;

        jclass buildVersionCls = env->FindClass("android/os/Build$VERSION");
        if (!buildVersionCls || clearJniException(env)) return false;
        jfieldID sdkField = env->GetStaticFieldID(buildVersionCls, "SDK_INT", "I");
        if (!sdkField || clearJniException(env)) return false;
        jint sdk = env->GetStaticIntField(buildVersionCls, sdkField);
        if (clearJniException(env)) return false;

        jclass pmCls = env->GetObjectClass(pm);
        if (!pmCls || clearJniException(env)) return false;
        jmethodID archiveInfo = env->GetMethodID(
            pmCls, "getPackageArchiveInfo",
            "(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;");
        jmethodID currentInfo = env->GetMethodID(
            pmCls, "getPackageInfo",
            "(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;");
        if (!archiveInfo || !currentInfo || clearJniException(env)) return false;

        jint archiveFlags = sdk >= 28 ? 0x08000000 : 0x00000040;
        jobject archive = env->CallObjectMethod(pm, archiveInfo, apkPath, archiveFlags);
        if (!archive || clearJniException(env)) return false;

        jclass packageInfoCls = env->GetObjectClass(archive);
        if (!packageInfoCls || clearJniException(env)) return false;
        jfieldID pkgField = env->GetFieldID(
            packageInfoCls, "packageName", "Ljava/lang/String;");
        if (!pkgField || clearJniException(env)) return false;

        jstring archivePackage = static_cast<jstring>(env->GetObjectField(archive, pkgField));
        if (!archivePackage || clearJniException(env)) return false;

        if (!nativeCheckString(env, archivePackage, OBFUSCATE("com.rena.w4b")) ||
            clearJniException(env)) {
            return false;
        }

        jobject current = env->CallObjectMethod(pm, currentInfo, packageName, archiveFlags);
        if (!current || clearJniException(env)) return false;

        std::string archiveHash = signingSha256FromPackageInfo(env, archive, sdk);
        if (clearJniException(env) || archiveHash.empty()) return false;

        std::string currentHash = signingSha256FromPackageInfo(env, current, sdk);
        if (clearJniException(env) || currentHash.empty()) return false;

        return archiveHash == currentHash;
    } catch (...) {
        clearJniException(env);
        return false;
    }
}

extern "C" jint JNI_OnLoad(JavaVM* vm, void*) {
    if (!vm) return JNI_ERR;

    JNIEnv* env = nullptr;
    jint getEnvResult = vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6);
    if (getEnvResult != JNI_OK || !env) {
        return JNI_ERR;
    }

    // Do not execute integrity verification while the native library is loading.
    // Startup verification is explicitly initiated from SplashActivity after the
    // Application and Java runtime are fully initialized.
    return JNI_VERSION_1_6;
}

extern "C" JNIEXPORT jboolean JNICALL
JNI_METHOD(verifyApkSigner)(JNIEnv* env, jclass, jobject context, jstring apkPath) {
    if (!env || !context || !apkPath) return JNI_FALSE;
    try {
        bool result = verifyArchiveSignerNative(env, context, apkPath);
        if (clearJniException(env)) return JNI_FALSE;
        return result ? JNI_TRUE : JNI_FALSE;
    } catch (...) {
        clearJniException(env);
        return JNI_FALSE;
    }
}

extern "C" JNIEXPORT jboolean JNICALL
JNI_METHOD(verifyIntegrity)(JNIEnv* env, jclass, jobject context) {
    if (!env || !context) return JNI_FALSE;
    try {
        return verifyIntegrityNative(env, context) ? JNI_TRUE : JNI_FALSE;
    } catch (...) {
        clearJniException(env);
        return JNI_FALSE;
    }
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
    return js(env, OBFUSCATE("Zoom"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(zoomEnabledText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Zoom enabled."));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(zoomDisabledText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Zoom disabled."));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(switchTogglesText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Switch Toggles"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(refreshText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Refresh Page"));
}


extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(socialMediaText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Social Media"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(checkingUpdatesText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Checking for updates..."));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(hideMenuToastText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Three-dot button hidden. Shake your phone to open navigation."));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(storageWarningText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Keeping more tabs open can increase storage usage."));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(refreshContentDescription)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Refresh WhatsApp Web"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(exitToastText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Returning to the home page. Press Back again to exit."));
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

    if (p.find(std::string(OBFUSCATE("CAMERA"))) != std::string::npos) {
        return js(env, OBFUSCATE("Camera access"));
    }
    if (p.find(std::string(OBFUSCATE("RECORD_AUDIO"))) != std::string::npos) {
        return js(env, OBFUSCATE("Microphone access"));
    }
    if (p.find(std::string(OBFUSCATE("POST_NOTIFICATIONS"))) != std::string::npos) {
        return js(env, OBFUSCATE("Notifications"));
    }
    if (p.find(std::string(OBFUSCATE("READ_EXTERNAL_STORAGE"))) != std::string::npos) {
        return js(env, OBFUSCATE("File access"));
    }

    return js(env, OBFUSCATE("Permission needed"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(permissionMessage)(JNIEnv* env, jclass, jstring permission) {
    std::string p = permissionName(env, permission);

    if (p.find(std::string(OBFUSCATE("CAMERA"))) != std::string::npos) {
        return js(env, OBFUSCATE(
            "Allow camera access so WhatsApp Web can use your camera for supported features."
        ));
    }

    if (p.find(std::string(OBFUSCATE("RECORD_AUDIO"))) != std::string::npos) {
        return js(env, OBFUSCATE(
            "Allow microphone access so WhatsApp Web can use voice and video features."
        ));
    }

    if (p.find(std::string(OBFUSCATE("POST_NOTIFICATIONS"))) != std::string::npos) {
        return js(env, OBFUSCATE(
            "Allow notifications so supported WhatsApp Web notifications can be shown."
        ));
    }

    if (p.find(std::string(OBFUSCATE("READ_EXTERNAL_STORAGE"))) != std::string::npos) {
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

    if (p.find(std::string(OBFUSCATE("CAMERA"))) != std::string::npos) {
        return js(env, OBFUSCATE(
            "Camera access was denied. You can enable it later in Settings > Apps > Rena W4B > Permissions."
        ));
    }

    if (p.find(std::string(OBFUSCATE("RECORD_AUDIO"))) != std::string::npos) {
        return js(env, OBFUSCATE(
            "Microphone access was denied. You can enable it later in Settings > Apps > Rena W4B > Permissions."
        ));
    }

    if (p.find(std::string(OBFUSCATE("POST_NOTIFICATIONS"))) != std::string::npos) {
        return js(env, OBFUSCATE(
            "Notifications were denied. You can enable them later in Settings > Apps > Rena W4B > Notifications."
        ));
    }

    if (p.find(std::string(OBFUSCATE("READ_EXTERNAL_STORAGE"))) != std::string::npos) {
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
JNI_METHOD(integrityFailedTitle)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Security check failed"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(integrityCloseText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Close"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(integrityErrorMessage)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Application integrity check failed."));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(hideMenuLabel)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Hide Three-Dot Button"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(hideMenuMessage)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE(
        "The three-dot menu will be hidden. Shake your phone to open the navigation menu."
    ));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(tooManyTabsTitle)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Too Many Tabs Open"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(newTabText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("+ New Tab"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(closeAllTabsText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Close All Tabs"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(tabLimitText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("You can have up to 10 tabs."));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(tabProfilesUnavailableText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("This WebView provider does not support isolated tab profiles, so another account cannot be opened safely here."));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(closeAllTabsMessage)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Close every tab except Tab 1? Your Tab 1 session will stay intact."));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(noJokesText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("No jokes in this app."));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(cancelText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Cancel"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(notNowText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Not now"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(ignoreText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Ignore"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(clearText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Clear"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(dataStorageText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Data & Storage"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(checkUpdatesText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Check for Updates"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(updateAvailableTitle)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Update Available"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(upToDateText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("You're up to date."));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(updateInstallPermissionTitle)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Allow App Installs"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(updateInstallPermissionMessage)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Allow Rena W4B to install updates from this source, then come back and check again."));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(downloadUpdateText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Download & Install"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(updateDigestMissingText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("The release did not provide a verified APK digest. Open the release page and verify the download manually."));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(updateDownloadFailedText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("The update could not be downloaded safely."));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(supportDevelopersText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Support Developers"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(starRepositoryText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Star this Repository"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(githubRepositoryUrl)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("https://github.com/ridhoae303/Rena-w4b"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(updaterApiUrl)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("https://api.github.com/repos/ridhoae303/Rena-w4b/releases/latest"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(applicationIntegrityTitle)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Application Integrity"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(dataCookiesText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Cookies & Site Data"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(dataCacheText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Cached Files"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(dataAllWebText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Clear All Web Data"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(dataClearedText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Web data cleared."));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(updateNotAvailableText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("This version is still valid. No downgrade will be performed."));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(accountsTabsText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Accounts / Tabs"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(toolsText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Tools"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(tabPrefix)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Tab "));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(clearCookiesMessage)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("This will clear cookies and site data and sign you out of the current web session."));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(clearAllWebMessage)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("This will clear cookies, site data, WebView cache, and related web data. Downloaded files in Rena will stay untouched."));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(cacheClearedText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("WebView cache cleared. Your session was kept."));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(storageAccessTitle)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Allow File Access"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(storageAccessMessage)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Allow Rena W4B to access its Rena folder for downloads and stored media."));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(storageSettingsButton)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Open Settings"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(downloadDeniedText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("File access is required before Rena W4B can save this download."));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(closeTabSymbol)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("×"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(okayText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Okay"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(backgroundAccessTitle)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Background access (optional)"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(backgroundAccessMessage)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("This setting is optional. Android may restrict background activity. Review it only if you want Rena W4B to keep working more reliably while it is in the background."));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(batteryOptimizationTitle)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Battery optimization (optional)"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(batteryOptimizationMessage)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("This setting is optional. Reducing battery restrictions can help long-running web sessions, but you can safely choose Not now and change it later in Android settings."));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(storageAlreadyHandledText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("File access is already configured."));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(noInternetTitle)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("No internet connection"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(noInternetMessage)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Rena W4B cannot detect an internet connection. You can exit the application or continue offline."));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(noInternetConnectionText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("No internet connection"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(poorInternetConnectionText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Loading is taking longer than usual. The network may be slow, or WhatsApp servers may be busy."));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(exitText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Exit"));
}


extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(notificationOpenText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Open"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(notificationReplyText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Reply"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(notificationReadText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Read"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(notificationIgnoreText)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Ignore"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(notificationChannelName)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("WhatsApp Web"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(notificationChannelDescription)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Notifications from trusted WhatsApp Web content."));
}
