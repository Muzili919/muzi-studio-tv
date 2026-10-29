# Android 6 fresh-install subscription repair

Applies to `muzi-player-4.0.7-muzi.10.apk` (422), SHA256
`876ec9c085c3c5c19a271ade2afb6dd134880aecca5c43706b4f1629843d0ec0`.
The candidate is 423 / `4.0.7-muzi.11`. No desktop or modern player code changes.

`DefaultConfig.ensure()` runs at the end of the player's Application startup,
before an activity loads subscriptions. It initializes only a slot with an empty
saved URL, using the existing Config database API and public configuration URL.
It never reads or migrates viewing history. Existing local and remote user URLs
are retained. Errors are logged per slot without terminating startup.

The HomeSurface patch hides VOD filters and posters while the existing live
entry loads. Selecting VOD restores them. The existing live retry/settings UI
is retained.

Build with JDK17, Android build-tools34.0.0 and apktool2.11.1:

1. Decode the input APK with apktool `d --no-assets` (decode resources).
2. Compile DefaultConfig.java against Android34 android.jar with Java8 target.
3. Convert its class with d8 `--min-api 23`, then use Disassemble.java with
   apktool.jar to decode that helper dex into smali.
4. Run `python3 apply.py DECODED_APK HELPER_SMALI ORIGINAL_APK`.
   The script restores all original assets, adds the startup hook and helper,
   patches live visibility and updates apktool.yml's version.
5. Build with apktool, zipalign, sign using the existing release key outside the
   repository, verify the signer and decode the signed APK to verify its hooks.

Compile the helper with `tests/android/util/Log.java`, the test Config fixture
and DefaultConfigTest.java, then run DefaultConfigTest for six policy checks.
These fixture checks do not verify Android Room startup or actual TV playback.

The delivered APK passed build/signature and hook inspection. Original asset and
native library bytes were compared. **Fresh-install and playback acceptance on
a real TV remain outstanding**, including the difference between a cold start
and an already configured installation. Publication was explicitly requested
after this limitation was reported; do not describe it as device-validated.
