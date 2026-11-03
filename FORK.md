# Olauncher+ (personal fork)

A fork of [tanujnotes/Olauncher](https://github.com/tanujnotes/Olauncher) that installs **side-by-side** with the Play Store version (`app.olauncher.petra`, shown as "Olauncher+").

## What's different

| Change | Why |
|---|---|
| Up to **10** home-screen apps (upstream: 8) | Needed more slots |
| "Apps on home screen" uses a − / + stepper instead of a popup | A 0–10 popup overflows the screen and can't scroll |
| Own package name, app name and signing key | Install next to the original; updates only from this fork |

## How the fork is organised

The rule: **fork logic lives in fork-only files; upstream files get only small hooks**, each marked `// Olauncher+ fork` (or an XML comment). This keeps upstream merges low-conflict.

| Fork-only files | Purpose |
|---|---|
| `app/petra.gradle` | Package name, release signing, test inputs (applied from the last line of `app/build.gradle`) |
| `app/src/release/res/values/strings.xml` | App name "Olauncher+" |
| `app/src/main/java/app/olauncher/fork/ExtraHomeSlots.kt` | Slots 9–10: storage (same key scheme as upstream) and view helpers |
| `app/src/main/java/app/olauncher/fork/HomeAppsCountDialog.kt` | The count stepper dialog |
| `app/src/main/res/values/fork_styles.xml` | Shared style for the extra slot views |
| `app/src/test/java/app/olauncher/fork/ExtraHomeSlotsTest.kt` | Drift guard (see below) |

Find every hook in upstream files: `git grep -n "Olauncher+ fork"`.

### Drift guard

Slots 9–10 follow upstream's slot 1–8 conventions. `ExtraHomeSlotsTest` reads upstream's sources and **fails the build** if a merge breaks those conventions, e.g. upstream adds a new per-slot setting, renames a pref key, reuses flag numbers 9–10, or restyles the slot views. When it fails, update the fork files to match, then re-run the tests.

## Branches and remotes

- `petra`: the fork's main branch.
- `upstream`: the original repo, **fetch-only** (push URL disabled on purpose).

## Updating from upstream

```sh
git fetch upstream
git merge upstream/master
./gradlew testReleaseUnitTest assembleRelease
adb install -r app/build/outputs/apk/release/app-release.apk
```

Conflicts, if any, will be at the marked hooks.

## Signing

Release builds are signed with the key in `~/.android/olauncher-petra.jks`. Credentials come from environment variables or `~/.gradle/gradle.properties`:
`PETRA_KEYSTORE_FILE`, `PETRA_KEYSTORE_PASSWORD`, `PETRA_KEY_ALIAS`, `PETRA_KEY_PASSWORD`.
**Back up the keystore.** Without it, updates can't be installed over the existing app.

## License

GPLv3, same as upstream.
