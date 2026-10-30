# Issue: In-App Google SSO for Veracross Assignments App

## Summary

The Android app needs to authenticate with Veracross via Google SSO entirely within the app.
Google's security policy blocks OAuth in Android WebView, making in-app Google login
fundamentally impossible without a real browser engine or external backend.

## Device

- **Device**: Android phone (connected via ADB at 192.168.1.99)
- **WebView provider**: `com.android.webview` version 74 (2019)
- **Chrome installed**: v157 (`com.android.chrome`)
- **Alternative WebView**: `com.google.android.webview` v154

## Root Cause

Google blocks OAuth in WebView through **4 independent server-side and client-side checks**:

1. **User-Agent string** — Contains `wv` and `Chrome/74`. Google's servers explicitly reject v74.
2. **X-Requested-With header** — Hardcoded to `com.veracross.assignments` in native Chromium C++
   (`aw_content_browser_client.cc`). Set after Java/Kotlin code runs. Cannot be stripped.
3. **TLS fingerprint (JA3)** — v74's HTTPS handshake uses 2019-era cipher suites. Google
   cross-references this against the claimed UA. Mismatch = block.
4. **Botguard** — Client-side JS checks `navigator.webdriver`, missing `window.chrome` object,
   0 plugins, V8 timing. All fail on WebView.

No combination of UA spoofing, proxy, or header manipulation bypasses all four layers.

## Approaches Tried

### 1. WebView with UA spoofing ❌
- Replaced `userAgentString` with Chrome/120 mobile UA
- Injected JS to patch `navigator.webdriver`, `navigator.plugins`, `window.chrome`
- **Result**: Google still blocks (TLS fingerprint + X-Requested-With remain)

### 2. WebView with header-stripping proxy ❌
- Intercepted requests via `shouldInterceptRequest`
- Attempted to strip `X-Requested-With` header
- **Result**: POST bodies can't be forwarded (no `getRequestBody()`), TLS fingerprint unchanged,
  Botguard still detects WebView environment

### 3. GeckoView (Mozilla Firefox engine) — Partially works, fundamental gaps
- ✅ Google SSO works (real browser, Google doesn't block Firefox)
- ✅ Built successfully after upgrading to AGP 9.2.0, Gradle 9.4.1, JDK 17, compileSdk 37
- ❌ **No cookie extraction API** — no `CookieManager`, no `getCookie()`, no `evaluateJavaScript()`
- ❌ **No way to get session cookie** after login → can't do background checks
- ❌ Kotlin 2.4.0 metadata incompatibility (GeckoView pulls kotlin-stdlib 2.4.20)
- ❌ AAR `minCompileSdk=37.2` (only 37.0 available) — required AAR modification
- **Verdict**: Can't extract cookies → can't do background checks → not viable

### 4. Chrome Custom Tabs (CCT) ❌
- ✅ Google SSO works (uses system Chrome)
- ❌ **Can't extract cookies** — no API for this
- ❌ CCT and WebView don't share cookies unless Chrome is the WebView provider

### 5. Changing WebView provider to Chrome ⚠️ (one-time system setting)
- If Chrome becomes the WebView provider: WebView uses Chrome's engine, cookies shared
- Requires: Settings → Developer options → WebView implementation → Chrome → reboot
- **Works but requires leaving the app for ~10 seconds (one-time)**

### 6. Backend (remote Chrome streaming) ✅ (currently deployed)
- ✅ Google SSO works (real Chrome on server)
- ✅ Cookie extraction works (server reads Chrome's cookies)
- ✅ Deep link `veracross://auth?cookie=...` returns session to app
- ❌ Requires external server (deployed on Render: https://veracross-backend.onrender.com)
- `BackendBrowserActivity` keeps user in-app while using remote Chrome

## What works now

The app uses `BackendBrowserActivity` which opens a WebView loading the backend URL.
The remote Chrome streams in the WebView. User logs in with Google. Backend captures
cookie and sends deep link. App extracts cookie and proceeds to assignments.

User never leaves the app, but an external server is required.

## The one path without external server

Change the device's WebView provider to Chrome (one-time, 10 seconds):
1. Settings → Developer options → WebView implementation → Chrome
2. Reboot
3. App's existing WebView code works (Chrome engine, shared cookies, no extraction needed)

## Files

- Python scraper: `C:\Users\Charlie\.cline\data\workspaces\chat\veracross-assignments\`
- Android app: `C:\Users\Charlie\.cline\data\workspaces\chat\veracross-android\`
- Backend: `C:\Users\Charlie\.cline\data\workspaces\chat\veracross-backend\`
- GitHub: https://github.com/jihvijhojhviihogyuvi/veracross-backend
- Render: https://veracross-backend.onrender.com

## Help wanted

Looking for any solution that allows:
1. Google SSO inside the Android app (no external server)
2. Cookie extraction after login (for OkHttp background checks)
3. No system setting changes by the user

Possible directions:
- GeckoView with cookie extraction workaround (WebExtension, GeckoWebExecutor)
- Chrome Custom Tabs with cookie sharing
- Chromium embedding (if feasible at smaller size)
- Any Android library that bundles a real browser with cookie API
- Workaround for Google's WebView OAuth block on newer Android versions

## Update: WebViewUpgrade library — potential solution

Found **WebViewUpgrade** (by JonaNorman): https://github.com/JonaNorman/WebViewUpgrade

This library allows upgrading the WebView kernel in your app WITHOUT installing a WebView APK.
It can use an **already-installed package** (like Chrome) as the WebView provider.

### How it works
- `UpgradePackageSource("com.android.chrome")` — uses installed Chrome as the WebView kernel
- Only affects the host app, not other apps
- Must be called before any WebView is created (cold start)
- After upgrade, WebView uses Chrome's engine → Google SSO works, cookies shared

### Maven dependency
```
implementation 'io.github.jonanorman.android.webviewup:core:0.1.0'
```

### Usage
```kotlin
val source = UpgradePackageSource("com.android.chrome")
WebViewUpgrade.upgrade(source)
// After upgrade, WebView uses Chrome's engine
```

### This could solve everything
If Chrome becomes the WebView provider in the app:
1. WebView uses Chrome's engine → Google OAuth not blocked
2. WebView and Chrome share cookie jar → CookieManager.getCookie() works
3. No cookie extraction needed
4. No external server needed
5. Background checks work with existing OkHttp + cookie code

### Next steps
1. Add the dependency to build.gradle.kts
2. Call WebViewUpgrade.upgrade() before any WebView is created (in Application.onCreate)
3. Revert all GeckoView changes
4. Test Google SSO in WebView
5. If it works, remove BackendBrowserActivity (no longer needed)

### Caveats
- Chrome must be installed (it is — v157)
- Upgrade must happen before first WebView creation (cold start)
- May not work on all devices/ROMs
- Need to test if Google SSO actually works after upgrade