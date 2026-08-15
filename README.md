# Rena W4B

> A lightweight WhatsApp Web wrapper for Android, made with AIDE in mind.

Rena W4B puts **WhatsApp Web** inside its own Android WebView, so you do not
have to keep opening Chrome just to get back to your WhatsApp session.

The idea is pretty simple: keep the UI clean, keep the WebView session intact,
and put the configuration/native string layer on the C++ side.

---

## Project Info

- **Project:** `Rena-w4b`
- **App name:** `Rena W4B`
- **Package:** `com.rena.w4b`
- **Native library:** `librena.so`
- **Language:** Java + C++
- **Java style:** Java 7 compatible, no lambdas
- **Minimum Android:** API 26
- **Target Android:** API 37
- **ABIs:** `armeabi-v7a`, `arm64-v8a`, `x86`, `x86_64`

---

## What it does

Rena W4B opens:

`https://web.whatsapp.com`

using a desktop-style User-Agent so WhatsApp Web behaves more like a desktop
browser inside the app.

The app comes with a small navigation drawer containing:

- App icon + app name
- Zoom/Out
- Refresh
- GitHub
- Telegram
- Modding Community
- Donate us
- `Made with ❤️ by ridhoae303`
- A hidden Rena easter egg

The drawer is responsive and scrollable, so it is not tied to one phone size,
one orientation, or one DPI setup.

---

## WhatsApp Web Session

Rena W4B does **not** copy WhatsApp cookies into Chrome or another browser.

The WebView keeps its own cookies/session storage. Normal Activity
pause/stop does not intentionally wipe that session, clear the WebView cache,
clear its history, or force a new QR login.

There is also a manual Refresh option. It reloads the current WebView without
first deleting the current session.

### One honest limitation

No WebView wrapper can guarantee that WhatsApp will never ask for a QR scan
again.

A new login can still happen when:

- WhatsApp invalidates the session
- the user logs out
- Android clears app data
- the WebView provider/storage is reset
- the app is uninstalled/reinstalled
- the account/session is changed elsewhere

So the goal here is simply:

**Don't make the user scan the QR code again for no reason.**

---

## Permissions

Rena W4B can request the Android permissions needed for WebView features,
including:

- Camera
- Microphone
- Notifications on supported Android versions
- Legacy external storage permissions on older Android versions

File uploads use Android's system file picker rather than requesting broad
"all files" access.

Permissions are introduced one at a time with an in-app explanation before
Android's own permission prompt appears.

Rejecting a permission does not force the user out of the app. The permission
can be enabled later through Android settings.

---

## WebView Features

The WebView is configured for the features WhatsApp Web can reasonably need:

- JavaScript
- DOM storage
- Database storage
- Cookies
- Third-party cookies where supported
- Desktop User-Agent
- Camera capture
- Microphone capture
- File chooser / uploads
- Downloads
- Hardware acceleration

The app uses Android's WebView/Chromium stack instead of adding another
browser engine on top of it.

---

## Zoom / Out

The navigation includes a `Zoom/Out` switch.

When enabled, WebView zooming is allowed.

When disabled again, the app steps the page back toward the captured normal
baseline instead of simply leaving the page stuck at the last zoom level.

---

## Navigation

The drawer is built with a scrollable layout so it keeps working in:

- portrait
- landscape
- phones
- tablets
- short-height windows
- larger Android/ChromeOS-style screens

Buttons also use touch-slop handling, so dragging across a button does not
immediately become a click.

The menu trigger is kept away from the WhatsApp Web header so it does not sit
on top of important page controls.

---

## Donate

The navigation includes a small:

**Donate us**

button.

Donation page:

`https://sociabuzz.com/ridhoae303`

The button has a lightweight shimmer/highlight animation, while the label and
URL come from the native configuration layer.

---

## Rena Easter Egg

There is a little hidden one.

Tap the app icon in the navigation menu **three times** inside the configured
tap window.

The icon shakes on every hit.

On the third hit, Rena W4B opens:

```text
assets/Rena.jpg
```

The preview supports:

- pinch zoom
- zoom out
- panning
- fade in/out
- back button to close
- tapping outside the actual rendered image to close

The app checks the image's real Matrix-mapped bounds for touch handling, so the
ImageView itself does not accidentally swallow taps that happen outside the
actual picture.

The image is decoded away from the main UI thread and sampled when necessary
so a huge source image does not immediately try to consume all available RAM.

---

## Fullscreen

Rena W4B is designed to run fullscreen.

Fullscreen is applied at both the theme/window level and through Android's
immersive system UI handling. It is also re-applied when the Activity regains
focus after temporary system UI such as permission dialogs or file pickers.

The idea is simple: no random status-bar gap hanging above the app.

---

## Native Layer

Java handles the UI and app flow.

The native library supplies configuration strings, URLs, the desktop
User-Agent, permission text, and other protected values through JNI.

Native library:

`librena.so`

The native code uses:

```cpp
#include <EasyObfuse.h>
```

and wraps string literals with:

```cpp
OBFUSCATE(...)
```

This is meant to make casual string extraction and basic repackaging more
annoying. It is not magic DRM, and a determined reverse engineer with control
over the device/runtime can still work around client-side checks.

---

## Integrity Checks

The app includes conservative integrity checks around things such as:

- package identity
- expected `Application` class
- signing certificate SHA-256
- debugger/tracer-related signals

The currently configured certificate SHA-256 is:

```text
e4201e2e32724c1ba1ef1100d35ff9f75c5d3e888a58c68b7747808f4c87607b
```

That hash needs to match the certificate actually used to sign the APK.

The anti-tamper side is intentionally conservative. The point is to catch
common tampering/repackaging without turning normal devices into a crash
lottery.

---

## GPU / OpenGL ES

Hardware acceleration is enabled for the WebView.

The project declares GLES 3.0 support without making it a hard installation
requirement, so devices that only expose GLES 2.0 are not rejected.

For the WebView itself, Android/Chromium owns the actual EGL/GLES context.
Rena W4B does not inject a custom OpenGL renderer in front of WebView because
that would add another rendering layer and could make the app heavier.

---

## Resources

Navigation icons:

```text
app/src/main/res/drawable-nodpi/
├── github.png
├── tg.png
└── wa.png
```

The `drawable-nodpi` folder is used so Android does not automatically apply
density scaling that can make small icons look softer on different DPI
devices.

Rena preview:

```text
app/src/main/assets/Rena.jpg
```

The image can be high resolution. The app fits/decodes it for the current
device instead of treating its raw pixel dimensions as UI dimensions.

---

## Native Build

The native side is under:

```text
app/src/main/jni/
├── Android.mk
├── Application.mk
├── CMakeLists.txt
├── EasyObfuse.h
└── native-lib.cpp
```

The generated native library name is:

```text
librena.so
```

---

## Release Build

Release builds use:

- shrinking
- minification
- ProGuard rules
- native C++ code

The ProGuard configuration is intentionally conservative because old AIDE
toolchains can produce compiler-generated inner-class/synthetic-accessor
layouts that modern tooling does not always love.

Do not blindly throw `-ignorewarnings` at a broken bytecode pipeline just to
make the build turn green. A warning that points to inconsistent program
classes can be a real problem.

---

## Project Layout

```text
Rena-w4b/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── assets/
│   │       │   └── Rena.jpg
│   │       ├── java/
│   │       │   └── com/rena/w4b/
│   │       │       ├── MainActivity.java
│   │       │       └── NativeConfig.java
│   │       ├── jni/
│   │       │   ├── Android.mk
│   │       │   ├── Application.mk
│   │       │   ├── CMakeLists.txt
│   │       │   ├── EasyObfuse.h
│   │       │   └── native-lib.cpp
│   │       └── res/
│   │           ├── drawable-nodpi/
│   │           │   ├── github.png
│   │           │   ├── tg.png
│   │           │   └── wa.png
│   │           ├── layout/
│   │           └── values/
│   └── proguard-rules.pro
├── build.gradle
├── settings.gradle
├── gradle.properties
├── LICENSE
└── README.md
```

---


## Preview

<p align="center">
  <img width="360" height="800" alt="preview2" src="https://github.com/user-attachments/assets/5925d1e4-3602-4b25-ab34-6d4409e36b42" />
  <img width="360" height="800" alt="preview" src="https://github.com/user-attachments/assets/255585dc-f103-4878-8027-b44008a293c4" />
</p>

## Third-Party Stuff

Rena W4B interacts with third-party software, libraries, platforms and online
services.

That includes, among other things:

- Android
- Android WebView / Chromium
- WhatsApp Web
- OpenGL ES / EGL infrastructure provided by Android
- EasyObfuse, as supplied/configured by the developer

Their own licenses, trademarks, rules and terms still apply.

Rena W4B is an independent wrapper project and is **not an official WhatsApp
client**.

---

## Credits

Made with ❤️ by **ridhoae303**

[![GitHub](https://img.shields.io/badge/GitHub-ridhoae303-181717?logo=github&logoColor=white)](https://github.com/ridhoae303)
[![Telegram](https://img.shields.io/badge/Telegram-ridhoae303-26A5E4?logo=telegram&logoColor=white)](https://t.me/ridhoae303)
[![Modding Community](https://img.shields.io/badge/WhatsApp-Modding%20Community-25D366?logo=whatsapp&logoColor=white)](https://chat.whatsapp.com/DcA3oplpxcbDr5vVqIfvE6)
[![Donate](https://img.shields.io/badge/Donate-SociaBuzz-FF4D8D?logo=buymeacoffee&logoColor=white)](https://sociabuzz.com/ridhoae303)


---

## Final Words

This project started from a pretty simple thought:

**"I just want WhatsApp Web in its own app without having to babysit Chrome."**

So yeah. That's Rena W4B.
