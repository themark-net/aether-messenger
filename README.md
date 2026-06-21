# Aether Messenger

Modern, free and open source Android messaging client focused on excellent reaction support, clean Material You design, and GrapheneOS compatibility. Built on standard Android SMS/MMS APIs as a foundation for advanced messaging.

**Important limitations (read this first):**

- This is **not** a full RCS client. Creating a production RCS implementation from scratch is extremely difficult (Google and GrapheneOS have both noted this publicly). RCS requires deep integration with carrier provisioning, specific protocol stacks (SIP/MSRP), and backend infrastructure that is not openly available for third-party apps in a reliable, cross-carrier way.
- What this app **does** provide today:
  - Full SMS sending/receiving.
  - Basic MMS support (text parts).
  - First-class local reactions: Long-press any message to add emoji reactions. Reactions display as pills on the bubble (similar to iMessage/WhatsApp style).
  - Modern Compose UI with Material 3 and dynamic theming.
  - Can be set as your default SMS app.
  - Fully compatible with GrapheneOS (no Google libraries required for core functionality).
  - Ready architecture for future RCS or other advanced protocol integration (the reaction system and message model are designed to extend).
- For true cross-platform advanced messaging with iPhone users today (typing indicators, high-quality media, reliable reactions over data):
  - Use **Molly** (recommended hardened Signal fork on GrapheneOS) or Signal for your important contacts.
  - Keep this app (or Textra/Google Messages) for legacy SMS/MMS.
- Reactions to iOS users without RCS will be local only or sent as follow-up text if you choose. With future RCS support in this app or system, they will map properly.

This project exists because no good FOSS RCS alternative existed. It gives you a high-quality, private, customizable base you control. Extend it, contribute, or use the reaction UI ideas in other projects.

## Features (v1)
- Conversation list with search and unread indicators.
- Message bubbles with sent/received distinction, timestamps, and reaction display.
- Long-press on message → emoji reaction picker (common set + ability to add more).
- Send text messages.
- Receive SMS/MMS and update in real time where possible.
- Material You dynamic colors, dark mode.
- Minimal permissions model.
- Local Room database for messages + reactions (survives app restarts, good for GrapheneOS).
- Option to request default SMS app role.

## Building the APK

**Prerequisites (on your development machine):**
- Android Studio Hedgehog or newer, or command line tools.
- Android SDK (API 34+ recommended, target 35).
- JDK 17 or 21.

**Steps:**
1. Clone this repo.
2. Open in Android Studio or run from terminal:
   ```bash
   ./gradlew assembleDebug
   ```
   (or `./gradlew assembleRelease` after signing config).
3. APK will be in `app/build/outputs/apk/debug/app-debug.apk`.
4. Install via `adb install` or your file manager. On GrapheneOS, allow unknown sources for the app.
5. Grant SMS, Contacts, Phone permissions when prompted.
6. (Optional but recommended) Go to system Settings > Apps > Default apps > SMS app and select Aether.

For release builds, add signing config in `app/build.gradle.kts`.

## GrapheneOS Specific Notes
- Works great without Sandboxed Google Play for core SMS + reactions.
- If you later want RCS, you can still use Google Messages alongside or contribute RCS stack code here when open components become available.
- Pair with Molly for E2EE advanced chats with willing contacts.
- UnifiedPush support can be added easily for battery-efficient notifications without Google.

## Architecture & Extensibility
- Kotlin + Jetpack Compose + Material 3.
- Room for persistence (messages + reactions).
- Clean separation: sms/ helpers, data/ layer, ui/ screens & components.
- The message model uses standard Telephony provider IDs where possible so reactions can be correlated.
- Future work ideas (PRs welcome):
  - Full RCS integration (when feasible).
  - MMS attachment support (images, etc.).
  - Delivery/read reports.
  - Contact avatars and better threading.
  - Backup/restore of messages + reactions.
  - Unified inbox with Signal/Matrix bridges (via Molly or other).

## RCS Attempt (Current Status)
We made a concrete first attempt at RCS support as requested.

**What was added:**
- `rcs/RcsManager.kt` — detailed documentation of exactly why full RCS is hard + a working stub.
- RCS toggle in every conversation screen (top bar).
- `RcsManager.sendMessage()` is called instead of direct SmsHelper. Currently it falls back to SMS while logging the RCS intent.
- Reaction sending path also has RCS hook (local-only for now).

**Current behavior:**
- Toggle "Enable RCS (experimental)" in a chat.
- If Google Messages is installed, it considers RCS "capable".
- Sending still goes over SMS/MMS (reliable fallback).
- All the UI and reaction system remain fully functional.

**Why a full working RCS client is genuinely hard (even though it's a "standardized" protocol):**

The GSMA Universal Profile is a published specification, but real-world RCS is not like implementing HTTP or a simple open protocol:

1. **IMS Provisioning is privileged and carrier-specific**  
   RCS rides on the carrier's IMS core. Registration requires SIP with specific authentication, certificates, and often device identifiers that normal apps cannot access cleanly. On GrapheneOS this is even harder because of background restrictions.

2. **No public client implementation or SDK**  
   Google Messages has deep integration with the system IMS stack and Google's Jibe fallback service. There is no equivalent open library that third parties can use to speak the full protocol (SIP + MSRP + GSMA extensions for chat, reactions, file transfer, group state, etc.).

3. **Massive protocol surface**  
   A compliant client needs to handle capability discovery, presence, one-to-one + group messaging, typing indicators, delivery/read reports, high-res media, and now E2EE negotiation (newer profiles). This is comparable to writing a full modern email client that works with Gmail's proprietary extensions.

4. **Centralized infrastructure in practice**  
   Many carriers route RCS through Google's Jibe platform rather than pure peer-to-peer. Independent clients would need to either partner with Google/carriers or reverse-engineer significant parts.

5. **Interop testing burden**  
   Must work reliably with Apple’s iOS 18+ RCS implementation (including their E2EE path) and every major US carrier. This requires physical devices and SIMs — not something solvable in simulation.

GrapheneOS themselves have said building an alternative is “extremely hard” and are treating a native RCS client as a long-term project.

**This scaffolding is useful because:**
- It documents the exact barriers in code.
- It keeps the app 100% usable today.
- It provides clear extension points (`sendMessage`, `sendReaction`) so when an open RCS library or GrapheneOS-provided component appears, integration is straightforward.
- The reaction system is already designed to map to RCS reactions when available.

If a viable open RCS stack emerges (or GrapheneOS ships their planned client), we can replace the stub in RcsManager with real calls without touching the rest of the app.

## License
AGPL-3.0 or later (or your preferred strong copyleft). See LICENSE file.