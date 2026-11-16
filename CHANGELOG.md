# Changelog

## v2.2 — Stable

- Fixed the scrolling-performance issue in the dialer interface.
- Improved display refresh-rate selection: use an exact supported mode when available, otherwise select a compatible mode according to the configured target. `AUTO` leaves display-mode selection to Android.
- Configured the release build to use a persistent signing key so future updates can retain the same Android signing identity.
- Registered the permanent release certificate's SHA-1 fingerprint in Firebase for Google Sign-In.
- Built and packaged the v2.2 release APK successfully with the permanent signing key.

### Installation and update notes

- Install the **v2.2 Stable** APK from the [GitHub Releases page](https://github.com/Ajking420/Dot-Dial/releases/tag/v2.2-stable).
- This Stable APK uses the permanent release certificate. It may not install as an in-place update over an earlier APK signed with a different certificate. Back up app data before removing an older installation.
- Future releases must be signed with the same permanent key to support in-place Android updates.
- Google Sign-In should be verified on-device after installing this signed APK.

## Previous release

- The repository previously contained a v1.0 release under the tag `Nothing_inspired_Dialer`.
