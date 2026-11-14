# LX AirPlay Lite (privacy-scoped fork)

This experimental branch turns AriaCast into a single-purpose Android sender:
**LX Music (cn.toside.music.mobile) -> Android AudioPlaybackCapture -> AirPlay 2 receiver**.
The intended receiver is a Denon Home 350.

## What is restricted

- The on-screen target list contains **AirPlay2** devices only.
- The service rejects any destination that is not a single AirPlay2 receiver.
- Audio playback capture uses Android `AudioPlaybackCaptureConfiguration.Builder.addMatchingUid()` with the UID resolved from the **installed** LX Music package.
- If LX Music cannot be resolved, capture fails closed: no fallback to capturing all device media.
- Only `USAGE_MEDIA` and `USAGE_UNKNOWN` from that UID are captured. Game audio is excluded.
- Incoming service intents requesting microphone, USB, or received-audio sources are rejected.
- Physical microphone implementation and on-screen microphone source controls are removed.
- Screen video streaming implementation in `AudioCastService` is removed; video-casting component is not exported from the simplified Android manifest.
- Manifest declares no Bluetooth/NFC, phone-state, boot-completed, notification-listener, or microphone-specific foreground service.
- Removed background artwork HTTP server startup, automatic update checks, plugin execution and web-remote autostart.
- Android auto-backup is disabled for stored pairing details.
- The fork uses a separate Android application ID, `com.newbie7666.lxairplaylite`, so it does not overwrite the original AriaCast installation.

## Expected Android permissions

- `RECORD_AUDIO`: **still required by Android for internal playback capture**. This fork does not open the physical microphone.
- `MediaProjection`: Android's system approval dialog is shown every time a new capture session starts; approval is used for *audio*, not screen video.
- `INTERNET`, `ACCESS_WIFI_STATE`, `CHANGE_WIFI_MULTICAST_STATE`, `ACCESS_NETWORK_STATE`: receiver discovery and audio transport.
- `POST_NOTIFICATIONS`: foreground-service status notification.
- `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PROJECTION`, `WAKE_LOCK`: Android foreground streaming support.

**Security limitations**: This is a purpose-limited modification, not a full third-party code audit. Legacy features may remain in the repository as unreachable source. Android grants the process an audio-record permission; app-specific UID matching is enforced by the capture configuration, not by a narrower OS permission. Only install artifacts you build from a trusted commit.

## How to test on Android

1. Build the branch's **githubDebug** APK through GitHub Actions or Android Studio.
2. Install this as a separate app next to AriaCast.
3. Install LX Music (`cn.toside.music.mobile`), and connect phone and Denon Home 350 to the same reachable LAN.
4. Launch Lite, refresh discovery and select the **AirPlay 2** entry for Home 350.
5. Approve `RECORD_AUDIO` and the Android screen capture / projection dialog.
6. Start playback in LX Music, confirm sound from Home 350 and test 30+ minutes continuous playback.
7. Start another media application while pausing LX: its audio must **not** stream to Home 350. Try a phone voice recording too; ambient microphone sound must not be sent.
8. Stop casting from Lite; confirm Android projection ends and the speaker stops.
9. If the AirPlay receiver requests pairing, enter the displayed PIN in the Lite pairing dialog.

### Acceptance criteria

- Android build and unit tests pass.
- AirPlay 2 device discovery and first pairing work.
- LX Music streams for 30 minutes without dropouts.
- Audio from unrelated apps and ambient microphone **never** reaches the receiver.
- No automatic Internet update, scripting plug-in, web remote, video projection or alternate-source controls run.
- Repeated start/stop restores phone volume and releases media projection.

## Notes

- Receiver and sender must support compatible formats and AirPlay 2 handshakes. Upstream AriaCast's implementation may fall back to legacy RAOP/AirPlay paths, even when a receiver is displayed as AirPlay 2; **do not infer cryptographic AirPlay 2 transport from the device label alone**.
- Android's capture policy of LX Music can prohibit capture regardless of permissions.
- This branch prioritizes retaining proven audio-streaming stability over minimizing every unused Kotlin class in the APK.
- Upstream is GPL-3.0; preserve original copyright and source availability for redistributed APKs.
