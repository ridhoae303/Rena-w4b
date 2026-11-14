#include <jni.h>
#include <string>
#include <fstream>
#include <cstring>
#include <cstdint>
#include <cstdlib>
#include <dlfcn.h>
#include <sys/stat.h>
#include <obfuscate.h>

#define JNI_METHOD(name) Java_com_rena_w4b_NativeConfig_##name

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

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(unsupportedAndroidTitle)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Unsupported Android Version"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(unsupportedAndroidMessage)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Rena W4B requires Android 8.0 (API 26) or later."));
}

static bool is_traced() {
    const std::string procPath(OBFUSCATE("/proc/self/status"));
    const std::string tracerPrefix(OBFUSCATE("TracerPid:"));
    const std::string zero(OBFUSCATE("0"));
    std::ifstream f(procPath.c_str());
    if (!f.is_open()) {
        return false;
    }

    std::string line;
    while (std::getline(f, line)) {
        if (line.compare(0, 10, tracerPrefix.c_str()) == 0) {
            std::string v = line.substr(10);
            while (!v.empty() && (v[0] == ' ' || v[0] == '\t')) {
                v.erase(0, 1);
            }
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
            packageUtf, static_cast<const char*>(OBFUSCATE("com.rena.w4b"))
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

        jfieldID appPackageField = env->GetFieldID(
            appInfoCls, "packageName", "Ljava/lang/String;");
        jfieldID uidField = env->GetFieldID(appInfoCls, "uid", "I");
        jfieldID sourceDirField = env->GetFieldID(
            appInfoCls, "sourceDir", "Ljava/lang/String;");
        jfieldID nativeLibraryDirField = env->GetFieldID(
            appInfoCls, "nativeLibraryDir", "Ljava/lang/String;");
        if (!appPackageField || !uidField || !sourceDirField ||
            !nativeLibraryDirField || clearJniException(env)) {
            return false;
        }

        jstring appPackageName = static_cast<jstring>(
            env->GetObjectField(appInfo, appPackageField));
        if (!appPackageName || !nativeCheckString(
                env, appPackageName, OBFUSCATE("com.rena.w4b")) ||
            clearJniException(env)) {
            return false;
        }

        jint appUid = env->GetIntField(appInfo, uidField);
        jclass processCls = env->FindClass("android/os/Process");
        if (!processCls || clearJniException(env)) return false;
        jmethodID myUid = env->GetStaticMethodID(processCls, "myUid", "()I");
        if (!myUid || clearJniException(env)) return false;
        jint runtimeUid = env->CallStaticIntMethod(processCls, myUid);
        if (clearJniException(env) || runtimeUid != appUid) return false;

        jmethodID getPackagesForUid = env->GetMethodID(
            pmCls, "getPackagesForUid", "(I)[Ljava/lang/String;");
        if (!getPackagesForUid || clearJniException(env)) return false;
        jobjectArray packages = static_cast<jobjectArray>(
            env->CallObjectMethod(pm, getPackagesForUid, runtimeUid));
        if (!packages || clearJniException(env)) return false;
        bool uidPackageMatch = false;
        const jsize packageCount = env->GetArrayLength(packages);
        for (jsize i = 0; i < packageCount; ++i) {
            jstring item = static_cast<jstring>(
                env->GetObjectArrayElement(packages, i));
            if (!item || clearJniException(env)) return false;
            if (nativeCheckString(env, item, OBFUSCATE("com.rena.w4b"))) {
                uidPackageMatch = true;
            }
            env->DeleteLocalRef(item);
            if (uidPackageMatch) break;
        }
        if (!uidPackageMatch || clearJniException(env)) return false;

        jstring sourceDir = static_cast<jstring>(
            env->GetObjectField(appInfo, sourceDirField));
        jstring nativeLibraryDir = static_cast<jstring>(
            env->GetObjectField(appInfo, nativeLibraryDirField));
        if (!sourceDir || !nativeLibraryDir || clearJniException(env)) return false;

        const char* sourcePath = env->GetStringUTFChars(sourceDir, nullptr);
        const char* nativeLibDir = env->GetStringUTFChars(nativeLibraryDir, nullptr);
        if (!sourcePath || !nativeLibDir || clearJniException(env)) return false;

        struct stat sourceStat{};
        const bool sourceIsRegularFile =
            (stat(sourcePath, &sourceStat) == 0) && S_ISREG(sourceStat.st_mode);
        const std::string nativeDirValue(nativeLibDir);
        env->ReleaseStringUTFChars(sourceDir, sourcePath);
        env->ReleaseStringUTFChars(nativeLibraryDir, nativeLibDir);
        if (!sourceIsRegularFile || clearJniException(env)) return false;

        Dl_info libInfo{};
        if (dladdr(
                reinterpret_cast<void*>(
                    reinterpret_cast<uintptr_t>(&verifyIntegrityNative)),
                &libInfo) != 0 &&
            libInfo.dli_fname != nullptr &&
            !nativeDirValue.empty()) {
            const std::string loadedPath(libInfo.dli_fname);
            if (loadedPath.compare(0, nativeDirValue.size(), nativeDirValue) != 0) {
                return false;
            }
        }

        std::ifstream cmdline(static_cast<const char*>(OBFUSCATE("/proc/self/cmdline")),
                              std::ios::in | std::ios::binary);
        if (cmdline.is_open()) {
            char processName[256] = {};
            cmdline.read(processName, sizeof(processName) - 1);
            const std::streamsize readCount = cmdline.gcount();
            if (readCount > 0) {
                const std::string runtimeProcess(processName,
                    static_cast<size_t>(readCount > 0 ? readCount : 0));
                const size_t nul = runtimeProcess.find('\0');
                const std::string processId =
                    runtimeProcess.substr(0, nul == std::string::npos ?
                                             runtimeProcess.size() : nul);
                if (!processId.empty() &&
                    processId != static_cast<const char*>(OBFUSCATE("com.rena.w4b"))) {
                    return false;
                }
            }
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

        jstring shaName = env->NewStringUTF(static_cast<const char*>(OBFUSCATE("SHA-256")));
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
        const std::string kHex(OBFUSCATE("0123456789abcdef"));
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

        const std::string procPath(OBFUSCATE("/proc/self/status"));
        const std::string tracerPrefix(OBFUSCATE("TracerPid:"));
        const std::string zero(OBFUSCATE("0"));
        std::ifstream proc(procPath.c_str());
        bool tracerClean = true;
        if (proc.is_open()) {
            std::string line;
            while (std::getline(proc, line)) {
                if (line.compare(0, 10, tracerPrefix.c_str()) == 0) {
                    std::string value = line.substr(10);
                    while (!value.empty() && (value[0] == ' ' || value[0] == '\t')) {
                        value.erase(0, 1);
                    }
                    tracerClean = value == zero;
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
JNI_METHOD(appLockBackSymbol)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("‹"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(changePinArrow)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("›"));
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
JNI_METHOD(officialTeamName2)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Hatta"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(officialTeamUsername2)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("hajacmyk"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(officialTeamName3)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Dimas"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(officialTeamUsername3)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("syputraa572"));
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

        jstring shaName = env->NewStringUTF(static_cast<const char*>(OBFUSCATE("SHA-256")));
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
        const std::string kHex(OBFUSCATE("0123456789abcdef"));
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

static bool verifyRuntimeBindingNative(JNIEnv* env, jobject context) {
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
        if (!nativeCheckString(env, packageName, OBFUSCATE("com.rena.w4b"))) return false;

        jobject pm = env->CallObjectMethod(context, getPackageManager);
        if (!pm || clearJniException(env)) return false;
        jclass pmCls = env->GetObjectClass(pm);
        if (!pmCls || clearJniException(env)) return false;

        jmethodID getApplicationInfo = env->GetMethodID(
            pmCls, "getApplicationInfo",
            "(Ljava/lang/String;I)Landroid/content/pm/ApplicationInfo;");
        jmethodID getPackagesForUid = env->GetMethodID(
            pmCls, "getPackagesForUid", "(I)[Ljava/lang/String;");
        if (!getApplicationInfo || !getPackagesForUid || clearJniException(env)) return false;

        jobject info = env->CallObjectMethod(pm, getApplicationInfo, packageName, 0);
        if (!info || clearJniException(env)) return false;
        jclass infoCls = env->GetObjectClass(info);
        if (!infoCls || clearJniException(env)) return false;

        jfieldID classNameField = env->GetFieldID(
            infoCls, "className", "Ljava/lang/String;");
        jfieldID uidField = env->GetFieldID(infoCls, "uid", "I");
        jfieldID nativeLibraryDirField = env->GetFieldID(
            infoCls, "nativeLibraryDir", "Ljava/lang/String;");
        if (!classNameField || !uidField || !nativeLibraryDirField || clearJniException(env)) return false;

        jstring className = static_cast<jstring>(env->GetObjectField(info, classNameField));
        if (!className || !nativeCheckString(
                env, className, OBFUSCATE("com.rena.w4b.RenaApplication"))) return false;

        jint appUid = env->GetIntField(info, uidField);
        jclass processCls = env->FindClass("android/os/Process");
        if (!processCls || clearJniException(env)) return false;
        jmethodID myUid = env->GetStaticMethodID(processCls, "myUid", "()I");
        if (!myUid || clearJniException(env)) return false;
        jint runtimeUid = env->CallStaticIntMethod(processCls, myUid);
        if (clearJniException(env) || runtimeUid != appUid) return false;

        jobjectArray packages = static_cast<jobjectArray>(
            env->CallObjectMethod(pm, getPackagesForUid, runtimeUid));
        if (!packages || clearJniException(env)) return false;
        bool packageBound = false;
        const jsize count = env->GetArrayLength(packages);
        for (jsize i = 0; i < count; ++i) {
            jstring item = static_cast<jstring>(
                env->GetObjectArrayElement(packages, i));
            if (!item || clearJniException(env)) return false;
            packageBound = nativeCheckString(
                env, item, OBFUSCATE("com.rena.w4b"));
            env->DeleteLocalRef(item);
            if (packageBound) break;
        }
        if (!packageBound || clearJniException(env)) return false;

        jstring nativeLibraryDir = static_cast<jstring>(
            env->GetObjectField(info, nativeLibraryDirField));
        if (!nativeLibraryDir || clearJniException(env)) return false;
        const char* nativeLibDir = env->GetStringUTFChars(nativeLibraryDir, nullptr);
        if (!nativeLibDir || clearJniException(env)) return false;
        const std::string expectedDir(nativeLibDir);
        env->ReleaseStringUTFChars(nativeLibraryDir, nativeLibDir);
        if (expectedDir.empty() || clearJniException(env)) return false;

        Dl_info libInfo{};
        if (dladdr(
                reinterpret_cast<void*>(
                    reinterpret_cast<uintptr_t>(&verifyRuntimeBindingNative)),
                &libInfo) == 0 ||
            libInfo.dli_fname == nullptr) {
            return false;
        }

        const std::string loadedPath(libInfo.dli_fname);
        if (loadedPath.compare(0, expectedDir.size(), expectedDir) != 0) {
            return false;
        }

        std::ifstream cmdline(
            static_cast<const char*>(OBFUSCATE("/proc/self/cmdline")),
            std::ios::in | std::ios::binary);
        if (!cmdline.is_open()) return false;
        char processName[256] = {};
        cmdline.read(processName, sizeof(processName) - 1);
        if (cmdline.gcount() <= 0) return false;
        const std::string processId(processName);
        if (processId != static_cast<const char*>(OBFUSCATE("com.rena.w4b"))) return false;

        return true;
    } catch (...) {
        clearJniException(env);
        return false;
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

extern "C" JNIEXPORT jboolean JNICALL
JNI_METHOD(verifyRuntimeBinding)(JNIEnv* env, jclass, jobject context) {
    if (!env || !context) return JNI_FALSE;
    try {
        return verifyRuntimeBindingNative(env, context) ? JNI_TRUE : JNI_FALSE;
    } catch (...) {
        clearJniException(env);
        return JNI_FALSE;
    }
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
JNI_METHOD(appFallbackName)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Rena W4B"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(renaImageAssetName)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Rena.jpg"));
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
JNI_METHOD(githubRepositoryUrl)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("https://github.com/ridhoae303/Rena-w4b"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(authoritativeVersionName)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("1.0.9"));
}

extern "C" JNIEXPORT jint JNICALL
JNI_METHOD(authoritativeVersionCode)(JNIEnv* env, jclass) {
    (void)env;
    return static_cast<jint>(std::atoi(OBFUSCATE("19")));
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
JNI_METHOD(closeTabSymbol)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("×"));
}

extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(downloadBackSymbol)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("←"));
}
extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(downloadProgressSeparator)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("  ·  "));
}
extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(downloadBytesSuffix)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE(" B"));
}
extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(downloadKilobytesSuffix)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE(" KB"));
}
extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(downloadMegabytesSuffix)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE(" MB"));
}
extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(downloadGigabytesSuffix)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE(" GB"));
}
extern "C" JNIEXPORT jstring JNICALL
JNI_METHOD(downloadStorageRootName)(JNIEnv* env, jclass) {
    return js(env, OBFUSCATE("Rena"));
}
