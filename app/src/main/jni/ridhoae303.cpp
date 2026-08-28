// Created by ridhoae303

#include <jni.h>
#include <string>
#include <cstdlib>
#include <unistd.h>
#include <signal.h>
#include <sys/mman.h>
#include <cstring>
#include <EasyObfuse.h>

static bool gVerified = false;

static bool clearJniException(JNIEnv* env) {
    if (env != nullptr && env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
        return true;
    }
    return false;
}

static bool isJniBad(JNIEnv* env, const void* value) {
    bool hadException = clearJniException(env);
    return value == nullptr || hadException;
}

static jstring newUtf8String(JNIEnv* env, const char* value) {
    if (env == nullptr || value == nullptr) return nullptr;

    const jsize length = static_cast<jsize>(std::strlen(value));
    jbyteArray bytes = env->NewByteArray(length);
    if (isJniBad(env, bytes)) return nullptr;

    if (length > 0) {
        env->SetByteArrayRegion(
                bytes,
                0,
                length,
                reinterpret_cast<const jbyte*>(value)
        );
        if (clearJniException(env)) {
            env->DeleteLocalRef(bytes);
            return nullptr;
        }
    }

    jclass stringClass = env->FindClass(OBFUSCATE("java/lang/String"));
    if (isJniBad(env, stringClass)) {
        env->DeleteLocalRef(bytes);
        return nullptr;
    }

    jmethodID constructor = env->GetMethodID(
            stringClass,
            OBFUSCATE("<init>"),
            OBFUSCATE("([BLjava/lang/String;)V")
    );
    if (isJniBad(env, constructor)) {
        env->DeleteLocalRef(bytes);
        env->DeleteLocalRef(stringClass);
        return nullptr;
    }

    jstring charset = env->NewStringUTF(OBFUSCATE("UTF-8"));
    if (isJniBad(env, charset)) {
        env->DeleteLocalRef(bytes);
        env->DeleteLocalRef(stringClass);
        return nullptr;
    }

    jstring result = static_cast<jstring>(
            env->NewObject(stringClass, constructor, bytes, charset)
    );

    env->DeleteLocalRef(charset);
    env->DeleteLocalRef(bytes);
    env->DeleteLocalRef(stringClass);

    if (isJniBad(env, result)) return nullptr;
    return result;
}

static char hexNibble(unsigned int value) {
    return static_cast<char>(value < 10U ? ('0' + value) : ('a' + (value - 10U)));
}

static std::string f(JNIEnv* env, jbyteArray sigBytes) {
    if (env == nullptr || sigBytes == nullptr) return "";

    jclass mdClass = env->FindClass(OBFUSCATE("java/security/MessageDigest"));
    if (isJniBad(env, mdClass)) return "";

    jmethodID getInstance = env->GetStaticMethodID(mdClass, OBFUSCATE("getInstance"), OBFUSCATE("(Ljava/lang/String;)Ljava/security/MessageDigest;"));
    if (isJniBad(env, getInstance)) return "";

    jstring sha256Str = env->NewStringUTF(OBFUSCATE("SHA-256"));
    if (isJniBad(env, sha256Str)) return "";

    jobject md = env->CallStaticObjectMethod(mdClass, getInstance, sha256Str);
    env->DeleteLocalRef(sha256Str);
    if (isJniBad(env, md)) return "";

    jmethodID update = env->GetMethodID(mdClass, OBFUSCATE("update"), OBFUSCATE("([B)V"));
    if (isJniBad(env, update)) return "";

    env->CallVoidMethod(md, update, sigBytes);
    if (clearJniException(env)) return "";

    jmethodID digest = env->GetMethodID(mdClass, OBFUSCATE("digest"), OBFUSCATE("()[B"));
    if (isJniBad(env, digest)) return "";

    jbyteArray hashBytes = (jbyteArray) env->CallObjectMethod(md, digest);
    if (isJniBad(env, hashBytes)) return "";

    jsize hashLen = env->GetArrayLength(hashBytes);
    if (hashLen <= 0 || clearJniException(env)) return "";

    jbyte* hashPtr = env->GetByteArrayElements(hashBytes, nullptr);
    if (isJniBad(env, hashPtr)) return "";

    std::string hex;
    hex.reserve((size_t) hashLen * 2U);
    for (int i = 0; i < hashLen; i++) {
        unsigned char v = (unsigned char) hashPtr[i];
        hex.push_back(hexNibble(v >> 4));
        hex.push_back(hexNibble(v & 0x0f));
    }

    env->ReleaseByteArrayElements(hashBytes, hashPtr, JNI_ABORT);
    return hex;
}

static jobject g(JNIEnv* env, jstring pkgName, jint userId) {
    (void) env;
    (void) pkgName;
    (void) userId;
    return nullptr;
}


static jint getSdkInt(JNIEnv* env) {
    if (env == nullptr) return 0;

    jclass versionClass = env->FindClass(OBFUSCATE("android/os/Build$VERSION"));
    if (isJniBad(env, versionClass)) return 0;

    jfieldID sdkField = env->GetStaticFieldID(versionClass, OBFUSCATE("SDK_INT"), OBFUSCATE("I"));
    if (isJniBad(env, sdkField)) {
        env->DeleteLocalRef(versionClass);
        return 0;
    }

    jint sdk = env->GetStaticIntField(versionClass, sdkField);
    env->DeleteLocalRef(versionClass);
    if (clearJniException(env)) return 0;
    return sdk;
}

static const int kSignatureStateMatch = 1;
static const int kSignatureStateMismatch = 0;
static const int kSignatureStateUnreadable = -1;

static std::string signatureHashFromObject(JNIEnv* env, jobject sigObj) {
    if (env == nullptr || sigObj == nullptr) return "";

    jclass sigClass = env->GetObjectClass(sigObj);
    if (isJniBad(env, sigClass)) return "";

    jmethodID toByteArray = env->GetMethodID(sigClass, OBFUSCATE("toByteArray"), OBFUSCATE("()[B"));
    if (isJniBad(env, toByteArray)) {
        env->DeleteLocalRef(sigClass);
        return "";
    }

    jbyteArray byteArray = (jbyteArray) env->CallObjectMethod(sigObj, toByteArray);
    env->DeleteLocalRef(sigClass);
    if (isJniBad(env, byteArray)) return "";

    std::string hash = f(env, byteArray);
    env->DeleteLocalRef(byteArray);
    return hash;
}

static int signatureArrayState(
        JNIEnv* env,
        jobjectArray sigsArray,
        const char* const* allowedHashes,
        size_t allowedCount,
        bool requireAll
) {
    if (env == nullptr || sigsArray == nullptr || allowedHashes == nullptr || allowedCount == 0) {
        return kSignatureStateUnreadable;
    }

    jsize count = env->GetArrayLength(sigsArray);
    if (count <= 0 || clearJniException(env)) return kSignatureStateUnreadable;

    bool anyMatch = false;
    for (jsize idx = 0; idx < count; ++idx) {
        jobject sigObj = env->GetObjectArrayElement(sigsArray, idx);
        if (isJniBad(env, sigObj)) return kSignatureStateUnreadable;

        std::string hash = signatureHashFromObject(env, sigObj);
        env->DeleteLocalRef(sigObj);
        if (hash.empty()) return kSignatureStateUnreadable;

        bool matched = false;
        for (size_t i = 0; i < allowedCount; ++i) {
            if (hash == allowedHashes[i]) {
                matched = true;
                anyMatch = true;
                break;
            }
        }

        if (requireAll && !matched) return kSignatureStateMismatch;
        if (!requireAll && matched) return kSignatureStateMatch;
    }

    return requireAll ? kSignatureStateMatch : (anyMatch ? kSignatureStateMatch : kSignatureStateMismatch);
}

static int verifyPackageSignature(
        JNIEnv* env,
        jobject pkgInfo,
        const char* const* allowedHashes,
        size_t allowedCount
) {
    if (env == nullptr || pkgInfo == nullptr || allowedHashes == nullptr || allowedCount == 0) {
        return kSignatureStateUnreadable;
    }

    const jint sdk = getSdkInt(env);
    jobjectArray sigsArray = nullptr;
    bool requireAll = false;

    if (sdk >= 28) {
        jclass pkgInfoClass = env->GetObjectClass(pkgInfo);
        if (!isJniBad(env, pkgInfoClass)) {
            jfieldID signingInfoField = env->GetFieldID(
                    pkgInfoClass,
                    OBFUSCATE("signingInfo"),
                    OBFUSCATE("Landroid/content/pm/SigningInfo;")
            );
            if (!isJniBad(env, signingInfoField)) {
                jobject signingInfo = env->GetObjectField(pkgInfo, signingInfoField);
                if (!isJniBad(env, signingInfo)) {
                    jclass signingInfoClass = env->GetObjectClass(signingInfo);
                    if (!isJniBad(env, signingInfoClass)) {
                        jmethodID hasMultipleSigners = env->GetMethodID(
                                signingInfoClass,
                                OBFUSCATE("hasMultipleSigners"),
                                OBFUSCATE("()Z")
                        );
                        if (!isJniBad(env, hasMultipleSigners)) {
                            jboolean multiple = env->CallBooleanMethod(signingInfo, hasMultipleSigners);
                            if (!clearJniException(env)) {
                                const char* methodName = (multiple == JNI_TRUE)
                                        ? OBFUSCATE("getApkContentsSigners")
                                        : OBFUSCATE("getSigningCertificateHistory");

                                jmethodID getSigners = env->GetMethodID(
                                        signingInfoClass,
                                        methodName,
                                        OBFUSCATE("()[Landroid/content/pm/Signature;")
                                );
                                if (!isJniBad(env, getSigners)) {
                                    sigsArray = (jobjectArray) env->CallObjectMethod(signingInfo, getSigners);
                                    if (!clearJniException(env) && sigsArray != nullptr) {
                                        requireAll = (multiple == JNI_TRUE);
                                        int modernState = signatureArrayState(env, sigsArray, allowedHashes, allowedCount, requireAll);
                                        env->DeleteLocalRef(sigsArray);
                                        env->DeleteLocalRef(signingInfoClass);
                                        env->DeleteLocalRef(signingInfo);
                                        env->DeleteLocalRef(pkgInfoClass);
                                        if (modernState != kSignatureStateUnreadable) {
                                            return modernState;
                                        }
                                    } else {
                                        sigsArray = nullptr;
                                    }
                                }
                            }
                        }
                        env->DeleteLocalRef(signingInfoClass);
                    }
                    env->DeleteLocalRef(signingInfo);
                }
            }
            env->DeleteLocalRef(pkgInfoClass);
        }
    }

    if (sigsArray == nullptr) {
        jclass pkgInfoClass = env->GetObjectClass(pkgInfo);
        if (isJniBad(env, pkgInfoClass)) return kSignatureStateUnreadable;

        jfieldID sigsField = env->GetFieldID(
                pkgInfoClass,
                OBFUSCATE("signatures"),
                OBFUSCATE("[Landroid/content/pm/Signature;")
        );
        if (isJniBad(env, sigsField)) {
            env->DeleteLocalRef(pkgInfoClass);
            return kSignatureStateUnreadable;
        }

        sigsArray = (jobjectArray) env->GetObjectField(pkgInfo, sigsField);
        env->DeleteLocalRef(pkgInfoClass);
        if (clearJniException(env) || sigsArray == nullptr) return kSignatureStateUnreadable;

        const jsize count = env->GetArrayLength(sigsArray);
        if (clearJniException(env) || count <= 0) {
            env->DeleteLocalRef(sigsArray);
            return kSignatureStateUnreadable;
        }
        requireAll = (count > 1);
        int legacyState = signatureArrayState(env, sigsArray, allowedHashes, allowedCount, requireAll);
        env->DeleteLocalRef(sigsArray);
        return legacyState;
    }

    return kSignatureStateUnreadable;
}

static jobject h(JNIEnv* env, jobject context, jstring pkgName) {
    if (env == nullptr || context == nullptr || pkgName == nullptr) return nullptr;

    jclass ctxClass = env->GetObjectClass(context);
    if (isJniBad(env, ctxClass)) return nullptr;

    jmethodID getPm = env->GetMethodID(ctxClass, OBFUSCATE("getPackageManager"), OBFUSCATE("()Landroid/content/pm/PackageManager;"));
    if (isJniBad(env, getPm)) {
        env->DeleteLocalRef(ctxClass);
        return nullptr;
    }

    jobject pm = env->CallObjectMethod(context, getPm);
    env->DeleteLocalRef(ctxClass);
    if (isJniBad(env, pm)) return nullptr;

    jclass pmClass = env->GetObjectClass(pm);
    if (isJniBad(env, pmClass)) {
        env->DeleteLocalRef(pm);
        return nullptr;
    }

    jmethodID getPkgInfo = env->GetMethodID(pmClass, OBFUSCATE("getPackageInfo"), OBFUSCATE("(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;"));
    if (isJniBad(env, getPkgInfo)) {
        env->DeleteLocalRef(pmClass);
        env->DeleteLocalRef(pm);
        return nullptr;
    }

    jint flags = 0x00000040;
    if (getSdkInt(env) >= 28) {
        flags = 0x08000000;
    }

    jobject pkgInfo = env->CallObjectMethod(pm, getPkgInfo, pkgName, flags);
    env->DeleteLocalRef(pmClass);
    env->DeleteLocalRef(pm);
    if (clearJniException(env)) return nullptr;

    return pkgInfo;
}

static void j() {
    void* ptr = mmap(nullptr, 4096, PROT_READ|PROT_WRITE, MAP_PRIVATE|MAP_ANONYMOUS, -1, 0);
    if(ptr != MAP_FAILED) {
        memset(ptr, 0, 4096);
        munmap(ptr, 4096);
    }
    volatile int *p = nullptr;
    *p = 0;
    kill(getpid(), SIGKILL);
    _exit(0);
}

static void crashForIntegrityFailure(JNIEnv* env) {
    if (env == nullptr) {
        j();
        return;
    }

    // Give the system Toast service enough time to display the queued message.
    usleep(1400000);

    jclass exceptionClass = env->FindClass(OBFUSCATE("java/lang/SecurityException"));
    if (isJniBad(env, exceptionClass)) {
        j();
        return;
    }

    if (env->ThrowNew(
            exceptionClass,
            OBFUSCATE("Application integrity verification failed")
    ) != JNI_OK) {
        clearJniException(env);
        j();
    }
}


static std::string jstringToStdString(JNIEnv* env, jstring value) {
    if (env == nullptr || value == nullptr) return {};

    const char* chars = env->GetStringUTFChars(value, nullptr);
    if (chars == nullptr) {
        clearJniException(env);
        return {};
    }

    std::string out(chars);
    env->ReleaseStringUTFChars(value, chars);
    return out;
}

static std::string jsonEscape(const std::string& input) {
    std::string out;
    out.reserve(input.size() + 8);

    for (unsigned char c : input) {
        switch (c) {
            case '\\': out += "\\\\"; break;
            case '"': out += "\\\""; break;
            case '\b': out += "\\b"; break;
            case '\f': out += "\\f"; break;
            case '\n': out += "\\n"; break;
            case '\r': out += "\\r"; break;
            case '\t': out += "\\t"; break;
            default:
                if (c < 0x20) {
                    out.push_back(' ');
                } else {
                    out.push_back(static_cast<char>(c));
                }
                break;
        }
    }

    return out;
}


static void postLeechReport(JNIEnv* env, jobject context, const char* reason) {
    if (env == nullptr || context == nullptr || reason == nullptr) return;

    jclass ctxClass = env->GetObjectClass(context);
    if (isJniBad(env, ctxClass)) return;

    jmethodID getPkgName = env->GetMethodID(
            ctxClass,
            OBFUSCATE("getPackageName"),
            OBFUSCATE("()Ljava/lang/String;")
    );
    if (isJniBad(env, getPkgName)) {
        env->DeleteLocalRef(ctxClass);
        return;
    }

    jstring pkgName = (jstring) env->CallObjectMethod(context, getPkgName);
    env->DeleteLocalRef(ctxClass);
    if (isJniBad(env, pkgName)) return;

    std::string packageName = jstringToStdString(env, pkgName);
    env->DeleteLocalRef(pkgName);
    if (packageName.empty()) return;

    std::string payload = std::string("{\"packageName\":\"") +
            jsonEscape(packageName) +
            std::string("\",\"reason\":\"") +
            jsonEscape(reason) +
            std::string("\"}");

    jstring jUrl = newUtf8String(env, OBFUSCATE("https://warden-sooty.vercel.app/api/verify"));
    if (isJniBad(env, jUrl)) return;

    jclass urlClass = env->FindClass(OBFUSCATE("java/net/URL"));
    if (isJniBad(env, urlClass)) {
        env->DeleteLocalRef(jUrl);
        return;
    }

    jmethodID urlCtor = env->GetMethodID(urlClass, OBFUSCATE("<init>"), OBFUSCATE("(Ljava/lang/String;)V"));
    if (isJniBad(env, urlCtor)) {
        env->DeleteLocalRef(urlClass);
        env->DeleteLocalRef(jUrl);
        return;
    }

    jobject urlObj = env->NewObject(urlClass, urlCtor, jUrl);
    env->DeleteLocalRef(jUrl);
    if (isJniBad(env, urlObj)) {
        env->DeleteLocalRef(urlClass);
        return;
    }

    jmethodID openConnection = env->GetMethodID(urlClass, OBFUSCATE("openConnection"), OBFUSCATE("()Ljava/net/URLConnection;"));
    if (isJniBad(env, openConnection)) {
        env->DeleteLocalRef(urlObj);
        env->DeleteLocalRef(urlClass);
        return;
    }

    jobject connection = env->CallObjectMethod(urlObj, openConnection);
    env->DeleteLocalRef(urlObj);
    env->DeleteLocalRef(urlClass);
    if (isJniBad(env, connection)) return;

    jclass httpClass = env->FindClass(OBFUSCATE("java/net/HttpURLConnection"));
    if (isJniBad(env, httpClass)) {
        env->DeleteLocalRef(connection);
        return;
    }

    jmethodID setConnectTimeout = env->GetMethodID(httpClass, OBFUSCATE("setConnectTimeout"), OBFUSCATE("(I)V"));
    jmethodID setReadTimeout = env->GetMethodID(httpClass, OBFUSCATE("setReadTimeout"), OBFUSCATE("(I)V"));
    jmethodID setDoOutput = env->GetMethodID(httpClass, OBFUSCATE("setDoOutput"), OBFUSCATE("(Z)V"));
    jmethodID setUseCaches = env->GetMethodID(httpClass, OBFUSCATE("setUseCaches"), OBFUSCATE("(Z)V"));
    jmethodID setRequestMethod = env->GetMethodID(httpClass, OBFUSCATE("setRequestMethod"), OBFUSCATE("(Ljava/lang/String;)V"));
    jmethodID setRequestProperty = env->GetMethodID(httpClass, OBFUSCATE("setRequestProperty"), OBFUSCATE("(Ljava/lang/String;Ljava/lang/String;)V"));
    jmethodID getOutputStream = env->GetMethodID(httpClass, OBFUSCATE("getOutputStream"), OBFUSCATE("()Ljava/io/OutputStream;"));
    jmethodID getResponseCode = env->GetMethodID(httpClass, OBFUSCATE("getResponseCode"), OBFUSCATE("()I"));
    jmethodID disconnect = env->GetMethodID(httpClass, OBFUSCATE("disconnect"), OBFUSCATE("()V"));

    if (isJniBad(env, setConnectTimeout) || isJniBad(env, setReadTimeout) || isJniBad(env, setDoOutput) ||
        isJniBad(env, setUseCaches) || isJniBad(env, setRequestMethod) || isJniBad(env, setRequestProperty) ||
        isJniBad(env, getOutputStream) || isJniBad(env, getResponseCode) || isJniBad(env, disconnect)) {
        env->DeleteLocalRef(httpClass);
        env->DeleteLocalRef(connection);
        return;
    }

    env->CallVoidMethod(connection, setConnectTimeout, 3500);
    env->CallVoidMethod(connection, setReadTimeout, 3500);
    env->CallVoidMethod(connection, setDoOutput, JNI_TRUE);
    env->CallVoidMethod(connection, setUseCaches, JNI_FALSE);

    jstring method = env->NewStringUTF(OBFUSCATE("POST"));
    jstring headerName = env->NewStringUTF(OBFUSCATE("Content-Type"));
    jstring contentType = env->NewStringUTF(OBFUSCATE("application/json; charset=UTF-8"));
    if (!isJniBad(env, method) && !isJniBad(env, headerName) && !isJniBad(env, contentType)) {
        env->CallVoidMethod(connection, setRequestMethod, method);
        env->CallVoidMethod(connection, setRequestProperty, headerName, contentType);
    }

    if (clearJniException(env)) {
        if (!isJniBad(env, method)) env->DeleteLocalRef(method);
        if (!isJniBad(env, headerName)) env->DeleteLocalRef(headerName);
        if (!isJniBad(env, contentType)) env->DeleteLocalRef(contentType);
        env->DeleteLocalRef(httpClass);
        env->DeleteLocalRef(connection);
        return;
    }

    jclass osClass = env->FindClass(OBFUSCATE("java/io/OutputStream"));
    if (isJniBad(env, osClass)) {
        if (!isJniBad(env, method)) env->DeleteLocalRef(method);
        if (!isJniBad(env, headerName)) env->DeleteLocalRef(headerName);
        if (!isJniBad(env, contentType)) env->DeleteLocalRef(contentType);
        env->DeleteLocalRef(httpClass);
        env->DeleteLocalRef(connection);
        return;
    }

    jmethodID write = env->GetMethodID(osClass, OBFUSCATE("write"), OBFUSCATE("([B)V"));
    jmethodID flush = env->GetMethodID(osClass, OBFUSCATE("flush"), OBFUSCATE("()V"));
    jmethodID close = env->GetMethodID(osClass, OBFUSCATE("close"), OBFUSCATE("()V"));

    if (isJniBad(env, write) || isJniBad(env, flush) || isJniBad(env, close)) {
        env->DeleteLocalRef(osClass);
        if (!isJniBad(env, method)) env->DeleteLocalRef(method);
        if (!isJniBad(env, headerName)) env->DeleteLocalRef(headerName);
        if (!isJniBad(env, contentType)) env->DeleteLocalRef(contentType);
        env->DeleteLocalRef(httpClass);
        env->DeleteLocalRef(connection);
        return;
    }

    jbyteArray body = env->NewByteArray(static_cast<jsize>(payload.size()));
    if (isJniBad(env, body)) {
        env->DeleteLocalRef(osClass);
        if (!isJniBad(env, method)) env->DeleteLocalRef(method);
        if (!isJniBad(env, headerName)) env->DeleteLocalRef(headerName);
        if (!isJniBad(env, contentType)) env->DeleteLocalRef(contentType);
        env->DeleteLocalRef(httpClass);
        env->DeleteLocalRef(connection);
        return;
    }

    env->SetByteArrayRegion(body, 0, static_cast<jsize>(payload.size()), reinterpret_cast<const jbyte*>(payload.data()));
    if (clearJniException(env)) {
        env->DeleteLocalRef(body);
        env->DeleteLocalRef(osClass);
        if (!isJniBad(env, method)) env->DeleteLocalRef(method);
        if (!isJniBad(env, headerName)) env->DeleteLocalRef(headerName);
        if (!isJniBad(env, contentType)) env->DeleteLocalRef(contentType);
        env->DeleteLocalRef(httpClass);
        env->DeleteLocalRef(connection);
        return;
    }

    jobject out = env->CallObjectMethod(connection, getOutputStream);
    if (!isJniBad(env, out)) {
        env->CallVoidMethod(out, write, body);
        env->CallVoidMethod(out, flush);
        env->CallVoidMethod(out, close);
        clearJniException(env);
        env->DeleteLocalRef(out);
    } else {
        clearJniException(env);
    }

    env->DeleteLocalRef(body);
    env->DeleteLocalRef(osClass);

    env->CallIntMethod(connection, getResponseCode);
    clearJniException(env);

    env->CallVoidMethod(connection, disconnect);
    clearJniException(env);

    if (!isJniBad(env, method)) env->DeleteLocalRef(method);
    if (!isJniBad(env, headerName)) env->DeleteLocalRef(headerName);
    if (!isJniBad(env, contentType)) env->DeleteLocalRef(contentType);
    env->DeleteLocalRef(httpClass);
    env->DeleteLocalRef(connection);
}

static int m(JNIEnv* env, jobject context) {
    if (env == nullptr || context == nullptr) return kSignatureStateUnreadable;

    jclass ctxClass = env->GetObjectClass(context);
    if (isJniBad(env, ctxClass)) return kSignatureStateUnreadable;

    jmethodID getPkgName = env->GetMethodID(ctxClass, OBFUSCATE("getPackageName"), OBFUSCATE("()Ljava/lang/String;"));
    if (isJniBad(env, getPkgName)) {
        env->DeleteLocalRef(ctxClass);
        return kSignatureStateUnreadable;
    }

    jstring pkgName = (jstring) env->CallObjectMethod(context, getPkgName);
    env->DeleteLocalRef(ctxClass);
    if (isJniBad(env, pkgName)) return kSignatureStateUnreadable;

    jclass ctxClass2 = env->GetObjectClass(context);
    if (isJniBad(env, ctxClass2)) {
        env->DeleteLocalRef(pkgName);
        return kSignatureStateUnreadable;
    }

    jmethodID getSharedPrefs = env->GetMethodID(ctxClass2, OBFUSCATE("getSharedPreferences"), OBFUSCATE("(Ljava/lang/String;I)Landroid/content/SharedPreferences;"));
    env->DeleteLocalRef(ctxClass2);
    if (isJniBad(env, getSharedPrefs)) {
        env->DeleteLocalRef(pkgName);
        return kSignatureStateUnreadable;
    }

    jstring prefsName = env->NewStringUTF(OBFUSCATE("x9j3kf"));
    if (isJniBad(env, prefsName)) {
        env->DeleteLocalRef(pkgName);
        return kSignatureStateUnreadable;
    }

    jobject prefs = env->CallObjectMethod(context, getSharedPrefs, prefsName, 0);
    env->DeleteLocalRef(prefsName);
    if (isJniBad(env, prefs)) {
        env->DeleteLocalRef(pkgName);
        return kSignatureStateUnreadable;
    }

    jclass spClass = env->GetObjectClass(prefs);
    if (isJniBad(env, spClass)) {
        env->DeleteLocalRef(pkgName);
        return kSignatureStateUnreadable;
    }

    jmethodID getBool = env->GetMethodID(spClass, OBFUSCATE("getBoolean"), OBFUSCATE("(Ljava/lang/String;Z)Z"));
    if (isJniBad(env, getBool)) {
        env->DeleteLocalRef(pkgName);
        return kSignatureStateUnreadable;
    }

    jstring keyLeech = env->NewStringUTF(OBFUSCATE("ld"));
    if (isJniBad(env, keyLeech)) {
        env->DeleteLocalRef(pkgName);
        return kSignatureStateUnreadable;
    }

    /*
     * "ld" is a historical marker from an earlier failed check. It must
     * never be trusted as the current integrity result: an old false
     * positive would otherwise permanently kill a corrected installation.
     * The current package signature is always authoritative below.
     */
    (void) env->CallBooleanMethod(prefs, getBool, keyLeech, JNI_FALSE);
    env->DeleteLocalRef(keyLeech);
    if (clearJniException(env)) {
        env->DeleteLocalRef(pkgName);
        return kSignatureStateUnreadable;
    }

    jobject pkgInfoPM = h(env, context, pkgName);
    env->DeleteLocalRef(pkgName);
    if (pkgInfoPM == nullptr) {
        return kSignatureStateUnreadable;
    }

    const char* allowedHashes[] = {
        OBFUSCATE("e4201e2e32724c1ba1ef1100d35ff9f75c5d3e888a58c68b7747808f4c87607b")
    };

    int sigState = verifyPackageSignature(env, pkgInfoPM, allowedHashes, 1);
    env->DeleteLocalRef(pkgInfoPM);
    if (clearJniException(env)) return kSignatureStateUnreadable;

    if (sigState == kSignatureStateMatch) {
        // A previously stored failure marker must not survive a valid match.
        jclass spEditorClass = env->FindClass(OBFUSCATE("android/content/SharedPreferences$Editor"));
        if (!isJniBad(env, spEditorClass)) {
            jmethodID edit = env->GetMethodID(spClass, OBFUSCATE("edit"), OBFUSCATE("()Landroid/content/SharedPreferences$Editor;"));
            if (!isJniBad(env, edit)) {
                jobject editor = env->CallObjectMethod(prefs, edit);
                if (!isJniBad(env, editor)) {
                    jmethodID remove = env->GetMethodID(spEditorClass, OBFUSCATE("remove"), OBFUSCATE("(Ljava/lang/String;)Landroid/content/SharedPreferences$Editor;"));
                    if (!isJniBad(env, remove)) {
                        jstring key2 = env->NewStringUTF(OBFUSCATE("ld"));
                        if (!isJniBad(env, key2)) {
                            env->CallObjectMethod(editor, remove, key2);
                            env->DeleteLocalRef(key2);
                            clearJniException(env);
                        }
                    }
                    jmethodID apply = env->GetMethodID(spEditorClass, OBFUSCATE("apply"), OBFUSCATE("()V"));
                    if (!isJniBad(env, apply)) {
                        env->CallVoidMethod(editor, apply);
                        clearJniException(env);
                    }
                    env->DeleteLocalRef(editor);
                }
            }
            env->DeleteLocalRef(spEditorClass);
        } else {
            clearJniException(env);
        }
        return kSignatureStateMatch;
    }

    if (sigState == kSignatureStateUnreadable) {
        return kSignatureStateUnreadable;
    }

    postLeechReport(env, context, OBFUSCATE("SIGNATURE_MISMATCH"));

    jclass spEditorClass = env->FindClass(OBFUSCATE("android/content/SharedPreferences$Editor"));
    if (!isJniBad(env, spEditorClass)) {
        jmethodID edit = env->GetMethodID(spClass, OBFUSCATE("edit"), OBFUSCATE("()Landroid/content/SharedPreferences$Editor;"));
        if (!isJniBad(env, edit)) {
            jobject editor = env->CallObjectMethod(prefs, edit);
            if (!isJniBad(env, editor)) {
                jmethodID putBool = env->GetMethodID(spEditorClass, OBFUSCATE("putBoolean"), OBFUSCATE("(Ljava/lang/String;Z)Landroid/content/SharedPreferences$Editor;"));
                if (!isJniBad(env, putBool)) {
                    jstring key2 = env->NewStringUTF(OBFUSCATE("ld"));
                    if (!isJniBad(env, key2)) {
                        env->CallObjectMethod(editor, putBool, key2, JNI_TRUE);
                        env->DeleteLocalRef(key2);
                        clearJniException(env);
                    }
                }

                jmethodID apply = env->GetMethodID(spEditorClass, OBFUSCATE("apply"), OBFUSCATE("()V"));
                if (!isJniBad(env, apply)) {
                    env->CallVoidMethod(editor, apply);
                    clearJniException(env);
                }
            }
        }
    } else {
        clearJniException(env);
    }

    crashForIntegrityFailure(env);
    return kSignatureStateMismatch;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_ridhoae303_expert_Takane_b(JNIEnv* env, jclass, jobject context) {
    if (env == nullptr || context == nullptr) return JNI_FALSE;

    // The Context is already supplied directly to the native verifier.
    // Do not depend on an additional Java helper method here: requiring a
    // removable/rename-sensitive helper can turn a valid build into a false
    // integrity failure after ProGuard or source cleanup.
    if (gVerified) return JNI_TRUE;

    const int sigState = m(env, context);
    if (sigState == kSignatureStateMatch) {
        gVerified = true;
        return JNI_TRUE;
    }
    if (sigState == kSignatureStateUnreadable) {
        return JNI_TRUE;
    }

    return JNI_FALSE;
}

