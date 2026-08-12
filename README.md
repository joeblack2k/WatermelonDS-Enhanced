# WatermelonDS
A Nintendo DS and DSi emulator for Android, built on top of [melonDS](https://melonds.kuribo64.net/) and the
[melonDS Android port](https://github.com/rafaelvcaetano/melonDS-android) by rafaelvcaetano.

WatermelonDS is a fork that keeps up with upstream while adding RetroAchievements (including offline play), a Vulkan
renderer, RetroArch shader presets and full external display support, wrapped in its own visual identity.



[<img src="https://raw.githubusercontent.com/Kunzisoft/Github-badge/main/get-it-on-github.png" alt="Get it on GitHub" height="80">](https://github.com/SapphireRhodonite/melonDS-android/releases/latest)

<p align="center">
   <img width="450" height="400" alt="WaterMelon1" src="https://github.com/user-attachments/assets/187ea254-877e-4efd-a8dd-93a6023684ad" />
   <img width="450" height="400" alt="WaterMelon1" src="https://github.com/user-attachments/assets/01e82877-a138-44a3-bc71-c24d48cabab6" />
   <img width="450" height="400" alt="WaterMelon1" src="https://github.com/user-attachments/assets/27868058-ce89-470d-afac-b812dea5d1de" />
</p>


# What WatermelonDS adds
Everything from the upstream Android port, plus:

### RetroAchievements
*  Achievements, leaderboards and Hardcore mode, with a shared card style for the in-game popups
*  Your profile and score are shown on load and in the settings screen
*  **Offline play**: unlocks earned without a connection are stored and submitted later. Two providers are available
   the one built into WatermelonDS, or [RAOfflineProxy](https://github.com/SapphireRhodonite) if you prefer to run it
   yourself. Note that RAOfflineProxy does not support Hardcore mode
*  Achievements can be disabled without logging out, and sessions start even for games that have no achievement set

### Renderers
*  Software, OpenGL, Vulkan and compute renderers, with internal resolution scaling
*  Vulkan gained a *fastpath* presentation profile, optimized texture uploads and dynamic scissor clipping
*  **RetroArch shader presets** on every renderer, through [librashader](https://github.com/SnowflakePowered/librashader)
*  Adrenotools driver support on the `gitHub` flavor, for custom Adreno drivers

### External displays
*  Play on a second screen (TV, dock, devices such as the Odin 2), with the touch screen still usable on the handheld
*  Per-ROM external screen and layout settings, screen rotation, backgrounds, video filtering and aspect ratio control
*  Achievements are shown on the external display while you browse your ROM list

### Library and layouts
*  DSiWare titles in the ROM list, box art on the "continue" shelf, and custom ROM names
*  Layout editor with screen transparency, aspect ratio options and separate centering controls
*  Settings backup and restore (requires a restart to apply), with internal and external layouts backed up separately
*  Alternate input bindings and a hold-to-fast-forward button

The core is kept in sync with melonDS 1.0 and later upstream changes.

# Fork maintenance

The maintained fork is `joeblack2k/WatermelonDS-Enhanced`. Its configured
upstream is `SapphireRhodonite/melonDS-android`; the fork's base is commit
`1e0a463d785448ab47f78a12e2a88241df288cf9` (`Fet_OfflineChevos`) and the
current candidate tree is
`b9385a48402dbabc453c8b290ab5c73151b2b5a8`, with `origin`/PR head at
`c68734fe226b70d60b20908b2af191293f084c7a`. The `melonDS-android-lib`
submodule is sourced from
`joeblack2k/melonDS-android-lib` on the public branch
`codex/slot2-camera-contract`, which exposes the baseline gitlink
`e4022d7e7ada535ff5ea9d087db389cc46e8da62`. The former submodule repository
name is stale and is not used.

The current PR is PR #1, open and draft, with base
`Fet_OfflineChevos@1e0a463d785448ab47f78a12e2a88241df288cf9` and head
`codex/watermelon-enhanced-pr@c68734fe226b70d60b20908b2af191293f084c7a`;
the complete uncommitted candidate source diff, including the two untracked
source files `app/src/main/java/me/magnum/melonds/ui/romlist/EnhancementRomListState.kt`
and `app/src/test/java/me/magnum/melonds/ui/emulator/TransientInputLifecycleContractTest.kt`.

M3 evidence is recorded in [`docs/ENHANCED_ACCEPTANCE.md`](docs/ENHANCED_ACCEPTANCE.md).
The repository build and baseline debug launch are evidenced, but distribution
and enhancement runtime acceptance remain blocked. The camera and 60 FPS
add-ons remain `SOURCE_ONLY`; licensing, ROM inputs, RetroAchievements,
gameplay, and physical enhancement/ROM launch, controls, and gameplay gates
remain open. Any black screenshot is non-UI evidence only.
The current lint rerun on `2026-08-12 12:38:21 +0200` failed with 635 errors,
574 warnings, and 1 hint. This is a checkout-wide candidate snapshot, not a
base comparison; the counts are not being classified against the earlier
630/573 result. The seven enhancement `MissingTranslation`
annotations leave zero enhancement-owned `MissingTranslation` findings. One
enhancement-owned `PluralsCandidate` warning remains for
`enhancement_import_success`; `strings.xml` is part of this documentation
batch. These counts are checkout-wide and do not classify every finding as
baseline. The external log SHA-256 is
`11e3ba3fa7b1ca291a34c816d8a2204619d91ea1e74ec3064ee19756ebdf3900`.
The candidate APK SHA-256 is
`37b32ac828008e0c4fcc7b411f5fe06f8a268ec78ac4ff4cefa78155091e740c`.

For the bounded update procedure and the exact reference model, see
[`docs/FORK_MAINTENANCE.md`](docs/FORK_MAINTENANCE.md).

# Missing Features
*  Local Multiplayer
*  DSi SD card support
*  Customizable button skins

# Performance
Performance is solid on 64 bit devices with thread rendering and JIT enabled, and should run at full speed on flagship
devices. Performance on older devices, specially 32 bit devices, is very poor due to the lack of JIT support.

# Integration with third-party frontends
It's possible to launch WatermelonDS from third party frontends. For that, you will need to have the ROMs you want to
launch already scanned by WatermelonDS. Then, you can configure your third-party frontend with the following
configuration:
*  Package name: `me.magnum.melondualds` (nightly builds use `me.magnum.melondualds.nightly`)
*  Activity name: `me.magnum.melonds.ui.emulator.EmulatorActivity`
*  Parameters (choose one):
    * Intent data (preferred) - a URI of the NDS ROM (ZIP and 7z files are supported). Ensure [read permission is granted](https://developer.android.com/reference/android/content/Intent#FLAG_GRANT_READ_URI_PERMISSION)
    * `uri` (deprecated) - a string with the [SAF](https://developer.android.com/guide/topics/providers/create-document-provider) URI of the NDS ROM (ZIP and 7z files are supported)
    * `PATH` (deprecated) - a string with the absolute path to the NDS ROM (ZIP and 7z files are supported)


### Info regarding save files
When launching ROMs from third-party frontends, if WatermelonDS hasn't scanned that particular ROM previously, it won't
be able to create the save file next to the ROM file if the option "Save next to ROM file" is enabled in the settings or
the save file directory is not set. Instead, WatermelonDS will create a save file in
`Android/data/me.magnum.melondualds/files/saves`

# Releases

Builds are published [here](https://github.com/SapphireRhodonite/melonDS-android/releases). Release candidates are
marked as pre-releases; they can contain more bugs than usual and you may need to clear your app data to get them to
work properly after an update.

# Building
To build the project you will need the Android SDK, NDK, CMake, a JDK 21 toolchain and a Rust toolchain
(`cargo` and `rustup`), which is used to build librashader for each ABI.

## Build steps:
1.  Clone the project, including submodules with:
    
    `git clone --recurse-submodules https://github.com/SapphireRhodonite/melonDS-android.git`
2.  Install the Android SDK, NDK (`28.0.13004108`) and CMake
3.  Install Rust with [rustup](https://rustup.rs/). If Android Studio does not inherit your shell `PATH`, set the
    `CARGO` and `RUSTUP` environment variables to the executable paths
4.  Build with:
    1.  Unix: `./gradlew :app:assembleGitHubProdDebug`
    2.  Windows: `gradlew.bat :app:assembleGitHubProdDebug`
5.  The generated APK can be found at `app/build/outputs/apk/gitHubProd/debug`

There are two flavor dimensions, so build tasks combine both: `gitHub` or `playStore` (the former enables adrenotools)
and `prod` or `nightly`. For example, `:app:assembleGitHubNightlyDebug` or `:app:assembleGitHubProdRelease`.

If you want to create a release build, you will need to modify your `local.properties` file to include the following fields:  
*  `MELONDS_KEYSTORE=<path_to_your_keystore>`
*  `MELONDS_KEYSTORE_PASSWORD=<keystore_password>`
*  `MELONDS_KEY_ALIAS=<name_of_your_key_alias>`
*  `MELONDS_KEY_PASSWORD=<key_alias_password>`

# Credits
*  [melonDS](https://github.com/melonDS-emu/melonDS) by Arisotura and contributors — the emulator core
*  [melonDS Android](https://github.com/rafaelvcaetano/melonDS-android) by rafaelvcaetano
*  [librashader](https://github.com/SnowflakePowered/librashader) — RetroArch shader preset support
*  [rcheevos](https://github.com/RetroAchievements/rcheevos) — RetroAchievements support
