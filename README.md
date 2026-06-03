# iBridge

Mirror your **Android** phone's notifications to an **iPhone and Apple Watch** — with no Mac, no Xcode, and no Apple Developer account.

iBridge is a small Android app that listens for your notifications and forwards them to a private [ntfy](https://ntfy.sh) channel. The free ntfy app on the iPhone receives them via Apple Push, and the Apple Watch mirrors them automatically. Everything runs on the phone and goes straight to ntfy — there's no server to host.

## How it works

```
Android phone (iBridge app)
   └─ posts each notification to →  ntfy.sh (your private topic)
                                      └─ Apple Push (APNs) →  ntfy app on iPhone
                                                                └─ Apple Watch mirrors it
```

## Features

- **Direct to ntfy** — no relay or backend to run; the app talks to ntfy.sh itself.
- **Private channels** — generate a random, unguessable ntfy topic in one tap.
- **On-phone filtering** — block/allow individual apps; only what you want reaches your watch.
- **Quiet hours** — pause mirroring during a time window (handles overnight ranges).
- **Master on/off switch** and a one-tap test notification.
- **No accounts, no secrets baked in** — each user sets up their own topic.

## Requirements

- An Android phone (Android 8.0 / API 26 or newer)
- An iPhone with the free **ntfy** app ([App Store](https://apps.apple.com/app/ntfy/id1625396347))
- An Apple Watch paired to that iPhone (notifications mirror automatically)

## Setup

1. Install iBridge on the Android phone (see **Building** below, or sideload the APK).
2. Open iBridge → **Setup guide** → **Generate random** → **Save topic**. Keep this topic secret — anyone who knows it can read your notifications.
3. On the iPhone, install the **ntfy** app, tap **+**, and subscribe to the **same topic** on the default server (`ntfy.sh`). Allow notifications when prompted.
4. Back in iBridge, tap **Grant notification access** and enable iBridge in the system list.
5. Turn the **Mirroring enabled** switch on, then **Send test notification** to confirm it reaches your watch.

> **Samsung tip:** to stop the OS killing the listener, set **Settings → Apps → iBridge → Battery → Unrestricted**.

## Building

Open the project in Android Studio and either:

- **Run** it on a connected phone (▶), or
- Build a shareable APK: **Build → Build Bundle(s) / APK(s) → Build APK(s)**. The file lands at
  `app/build/outputs/apk/debug/app-debug.apk` — send that to anyone; they install it after enabling "Install unknown apps".

No API keys or configuration files are required to build.

## Privacy

Notification content is sent to the ntfy server you choose (ntfy.sh by default) and pushed to your devices. Use a long, random topic so it stays private. iBridge stores its settings only on your phone; it has no analytics and no accounts.

## Tech

- Kotlin, Android Views (no Compose), minSdk 26
- `NotificationListenerService` to capture notifications
- Plain `HttpURLConnection` to publish to ntfy's JSON API — no third-party libraries
- Settings stored in `SharedPreferences`

## License

MIT — do what you like; no warranty.
