# Veracross Assignments - Project Goal and Status

## Project Goal

Build an Android app that scrapes and displays Veracross school assignments, with in-app Google SSO login (no external server, no leaving the app, no cookie paste). The app should extract session cookies after Google login to enable background assignment checks with push notifications.

## The Core Problem

Google blocks OAuth in Android WebView through multi-layer detection:
1. User-Agent: Contains wv and Chrome/74 - Google server-side rejects v74 explicitly
2. X-Requested-With header: Hardcoded to app package name in native Chromium C++ - cannot be stripped
3. TLS fingerprint (JA3): v74 HTTPS handshake uses 2019-era cipher suites - Google cross-references against claimed UA
4. Botguard: Client-side JS checks navigator.webdriver, missing window.chrome, 0 plugins, V8 timing - all fail on WebView

No UA spoofing, proxy, or header manipulation bypasses all four layers.

## Device Constraints

- WebView provider: com.android.webview version 74 (2019)
- Chrome installed: v157 (com.android.chrome)
- Alternative WebView: com.google.android.webview v154
- JDK available: 8, 17, 21, 25
- Gradle: 8.11.1, 9.2.0, 9.3.1, 9.4.1, 9.7.1

## Projects

### 1. Python Scraper (veracross-assignments/)
- Status: Working
- scrape.py captures portal data via Playwright
- Successfully extracted 30 assignments (7 due in next 5 days)

### 2. Android App (veracross-android/)
- Status: Working (with backend login)
- Full app with WorkManager background checks
- Notification system: non-swipable for tests, swipable for homework, new-item alerts
- Auth via deep link veracross://auth?cookie=...
- APK built and installed on device

### 3. Backend (veracross-backend/)
- Status: Deployed on Render
- Express + Puppeteer server streaming Chrome browser via screenshot polling (10fps)
- URL: https://veracross-backend.onrender.com
- GitHub: https://github.com/jihvijhojhviihogyuvi/veracross-backend

## Approaches Tried

### A. WebView with UA spoofing - FAILED
- Replaced userAgentString with Chrome/120 mobile UA
- Injected JS to patch navigator.webdriver, navigator.plugins, window.chrome
- Failed: Google still blocks (TLS fingerprint + X-Requested-With remain)

### B. WebView with header-stripping proxy - FAILED
- Intercepted requests via shouldInterceptRequest
- Failed: POST bodies cannot be forwarded, TLS fingerprint unchanged, Botguard detects WebView

### C. GeckoView (Mozilla Firefox engine) - FAILED
- Google SSO works (real browser)
- Build succeeded after upgrading to AGP 9.2.0, Gradle 9.4.1, JDK 17, compileSdk 37
- No cookie extraction API - no CookieManager, no getCookie(), no evaluateJavaScript()
- No way to get session cookie after login
- Kotlin 2.4.0 metadata incompatibility
- AAR minCompileSdk=37.2 (only 37.0 available)
- Verdict: Cannot extract cookies, not viable

### D. Chrome Custom Tabs (CCT) - FAILED
- Google SSO works (uses system Chrome)
- Cannot extract cookies - no API for this
- CCT and WebView do not share cookies unless Chrome is the WebView provider

### E. Changing WebView provider to Chrome - WORKS (one-time setting)
- If Chrome becomes the WebView provider: WebView uses Chrome engine, cookies shared
- Requires: Settings -> Developer options -> WebView implementation -> Chrome -> reboot
- Works but requires leaving the app for ~10 seconds (one-time)

### F. Backend (remote Chrome streaming) - WORKING (currently deployed)
- Google SSO works (real Chrome on server)
- Cookie extraction works (server reads Chrome cookies)
- Deep link returns session to app
- Requires external server (deployed on Render)
- BackendBrowserActivity keeps user in-app while using remote Chrome

### G. WebViewUpgrade library (JonaNorman) - ATTEMPTED
- Allows upgrading WebView kernel to use Chrome without installing WebView APK
- Maven: io.github.jonanorman.android.webviewup:core:0.1.0
- Failed: Import path was wrong (com.norman.webviewup.lib.source.UpgradePackageSource)
- After fixing imports, build failed due to other issues
- Status: Not working

### H. Cefrium (Chromium Embedded Framework for Android) - IN PROGRESS
- Real Chromium engine bundled in app (com.cefrium:cefrium-sdk:0.17.0)
- Has getCookies(), setCookie(), evaluateJavaScript(), deleteCookies(), flushCookies()
- Free and open source (LGPL-3.0-or-later)
- Published on Maven Central
- Requires JDK 25, Gradle 9.7.1, AGP 9.4, compileSdk 37, minSdk 29
- Large footprint (~248MB AAR)
- CefriumBrowser has createWithSurface() returning FrameLayout
- getCookies(url, CookieCallback) returns List<Cookie> with name, value, domain, path, secure, httpOnly
- Status: Attempting to integrate

## The one path without external server

Change the WebView provider to Chrome (one-time, 10 seconds):
1. Settings -> Developer options -> WebView implementation -> Chrome
2. Reboot
3. App existing WebView code works (Chrome engine, shared cookies)

## What works now

The app uses BackendBrowserActivity which opens a WebView loading the backend URL.
The remote Chrome streams in the WebView. User logs in with Google. Backend captures
cookie and sends deep link. App extracts cookie and proceeds to assignments.

User never leaves the app, but an external server is required.

## Next steps

1. Try Cefrium integration - real Chromium engine with cookie extraction API
   - Need JDK 25 / Gradle 9.7.1 / AGP 9.4 build setup
   - Need to rewrite LoginActivity to use CefriumBrowser
   - Need to extract cookies via getCookies() callback
   - Need to handle large APK size (~248MB)
2. If Cefrium works: Remove backend dependency, make it fully in-app
3. If Cefrium fails: Fall back to BackendBrowserActivity (current working solution)

## Files

- Python scraper: veracross-assignments/
- Android app: veracross-android/
- Backend: veracross-backend/
- GitHub: https://github.com/jihvijhojhviihogyuvi/veracross-backend
- Render: https://veracross-backend.onrender.com
- Issue: veracross-android/issue.md

## Help wanted

Looking for any solution that allows:
1. Google SSO inside the Android app (no external server)
2. Cookie extraction after login (for OkHttp background checks)
3. No system setting changes by the user

Possible directions:
- Cefrium integration (Chromium engine with cookie API) - in progress
- GeckoView with cookie extraction workaround (WebExtension, GeckoWebExecutor)
- Chrome Custom Tabs with cookie sharing
- Chromium embedding (if feasible at smaller size)
- Any Android library that bundles a real browser with cookie API
- Workaround for Google WebView OAuth block on newer Android versions
