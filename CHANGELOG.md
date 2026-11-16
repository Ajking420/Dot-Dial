# Changelog

## v2.2 — scroll performance update

- Corrected refresh-rate selection for the dialer's display-mode preference.
- When the requested refresh rate is supported, the exact mode is selected.
- When the exact rate is unavailable, the selector chooses the lowest supported rate above the requested target, falling back to the fastest compatible mode.
- `AUTO` leaves display-mode selection to Android.
- Google Sign-In was restored for the tested scroll-fixed APK by registering that APK's signing-certificate SHA-1 fingerprint in Firebase. This is a Firebase project configuration change, not a change to the Google Sign-In source implementation.

### Distribution note

The tested v2.2 APK must retain its signing certificate for in-place updates. Do not create a new temporary keystore for a later build and expect Android/Firebase sign-in compatibility to remain unchanged.

## Previous release

- The repository previously contained a v1.0 release under the tag `Nothing_inspired_Dialer`.
