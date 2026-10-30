# Veracross Assignments - Android App

Built APK: VeracrossAssignments.apk

## 3 Login Methods (Way 1, 2, 3)

### Way 1 - In-app Google SSO (WebLiteSignIn rewrite)
- The app uses a WebView with Google\'s WebLiteSignIn flow (embedded-friendly, skips Botguard).
- Uses the genuine WebView User-Agent (no spoofing).
- Auto-detects session cookie and proceeds.
- May not work on very old WebViews (device has v74).

### Way 2 - Direct Veracross username/password (FOOLPROOF)
- Use your Veracross username and password (not Google SSO).
- Never touches Google OAuth, so Google\'s block doesn\'t apply.
- Auto-detects session cookie and proceeds.
- If Google-only account: tap "Can\'t access your account?" to set a password first.

### Way 3 - Backend-assisted Google OAuth (fully automatic)
- Deploy the backend (veracross-backend/) to a Node.js host.
- In the app, tap "Backend login" and enter the backend URL.
- Enter Google credentials on the backend page.
- Backend uses headless Chrome to log in, captures cookie, returns to app via deep link.
- Requires deploying a backend (security: credentials go to your server).

## Fallback: Paste cookies
- If all else fails, log in via Chrome, copy cookies, paste into the app.

## Features
- Background checks using WorkManager (configurable times per day in Settings).
- Test/Quiz due tomorrow: non-dismissible (ongoing) high-priority notification.
- Homework due tomorrow: dismissible notification with details + link.
- New assignment/test appears: notification the first time it is seen.
- Reschedules checks after reboot.

## Install
1. Copy VeracrossAssignments.apk to your phone.
2. Open it and allow installation from unknown sources.
3. Open the app and use one of the 3 login methods.
4. Android 13+: grant notification permission.
5. Settings: set checks per day (e.g. 4).

## Rebuild
Requires JDK 21 + Android SDK (platforms;android-34, build-tools 34.0.0).
```
set JAVA_HOME=C:\jdk21\jdk-21.0.5+11
set ANDROID_HOME=C:\AndroidSdk
gradle assembleDebug
```
