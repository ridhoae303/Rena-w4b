<p align="center">
  <img width="100%" alt="Rena W4B banner" src="./banner/banner.png" />
</p>

<h1 align="center">Rena W4B</h1>

<p align="center">
  A lightweight WhatsApp Web wrapper for Android.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Android-API%2026%2B-3DDC84?logo=android&logoColor=white" alt="Android" />
  <img src="https://img.shields.io/badge/Java-7%20compatible-orange?logo=java&logoColor=white" alt="Java" />
  <img src="https://img.shields.io/badge/C%2B%2B-Native-00599C?logo=c%2B%2B&logoColor=white" alt="C++" />
  <img src="https://img.shields.io/badge/License-RenaW4B-lightgrey" alt="License" />
</p>

## Preview

<table>
  <tr>
    <td width="50%" align="center">
      <img width="100%" alt="Rena W4B preview 2" src="https://github.com/user-attachments/assets/5925d1e4-3602-4b25-ab34-6d4409e36b42" />
    </td>
    <td width="50%" align="center">
      <img width="100%" alt="Rena W4B preview" src="https://github.com/user-attachments/assets/255585dc-f103-4878-8027-b44008a293c4" />
    </td>
  </tr>
</table>

## About

Rena W4B puts **WhatsApp Web** into its own Android WebView, so you do not have
to keep opening Chrome just to get back to your session.

The project keeps the UI lightweight while the native C++ layer handles the
protected configuration/string side.

## Features

- Desktop-style WhatsApp Web inside WebView
- Persistent WebView session handling
- Manual **Refresh** without intentionally wiping the session
- **Zoom/Out** checkbox
- Responsive scrollable navigation
- Camera, microphone, notification and file access support
- Fullscreen / immersive UI
- Hardware-accelerated WebView rendering
- GitHub, Telegram, community and donation links
- Rena easter egg with zoomable image preview
- Native C++ configuration through `librena.so`
- Basic package/signature/debugger integrity checks

## Session

Rena W4B does not copy WhatsApp cookies into Chrome or another browser.

Normal Activity pause/stop does not intentionally clear cookies, history,
cache, or force a fresh QR login. A QR scan can still be required when
WhatsApp invalidates the session, app data is cleared, or the WebView storage
is reset.

## Native

```text
Package:   com.rena.w4b
Library:   librena.so
Native:    C++
Obfuscation: EasyObfuse
```

Native strings use `OBFUSCATE(...)`, while Java handles the UI and app flow.

## Resources

```text
app/src/main/res/drawable-nodpi/
├── github.png
├── tg.png
└── wa.png

app/src/main/assets/
└── Rena.jpg

banner/
└── banner.png
```

## Build

- Minimum Android: **API 26**
- Target Android: **API 37**
- ABIs: `armeabi-v7a`, `arm64-v8a`, `x86`, `x86_64`
- Native build: CMake + NDK make files
- Release: ProGuard / minification enabled

A working `EasyObfuse.h` setup is required for the native string layer.

## Disclaimer

Rena W4B is an independent wrapper project and is **not an official WhatsApp
client**. Third-party software, trademarks and services remain subject to
their own licenses and terms.

##Credits##
Made with ❤️ by **ridhoae303**

<p>
<a href="https://github.com/ridhoae303"><img src="https://img.shields.io/badge/Github-ridhoae303-181717?logo=github&logoColor=white" alt="GitHub"></a>
<a href="https://t.me/ridhoae303"><img src="https://img.shields.io/badge/Telegram-ridhoae303-26A5E4?logo=telegram&logoColor=white" alt="Telegram"></a>
<a href="https://chat.whatsapp.com/DCA3op1pxcDbr5VvqIFvE6"><img src="https://img.shields.io/badge/WhatsApp-0b0b0b?logo=WhatsApp&logoColor=white" alt="WhatsApp"></a>
<a href="https://sociabuzz.com/ridhoae303"><img src="https://img.shields.io/badge/Donate-SociaBuzz-FF4D8D?logo=buymeacoffee&logoColor=white" alt="Donate"></a>
</p>

---

**Collaborator**  
**SuniDreami**

<p>
<a href="https://github.com/SunDream"><img src="https://img.shields.io/badge/Github-SuniDreami-181717?logo=github&logoColor=white" alt="GitHub"></a>
<a href="https://wa.me/qr/6JZKBXL7GDUYM1"><img src="https://img.shields.io/badge/WhatsApp-SuniDreami-25D366?logo=whatsapp&logoColor=white" alt="WhatsApp"></a>
</p>
