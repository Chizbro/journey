# Health Connect: which free Android apps WRITE GPS-derived `DistanceRecord`

**Date of access: 2026-08-06.** Primary sources only: `developer.android.com`, `support.google.com`, F-Droid repository index XML, vendor help centres, and — strongest of all — **the actual artefacts**: open-source repos cloned and grepped at exact release tags, and shipped APKs downloaded and inspected with `aapt2`. Where a claim rests on a vendor's prose rather than on a binary, that is stated explicitly. Marketing copy and third-party blog posts are used nowhere as authority.

---

## Summary: what to install, and the one catch

**The answer is OpenTracks — but you need the NIGHTLY channel, not the stable release.**

`DistanceRecord` writing landed in OpenTracks on **2026-07-31**, *after* the current stable release v4.28.0 (2026-07-06) was cut. The stable build on Play and F-Droid writes only `ExerciseSessionRecord` + route, and Health Connect does **not** derive `DistanceRecord` from an exercise session. So the stable build is useless for our purpose; the nightly build is exactly what we want.

| | Verdict |
| --- | --- |
| **Install this** | **OpenTracks (Nightly)**, package `de.dennisguse.opentracks.nightly`, version ≥ `v4.28.0-31-g01ea65742` (versionCode ≥ 6730) |
| **Where** | Direct APK from the nightly repo (no Play, no F-Droid client, no account) — see §7. Or add `https://fdroid.storchp.de/fdroid/repo?fingerprint=99985A7E73DCB0B16C9BDDCE7A0B4996F88068AE7C771ED53E217E69CD1FF196` to an F-Droid client. |
| **Writes distance?** | Yes — `DistanceRecord`. Verified three ways: git history, `aapt2` on the shipped APK, and the `DistanceRecord` class present in the APK's DEX (§2.6). |
| **GPS-derived?** | Yes — `TrackStatistics.totalDistance`, accumulated from `trackPoint.distanceToPrevious(...)`. Never step-derived; the APK does not even request `ACTIVITY_RECOGNITION`, so it *cannot* read the step counter. |
| **Free / account / hardware** | Free, Apache-2.0, **no account**, **no `INTERNET` permission at all**, phone-only |
| **Gotcha** | Export is **off by default**. Settings → Import and Export → "Instant post-workout export" must be switched on (§2.7). |
| **Backup** | **Strava** (§4.1) — the only closed-source app whose own docs explicitly say it sends "time, distance, and calorie data from GPS-based activities" to Health Connect. Stable and released today, but **requires an account**. |
| **Also install** | **Health Connect Toolbox** (§5) — Google's own test app, writes any data type by hand, `adb install`, no account. The right tool for the dedup half of the test. |

**Why this is the ideal calibration instrument, beyond just "it works":** OpenTracks does not request `ACTIVITY_RECOGNITION` and has no step counter. There is therefore *zero* possibility of the circular "distance = steps × assumed stride" trap that would make the calibration worthless. Its distance is pure GPS haversine accumulation. That is a stronger guarantee than we could get from any closed-source app, where we would be trusting a support page.

**Why it is also ideal for the dedup test:** it writes with `Metadata.activelyRecorded(Device(TYPE_PHONE, ...))`, so it appears in Health Connect's Activity priority list as a genuine second phone-based writer, which is exactly the condition under which `aggregate()` is documented to dedupe.

**⚠️ The one app to actively avoid: Fitbit / Google Health.** It *does* write `DistanceRecord` — Google publishes an explicit table saying so — but for phone-only tracking Google also states "Your distance is estimated by your stride length… calculated using your height and sex" and "Your phone's location doesn't affect your step count". That is the exact circularity this experiment exists to avoid, and it would fail *silently*, producing a plausible number that is really just the app's own stride assumption echoed back. If it is installed, remove it or demote it before the calibration walk. See §4.3 — this is the most important negative in the report.

**The one caveat to accept:** a nightly build is by definition untested. For our purposes — a throwaway test instrument, installed to generate two records — this is an acceptable risk. It is not a recommendation to rely on it long-term. When v4.29.0 ships, the stable channel will have this feature.

---

## 1. The premise, re-confirmed: nothing native writes distance

Health Connect's own on-device recording collects **steps only**. Google's user-facing help page for the feature is titled "Track your steps with Health Connect" and describes only step collection — "From your Android device, Health Connect can automatically collect step data" — with no distance equivalent ([Track your steps with Health Connect](https://support.google.com/android/answer/16786157?hl=en)).

And Health Connect does **not** synthesise distance from anything else. `Distance` is a first-class interval record with mandatory fields `distance`, `startTime`, `endTime`, `metadata`; it must be written explicitly by some app ([Health Connect data types](https://developer.android.com/health-and-fitness/health-connect/data-types)). Writing an `ExerciseSessionRecord` — even one carrying a full GPS route — does not populate `DistanceRecord`, and therefore does not contribute to `aggregate(DistanceRecord.DISTANCE_TOTAL)`. **This is the single fact that disqualifies OpenTracks stable v4.28.0** (§2).

Permission strings, confirmed: read `android.permission.health.READ_DISTANCE`, write `android.permission.health.WRITE_DISTANCE` ([Health Connect data types](https://developer.android.com/health-and-fitness/health-connect/data-types)).

### ⚠️ Google's official "apps that work with Health Connect" list no longer exists

Worth recording as a negative, because it is the source the task hoped to lean on. Neither of the two places it historically lived now carries an app list:

- [health.google/health-connect-android/](https://health.google/health-connect-android/) — describes Health Connect and claims "over 50 data types across activity, sleep, nutrition, vitals and now medical records", but names **no apps**.
- [support.google.com/android/answer/12201227](https://support.google.com/android/answer/12201227) — covers how to connect an app and grant permissions; contains **no compatible-app list**, and no guidance on priority or dedup.

One artefact does survive — the Play Store curated collection "Works with Health Connect" (§4) — but it only names apps; it says nothing about direction or data types, so it cannot answer requirement (a) or (b) for anything.

Consequence: there is no authoritative registry mapping app → data types written. Every claim below had to be established from source code or from each vendor's own support documentation, which is why the confidence levels differ so sharply between §2 and §4.

---

## 2. OpenTracks — the recommendation, with the release-timing trap

Repository: [codeberg.org/OpenTracksApp/OpenTracks](https://codeberg.org/OpenTracksApp/OpenTracks) (the GitHub repo is now a redirect stub). Licence Apache-2.0. Verified by local clone on 2026-08-06.

### 2.1 The trap: stable v4.28.0 does NOT write distance

v4.28.0's changelog says "Support for exporting to Health Connect" ([OpenTracks releases](https://codeberg.org/OpenTracksApp/OpenTracks/releases)), which reads as if the job is done. It is not. Checking out the tag and grepping the manifest:

```
$ git clone --branch v4.28.0 https://codeberg.org/OpenTracksApp/OpenTracks.git
$ grep -n "permission.health" src/main/AndroidManifest.xml
72:    <uses-permission android:name="android.permission.health.WRITE_EXERCISE" />
73:    <uses-permission android:name="android.permission.health.WRITE_EXERCISE_ROUTE" />
```

Only two health permissions — **no `WRITE_DISTANCE`**. And the exporter at that tag builds only one record type:

```
$ grep -n "Record(" src/.../healthconnect/exporter/HealthConnectWorker.java
118:        ExerciseSessionRecord record = new ExerciseSessionRecord(
```

Corroborated independently by F-Droid, which lists v4.28.0's health permissions as exactly `WRITE_EXERCISE` and `WRITE_EXERCISE_ROUTE` ([F-Droid: de.dennisguse.opentracks.playstore](https://f-droid.org/en/packages/de.dennisguse.opentracks.playstore/); [F-Droid: de.dennisguse.opentracks](https://f-droid.org/en/packages/de.dennisguse.opentracks/)).

So **installing OpenTracks from Play or from the main F-Droid repo today would produce no `DistanceRecord` and the test would silently yield nothing.** This is precisely the sort of failure that would have been misdiagnosed as "Health Connect dedup is broken".

### 2.2 The fix: distance landed on `main` on 2026-07-31

`git log` on the Health Connect exporter directory (HEAD `a542f4566`, 2026-08-05):

```
a542f4566 2026-08-05 Bugfix: HealthConnect doesn't provide granted permissions in time.
1eeb70633 2026-08-05 Bugfix: HealthConnect requires unique timestamps for locations.
e9456919c 2026-08-03 Bug: don't crash in settings if HealthConnect is not installed.
01ea65742 2026-07-31 HealthConnect: send total distance, elevation gain and speed.   <-- the one we need
00be0b565 2026-07-26 Cleanup: removed unused methods and constants.
95e6448cb 2026-05-31 Health Connect: minimal implementation for writing route data.
713e454e4 2026-05-01 HealthConnect: Queue exports in the background
f0616fc04 2026-05-01 HealthConnect: Add exercise session exporter
```

Commit `01ea65742` touches `AndroidManifest.xml` (+3 lines), `HealthConnectUtils.java`, `HealthConnectWorker.java`. The latest tag is still `v4.28.0` — **this commit is unreleased**.

On `main`, the manifest now declares five health permissions ([AndroidManifest.xml, main](https://codeberg.org/OpenTracksApp/OpenTracks/raw/branch/main/src/main/AndroidManifest.xml)):

```xml
<uses-permission android:name="android.permission.health.WRITE_EXERCISE" />
<uses-permission android:name="android.permission.health.WRITE_EXERCISE_ROUTE" />
<uses-permission android:name="android.permission.health.WRITE_DISTANCE" />
<uses-permission android:name="android.permission.health.WRITE_ELEVATION_GAINED" />
<uses-permission android:name="android.permission.health.WRITE_SPEED" />
```

and the exporter inserts a real `DistanceRecord` ([`HealthConnectWorker.java`](https://codeberg.org/OpenTracksApp/OpenTracks/raw/branch/main/src/main/java/de/dennisguse/opentracks/io/healthconnect/exporter/HealthConnectWorker.java)):

```java
records.add(new DistanceRecord(
    track.startTime().toInstant(),
    track.startTime().getOffset(),
    track.stopTime().toInstant(),
    track.stopTime().getOffset(),
    Length.meters(track.statistics().totalDistance().toM()),
    metadata
));
```

alongside `ExerciseSessionRecord`, `ElevationGainedRecord`, and `SpeedRecord`, all handed to `client.insertRecords(records, ...)`. The permission set requested at runtime matches ([`HealthConnectUtils.java`](https://codeberg.org/OpenTracksApp/OpenTracks/raw/branch/main/src/main/java/de/dennisguse/opentracks/io/healthconnect/exporter/HealthConnectUtils.java)):

```java
public static final Set<String> PERMISSIONS = Set.of(
        HealthPermission.getWritePermission(...ExerciseSessionRecord.class),
        HealthPermission.getWritePermission(...DistanceRecord.class),
        HealthPermission.getWritePermission(...ElevationGainedRecord.class),
        HealthPermission.getWritePermission(...SpeedRecord.class),
        HealthPermission.PERMISSION_WRITE_EXERCISE_ROUTE
);
```

### 2.3 Requirement (c): the distance is GPS-derived — proven, not assumed

This is the load-bearing claim for calibration, so it is worth tracing all the way down. `DistanceRecord` takes `track.statistics().totalDistance()`. That value is accumulated in [`TrackStatisticsUpdater.java`](https://codeberg.org/OpenTracksApp/OpenTracks/raw/branch/main/src/main/java/de/dennisguse/opentracks/data/statistics/TrackStatisticsUpdater.java):

```java
Distance movingDistance = null;
if (trackPoint.sensorDistance() != null) {
    movingDistance = trackPoint.sensorDistance();
} else {
    // GPS-based distance/speed
    movingDistance = trackPoint.distanceToPrevious(lastTrackPoint);
}
if (movingDistance != null) {
    currentSegment.addTotalDistance(movingDistance);
}
```

Two branches, and both are safe for us:

- `sensorDistance()` is populated **only** by a paired Bluetooth LE cycling/running distance sensor. You have no such hardware, so this branch is dead.
- Otherwise: point-to-point geodesic distance between consecutive GPS fixes. **Pure GPS.**

There is no third branch. OpenTracks ships no step counter and has no stride-length setting anywhere — and, decisively, the shipped APK does **not** request `ACTIVITY_RECOGNITION` (verified with `aapt2`, §2.6), without which it cannot read the hardware step counter on Android 10+ at all. The circularity risk the calibration depends on avoiding is not merely unlikely, it is structurally impossible. Compare this to Fitbit/Google Health (§4.3), where the vendor states outright that phone-only distance is estimated from stride length and that "your phone's location doesn't affect your step count" — there, the calibration would be a tautology.

### 2.4 Requirements (d) and (e): free, no account, no hardware, no network

The APK declares no `INTERNET` permission at all — see the permission list in §2.6 below. An app that cannot open a socket cannot require an account. F-Droid's description states "No Internet access or extra permissions" ([F-Droid](https://f-droid.org/en/packages/de.dennisguse.opentracks/)). Apache-2.0, no IAP, no subscription, phone GPS only; BLE sensors are optional.

The stable build is also on Google Play as `de.dennisguse.opentracks.playstore` ([Play listing](https://play.google.com/store/apps/details?id=de.dennisguse.opentracks.playstore)), but as established above the Play build is the wrong one, so Play-availability-in-Australia is moot — go via F-Droid.

### 2.5 The nightly channel — verified to contain the fix

The nightly repo is `https://fdroid.storchp.de/fdroid/repo` (fingerprint `99985A7E73DCB0B16C9BDDCE7A0B4996F88068AE7C771ED53E217E69CD1FF196`), linked from the project README. Parsing that repo's `index.xml` directly on 2026-08-06 gives the decisive evidence — the permission list is extracted from the built APK, not from marketing copy:

| Nightly version | versionCode | Added | Health permissions in the APK |
| --- | --- | --- | --- |
| `v4.28.0-33-ga542f4566` | 6732 | 2026-08-05 | `health.WRITE_DISTANCE`, `health.WRITE_ELEVATION_GAINED`, `health.WRITE_EXERCISE`, `health.WRITE_EXERCISE_ROUTE`, `health.WRITE_SPEED` |
| `v4.28.0-31-g01ea65742` | 6730 | 2026-08-04 | same five, incl. `WRITE_DISTANCE` |
| `v4.28.0-30-ge9456919c` | 6729 | 2026-08-03 | `health.WRITE_EXERCISE`, `health.WRITE_EXERCISE_ROUTE` only |

The boundary is exactly where the git history predicts: build 6729 (pre-`01ea65742`) has no `WRITE_DISTANCE`; build 6730 (the distance commit) has it. **Take versionCode ≥ 6730.** The current nightly, 6732 (2026-08-05), also carries the two 2026-08-05 Health Connect bugfixes, one of which — "HealthConnect doesn't provide granted permissions in time" — is directly relevant to first-run permission handling. Prefer 6732.

Also confirmed from the same index: `targetSdkVersion` 37, `minSdkVersion` 26 — fine for a Pixel 9 Pro on Android 16.

### 2.6 Verified against the actual APK binary

The repo index could in principle be stale or wrong, so the APK itself was downloaded and inspected with `aapt2` on 2026-08-06. This is the strongest evidence available and it settles every requirement at once:

```
$ curl -O https://fdroid.storchp.de/fdroid/repo/de.dennisguse.opentracks.playstore_v4.28.0-33-ga542f4566_6698-nightly-release.apk
$ aapt2 dump badging de.dennisguse.opentracks.playstore_v4.28.0-33-ga542f4566_6698-nightly-release.apk

package: name='de.dennisguse.opentracks.nightly' versionCode='6732'
         versionName='v4.28.0-33-ga542f4566' compileSdkVersion='37'
application-label:'OpenTracks (Nightly)'
uses-permission: name='android.permission.ACCESS_FINE_LOCATION'
uses-permission: name='android.permission.ACCESS_COARSE_LOCATION'
uses-permission: name='android.permission.FOREGROUND_SERVICE_LOCATION'
uses-permission: name='android.permission.health.WRITE_EXERCISE'
uses-permission: name='android.permission.health.WRITE_EXERCISE_ROUTE'
uses-permission: name='android.permission.health.WRITE_DISTANCE'      <-- confirmed in the shipped binary
uses-permission: name='android.permission.health.WRITE_ELEVATION_GAINED'
uses-permission: name='android.permission.health.WRITE_SPEED'
uses-permission: name='android.permission.ACCESS_NETWORK_STATE'
uses-permission: name='android.permission.BLUETOOTH_SCAN' usesPermissionFlags='neverForLocation'
...
```

Four conclusions, all from the binary rather than from anyone's description of it:

1. **Package name is exactly `de.dennisguse.opentracks.nightly`.** This is the string the probe app should expect as the `DataOrigin` package. It differs from stable's `de.dennisguse.opentracks.playstore`, so the two can be installed side by side — that would give a third writer if we ever want one. (Note the *filename* says `.playstore`; that is a build-artifact naming quirk in the nightly pipeline and does not reflect the applicationId. Trust `aapt2`, not the filename.)
2. **`WRITE_DISTANCE` is present in the shipped APK.** Not just on `main`, not just in an index — in the file you will install.
3. **No `android.permission.INTERNET`.** The full permission list has `ACCESS_NETWORK_STATE` but not `INTERNET`. The app literally cannot make a network request, which forecloses any account, telemetry, or cloud sync. Requirement (d) is satisfied at the strongest possible level.
4. **No `android.permission.ACTIVITY_RECOGNITION`.** This is the decisive one for requirement (c): without it the app cannot access the hardware step counter on Android 10+. It is structurally incapable of deriving distance from steps. The calibration cannot be circular.

A declared permission alone would only prove intent, so the compiled code was checked too. Unzipping the APK's DEX files and grepping the string table:

```
$ unzip -q ot_nightly.apk 'classes*.dex' && strings -a classes*.dex | grep -i 'DistanceRecord\|healthconnect' | sort -u
Landroidx/health/connect/client/records/DistanceRecord;
Landroidx/health/connect/client/records/DistanceRecord$Companion;
Landroidx/health/connect/client/records/DistanceRecord$Companion$DISTANCE_TOTAL$1;
Lde/dennisguse/opentracks/io/healthconnect/exporter/HealthConnectWorker;
Lde/dennisguse/opentracks/io/healthconnect/exporter/HealthConnectUtils;
...
```

The `DistanceRecord` class is genuinely linked into the shipped binary alongside `HealthConnectWorker`. Permission, code, and source history all agree.

APK is 10.8 MB, served with `content-type: application/vnd.android.package-archive` — a plain `adb install` works, no F-Droid client needed.

### 2.7 How the export is triggered — must be enabled, and it is not automatic by default

Not automatic out of the box. From `HealthConnectUtils.java`:

```java
public static void postExportTrack(Context context, Track.Id trackId) {
    if (!PreferencesUtils.shouldInstantExportToHealthConnect()) {
        return;      // <-- silently does nothing unless the setting is on
    }
    ...WorkManager.getInstance(context).enqueue(request);
}
```

- `postExportTrack(...)` is called from `TrackStoppedActivity.java:97` — i.e. when you finish and save a recording, **gated on the "instant export to Health Connect" preference.**
- `postExportAll(...)` is called from `settings/HealthConnect.java:49` — a manual "export all existing tracks" button. Useful as a fallback if the per-track export misfires, and useful to backfill.

**The preference defaults to OFF.** From `res/values/settings.xml:62`:

```xml
<bool name="post_workout_export_health_connect_enabled_default" translatable="false">false</bool>
```

So a fresh install records a perfect GPS track and writes **nothing** to Health Connect. This is the most likely way the test fails silently. The exact UI path, from `res/xml/settings_import_export.xml` and `res/values/strings.xml`:

> **Settings → "Import and Export" → "Export to Health Connect"** category, which contains three items:
> 1. **"Health Connect settings"** — "Open the Health Connect settings" → launches the permission grant flow (`HealthConnect$Settings`). Do this first.
> 2. **"Export all"** — bulk-exports every existing track (`HealthConnect$ExportAll`). Your fallback/backfill.
> 3. **"Instant post-workout export"** — "Export activity to Health Connect after recording is finished". **This is the switch you must turn on.**

Also note the permission rationale string, which is a nice confirmation of the one-way behaviour we want: *"If enabled in the settings, OpenTracks writes exercise to Health Connect. OpenTracks does not read data from Health Connect."*

Export runs via `WorkManager` as a `OneTimeWorkRequest`, so it is asynchronous and may lag the end of the walk by a short interval. Do not expect the record to appear the instant you tap stop.

Metadata written: `Metadata.activelyRecorded(new Device(Device.TYPE_PHONE, Build.MANUFACTURER, Build.MODEL))`, with `Track.uuid` used as `clientRecordId` "to prevent duplicates on re-export". Two consequences that matter to us:

1. `recordingMethod` = actively-recorded and `Device.TYPE_PHONE` — it presents as a legitimate phone-recorded source, which is what the priority list operates on.
2. Re-exporting the same track is idempotent, so hitting "export all" repeatedly will not inflate the totals. Convenient for iterating on the probe app.

---

## 3. FitoTrack and RunnerUp — definitive negatives

Both were cloned and grepped on 2026-08-06. Recording these explicitly so nobody re-investigates them.

### FitoTrack — no Health Connect support whatsoever

[codeberg.org/jannis/FitoTrack](https://codeberg.org/jannis/FitoTrack), HEAD `5172d24`.

```
$ grep -ril "healthconnect\|health\.connect\|DistanceRecord\|insertRecords\|permission\.health" .
(no output — zero matches)
```

Zero matches across the entire repository. No `androidx.health.connect` dependency in `app/build.gradle`. The manifest's permission list contains no `android.permission.health.*` entries — the closest is `android.permission.FOREGROUND_SERVICE_HEALTH`, which is an unrelated foreground-service type and is **not** a Health Connect permission (a plausible source of a false positive if someone greps carelessly for "HEALTH"). FitoTrack does declare `INTERNET` and `ACTIVITY_RECOGNITION`, unlike OpenTracks.

**Verdict: unusable. Cannot write to Health Connect at all.** Its "auto export" feature ([wiki](https://codeberg.org/jannis/FitoTrack/wiki/Auto-Export)) is file/GPX export to storage, which is a different thing entirely and is likely what any secondary source describing FitoTrack "export" is referring to.

### RunnerUp — no Health Connect support either

[github.com/jonasoreland/runnerup](https://github.com/jonasoreland/runnerup), HEAD `5ca438f` (2026-08-01), so actively maintained — this is a current negative, not a stale one.

```
$ grep -ril "healthconnect\|health\.connect\|DistanceRecord\|permission\.health" .
(no output — zero matches)
```

No manifest in the repo declares any `permission.health.*`. **Verdict: unusable.** RunnerUp exports to Strava/Runkeeper/etc. over their web APIs, not to Health Connect.

---

## 4. Commercial candidates

A methodological note that applies to everything in this section. Because Google's app registry is gone (§1) and because **every APK-analysis mirror blocked automated access** (AppBrain, APKPure, APKCombo, APKMonk, and the Wayback Machine all returned 403/CAPTCHA/DNS failures), *no* closed-source app below could be verified at the manifest level the way OpenTracks was. Everything here rests on vendor help-centre documentation. That is a genuinely weaker class of evidence, and it is why §2's open-source verification is worth so much more.

One official Google artefact does survive: the Play Store curated collection **"Works with Health Connect"** ([play.google.com/store/apps/collection/promotion_all__health_connect](https://play.google.com/store/apps/collection/promotion_all__health_connect)), listing Google Health (Fitbit), Samsung Health, MyFitnessPal, AllTrails, Garmin Connect, Flo, Cronometer, MyNetDiary, Runna, Home Workout, Mi Fitness, myAir, BetterSleep, fatsecret, Nike Run Club, Withings, Alpha Progression, Slopes, Dexcom. It is curated and paginated, so absence is suggestive but not proof, and — critically — **it does not say which data types each app writes, or in which direction.** It cannot settle requirement (b) for anything.

### 4.1 Strava — the only closed-source app definitively proven to write GPS distance ✅

This is the backup recommendation. Strava's own help centre states it plainly, in two separate articles with identical wording ([Health Connect and Strava](https://support.strava.com/en-us/articles/15401554-health-connect-and-strava); [Syncing Strava Activities with Health Connect](https://support.strava.com/hc/en-us/articles/43440435267597-Syncing-Strava-Activities-with-Health-Connect)):

> Strava sends **"time, distance, and calorie data from GPS-based activities"** to Health Connect, and reads back only **"weight data."**

That single sentence satisfies (a) writes-not-just-reads, (b) distance specifically, and (c) GPS-derived — the phrase "from GPS-based activities" is exactly the scoping we needed, and it rules out step×stride. No other closed-source candidate has documentation this precise.

- **(d) Free, but account is mandatory.** "there is no cost to record and share your activities" ([Is Strava free?](https://support.strava.com/hc/en-us/articles/216917627-Is-Strava-free)); signup required at [strava.com/register/free](https://www.strava.com/register/free). ⚠️ This is a real cost given the preference to avoid accounts — but see the note below: it is unavoidable across *every* commercial candidate, so it is not a tiebreaker among them, only a reason to prefer OpenTracks.
- **(e) Available in Australia, phone-only**, no wearable required ([Play AU listing](https://play.google.com/store/apps/details?id=com.strava&hl=en_AU)). Note the integration is **Android-app-only**: "This integration is only available on the Strava Android app".
- **(f) Recent change, and a trap worth knowing.** Strava's *Google Health (Fitbit)* integration went one-way in 2026 — "Activities logged in Strava will no longer sync to Google Health via the Strava integration, regardless of your connection settings" ([Google Health and Strava](https://support.strava.com/en-us/articles/15401544-google-health-and-strava)). The **Health Connect** path is a separate integration and still carries distance outbound. Do not conflate the two; connecting the wrong one would produce no distance.

### 4.2 The rest — integration plausible, distance unproven

None of these should be installed on current evidence. Recording them so the question is closed.

| App | Writes to HC? | Writes **Distance**? | Evidence grade |
| --- | --- | --- | --- |
| **MapMyWalk / MapMyRun** | ✅ Yes, one-way | ⚠️ Unverified — docs say "workouts" | First-party, but vague |
| **adidas Running (Runtastic)** | 🟡 Probable | ⚠️ Unverified | Third-party only; first-party docs offline |
| **Nike Run Club** | 🟡 Listed by Google | ⚠️ Unverified | Collection listing only; Nike silent |
| **komoot** | ❌ No | ❌ No | Strong first-party negative |

**MapMyWalk / MapMyRun (Under Armour)** — write direction is confirmed and unambiguous ([Google Health Connect, UA help](https://help.mapmyfitness.com/hc/en-us/articles/42203015991575-Google-Health-Connect)): "This connection only works in one direction. We send MapMy workouts to Health Connect but we do not receive any workouts from Health Connect." But it says *workouts*, never naming data types, so (b) is unproven. Circumstantial only: UA's *Samsung Health* article lists synced fields as "exercise type, calories, distance, duration, and start/end times" ([Samsung Health Integration](https://help.mapmyfitness.com/hc/en-us/articles/36780316319767-Samsung-Health-Integration)) — plausible the HC payload matches, but that is inference across two different integrations and must not be treated as evidence. Free tier exists, account required ([Getting Started](https://help.mapmyfitness.com/hc/en-us/articles/36601327669143)), MVP is the paid tier. Available in AU. **Flagged as recent:** that Health Connect help article's Zendesk metadata shows it was created and last updated on **2026-07-24**, i.e. roughly two weeks before access — UA appears to have shipped HC support in mid-2026, so anything written about this app before July 2026 is stale.

**adidas Running (Runtastic)** — the weakest documentation of the set, because **`help.runtastic.com` has been decommissioned**: every URL, including its Zendesk API, now 301-redirects to `adidas.com.au/help/pac-contact-us`. Two independent corporate-wellness vendors document a "Google Health Connect" entry in the app's own Android UI at Profile → Settings → App Settings → Partner Accounts ([movezengo](https://support.movezengo.com/article/157-adidas-running-runtastic), dated 2024-09-16; [Personify Health](https://personifyhealth.zendesk.com/hc/en-us/articles/28015903036315-How-to-connect-Adidas-Running-to-Google-Fit)), which makes the integration likely real and outbound — but these are third-party and the one sentence naming "distance, calories, and duration" sits inside an article titled for *Google Fit*, so it cannot be cleanly attributed to the HC path. "Health Connect" is **absent** from the Play descriptions in `en_AU`, `en_US` and `en_NZ` ([Play AU](https://play.google.com/store/apps/details?id=com.runtastic.android&hl=en_AU)). ⚠️ Ads, IAP, account required.

> ⚠️ **Search-summary artefact, flagged.** Several search engines confidently surfaced a sentence about adidas Running letting you "sync your activities effortlessly with Health Connect… including Garmin, Polar, Amazfit/Zepp, Coros, Suunto, Wahoo". The pages it was attributed to were fetched directly and **do not contain that sentence**. It appears to be generated summary text, not a quote. Do not propagate it.

**Nike Run Club** — appears by name in Google's "Works with Health Connect" collection, so an integration exists. But Nike publishes **no** Health Connect help article at all: [connect-nrc-health-connect](https://www.nike.com/help/a/connect-nrc-health-connect) returns "Article Not Found", and the partner-apps article ([connect-nrc-partner-apps-devices](https://www.nike.com/help/a/connect-nrc-partner-apps-devices)) names only Strava and Garmin and never mentions Health Connect. Nike's only health-platform documentation is **Apple Health — iOS only** ([connect-nrc-health-app](https://www.nike.com/help/a/connect-nrc-health-app)). The Play listing still advertises only Google Fit ([Play AU](https://play.google.com/store/apps/details?id=com.nike.plusgps&hl=en_AU)) — itself a red flag, since the Google Fit APIs are being retired ([developers.google.com/fit](https://developers.google.com/fit)). Direction and data types both unknown. Nike Member account mandatory.

**komoot — definitive negative.** Its help-centre search API was queried across "health connect", "Android Google fitness sync", and "Google": **zero** matching articles. The Connected Devices & Integrations category ([link](https://support.komoot.com/hc/en-us/categories/10073828995482-Connected-Devices-and-Integrations)) lists Garmin, Wahoo, COROS, Suunto, Bryton, Lezyne, TwoNav, Meilan, Amazfit, Huawei, Samsung watches and Wear OS — no Android health platform. Its only health-platform integration is Apple Health, and komoot states explicitly that it is iOS-exclusive: "Linking to Apple Health is only possible through the komoot app on iOS devices. This connection cannot be established via the Android version of the komoot app" ([Sync Apple Health with komoot](https://support.komoot.com/hc/en-us/articles/10449275638170-Sync-Apple-Health-with-komoot), updated 2026-07-30). The 2026 feature changelog ([link](https://support.komoot.com/hc/en-us/articles/10621431252250)) records no HC work through July 2026. A [komoot marketing page](https://www.komoot.com/running-app/running-app-with-samsung-health-integration) claims Samsung Health sync "including distance, speed, and elevation", but it never mentions Health Connect and no help article backs it — marketing copy, disregarded.

### 4.3 ⚠️ Fitbit / Google Health — the trap: it DOES write distance, and that distance is step-derived

**This is the most dangerous candidate on the list, and the one most likely to be suggested.** It passes requirements (a) and (b) cleanly and then fails (c) in exactly the way that would silently destroy the calibration.

First, the good news, and it is unusually well documented. Google publishes an explicit read/write table for the Fitbit / Google Health app's Health Connect integration ([How do I use Health Connect in the Fitbit app?](https://support.google.com/fitbit/answer/14506680?hl=en-AU)):

- **Writes to Health Connect** (Fitness): "Steps, Speed, Step cadence, VO2 max, Floors, **Distance**, Elevation gained, Exercise, Exercise route, Total calories burned"
- **Reads from Health Connect** (Fitness): "Steps, VO2 max, Floors, Active calories burned, Distance, Exercise, Total calories burned"

So Distance is written. This is the clearest data-type table any vendor publishes — better even than Strava's.

**Now the disqualifying part.** For phone-only tracking with no Fitbit device (the old MobileTrack feature, which still exists), Google states plainly ([Track steps with your phone](https://support.google.com/fitbit/answer/14236404)):

> "Use the Google Health app to track your basic activities directly from your phone. These activities include steps, distance, and calories burned."
>
> **"Your distance is estimated by your stride length. By default, stride length is calculated using your height and sex."**
>
> **"Your phone's location doesn't affect your step count."**

That is the circular trap, stated by the vendor in its own words. Its phone-only `DistanceRecord` is literally `steps × a stride length inferred from your height and sex`. Feeding it into `distance ÷ steps` would return **the app's assumed stride length**, not the user's true one. The calibration would appear to succeed, produce a plausible-looking number, and be **completely worthless** — the worst possible failure mode, because nothing about it looks wrong.

Worse, it would also be the higher-priority source by default in many setups, so it could displace a genuine GPS figure in `aggregate()`.

**Verdict: do not install for calibration.** If it is already installed, it should be *removed* or demoted in the priority list before the calibration walk, and the probe app should check whether `com.google.android.apps.fitbit` (or equivalent) appears among `DistanceRecord` sources.

The one nuance: Fitbit's separate GPS-session feature does use phone GPS for a recorded activity. **Confirmed on the second pass (2026-08-06):** the app became **Google Health** on 19 May 2026 (package unchanged, `com.fitbit.FitbitMobile`), and phone-only users can "start workout tracking from your phone", capturing "your route, distance, pace, and more" via Today → Add → Start Exercise ([feature map](https://support.google.com/product-documentation/answer/17081467), [how-to](https://support.google.com/googlehealth/answer/14225688)).

**This makes it worse, not better.** Both the step-derived passive distance and the GPS session distance are written by the *same package* through the same path, so once in Health Connect they are indistinguishable by `dataOrigin`. A calibration run could silently blend a real measurement with an assumption and still look entirely plausible. **Do not use it for calibration**, and it requires a Google account besides.

> **Generalise this.** Any app whose primary identity is a pedometer or all-day activity tracker — Fitbit, Pacer, Samsung Health's step tracking, Google Fit's — is likely to compute distance as steps × stride. Pacer's own help material describes pedometer distance as "[number of steps] x [average step length]", with Android users able to set their own step length ([Pacer help centre](https://support.mypacer.com/hc/en-us/articles/42318897381005-Steps-Count-but-Distance-Does-Not-Explanation-and-Fixes) — page returned 403 to direct fetch, so this rests on the indexed summary and is **unverified**). The rule to carry forward: **a `DistanceRecord` is only useful to us if it came from a GPS-recorded outdoor session.** Requirement (c) is not a nice-to-have; it is the whole experiment.

### 4.4 Samsung Health, Garmin, Polar, Sports Tracker, Pacer, Google Fit — resolved (2026-08-06)

A second research pass closed this group. None of it changes the recommendation, but the negatives are worth recording so nobody re-investigates them.

**Write Distance, and could serve as a fallback:**

- **Polar Flow** — the best non-Google, hardware-free fallback. Polar's own list includes "**Distance**: if training session has distance information" and "Exercise route: if available" ([support.polar.com](https://support.polar.com/en/flow-app-health-connect)). Write-only, no documented read. Phone GPS works with no watch: "If you've selected an outdoor sport profile, the **Phone GPS is switched on**" ([training recording](https://support.polar.com/en/flow-app-training-recording)); the HR sensor is explicitly optional. Health Connect replaced Google Fit in Flow v6.22.0 (22 May 2024). **A Polar account is mandatory.** Caveat: Polar never states in a single document that a phone-GPS Flow session lands in HC as Distance — that conclusion joins two pages. Strong inference, not one citation.
- **Samsung Health** — Samsung's developer blog publishes an explicit mapping including `Exercise, distance` → `DistanceRecord`, bidirectional sync ([developer.samsung.com](https://developer.samsung.com/health/blog/en/accessing-samsung-health-data-through-health-connect)). GPS distance without a watch is documented. **But its behaviour on a Pixel 9 Pro is UNVERIFIED** — the "works on non-Samsung phones" statement comes from the *deprecated* Samsung Health SDK FAQ and may be stale, while the Play listing hedges "some mobile devices are not supported". Samsung account needed for sync.
- **Garmin Connect** — does write Distance, one-way ([support.garmin.com](https://support.garmin.com/en-US/?faq=JToBEy0jfe6pIygark2Ui5)), but **fails the phone-only requirement outright**: there is no standalone phone-GPS recording, and the device-free manual-activity fallback has "no GPS map". Needs Garmin hardware.

**Definitive negatives — no Health Connect integration exists at all:**

- **Pacer** — rejected on two independent grounds. Its official Android integrations list omits Health Connect entirely ([support.mypacer.com](https://support.mypacer.com/hc/en-us/articles/41658925082253-Which-wearables-3rd-party-app-integration-does-Pacer-support)), and manifest inspection of the current build found zero `android.permission.health.*` declarations. Separately, its default distance is step-derived: "Distance = Steps × Stride Length". Two decoys in its manifest are worth knowing about — `FOREGROUND_SERVICE_HEALTH` is an Android 14 service *type*, and a class named `SHealthConnectManager` refers to Samsung Health, not Health Connect.
- **Sports Tracker** (now published by Suunto Oy) — zero health permissions, and it lacks the `ACTION_SHOW_PERMISSIONS_RATIONALE` activity Google mandates of every HC client. It does integrate Apple Health on iOS; the Android counterpart was never built.
- **Polar Beat** — deliberately not implemented. Polar *removed* Beat's Google Fit link in v3.5.9 and redirected users: "we have removed the connection to Google Fit. Going forward, we recommend using Google's Health Connect **in Polar Flow app**" ([release note](https://support.polar.com/en/updates/polar-beat-version-359-for-android)).

**Google Fit — live but terminal.** Still on Play AU (updated 15 July 2026) and it does write to Health Connect, but Google publishes no data-type list, so **Distance specifically is unverified**. The Fit APIs are supported only "until the end of 2026" ([migration FAQ](https://developer.android.com/health-and-fitness/health-connect/migration/fit/faq)), with users to be migrated into Google Health. No consumer shutdown date has been published. Do not build on it.

**Account signup is uniform across every commercial candidate**, which is a decisive argument for OpenTracks — it requires no account and cannot open a network socket (§2.6).

### 4.5 Account signup is unavoidable across all commercial candidates

Every app in §4 requires creating an account before it will record an activity. Since this is uniform, it cannot discriminate between them — but it is a decisive argument for **OpenTracks, which requires no account and cannot even open a network socket** (§2.6). If avoiding signups matters, the open-source route is not merely equivalent, it is strictly better on this axis.

---

## 5. Health Connect Toolbox — the right tool for objective 2, and it was not on the list

Worth surfacing prominently because it solves the deduplication half of the problem better than any consumer app, and it is Google's own tool.

The **Health Connect Toolbox** is a first-party developer companion app that "supports reading and writing **all** Health Connect data types" ([Test your integration with the Health Connect Toolbox](https://developer.android.com/health-and-fitness/health-connect/test/health-connect-toolbox)). All data types includes Distance. You insert a record by hand:

> "To insert a new health record: 1. Tap on **Insert Health Record**. 2. Select a category. 3. Select a health record type. 4. Enter the value. 5. Tap the **SAVE** button."

"All data types" is a documentation claim, so it was verified the same way OpenTracks was. `https://goo.gle/health-connect-toolbox` 302-redirects to a Google-hosted static file, `https://www.gstatic.com/health-ecosystems/health_connect_toolbox.zip`, which contains a single APK:

```
$ unzip -l health_connect_toolbox.zip
 13066367  2025-12-03 15:07   HealthConnectToolbox-2_3_3.apk

$ aapt2 dump badging HealthConnectToolbox-2_3_3.apk
package: name='androidx.health.connect.client.devtool' versionCode='1' versionName='ToolboxApp-2.3.3'
application-label:'Health Connect Toolbox'
targetSdkVersion:'33'   sdkVersion:'26'
uses-permission: name='android.permission.health.WRITE_DISTANCE'     <-- confirmed
uses-permission: name='android.permission.health.WRITE_STEPS'
uses-permission: name='android.permission.INTERNET'
```

`WRITE_DISTANCE` is confirmed in the binary. Properties that matter here:

- **Free, no account, no Play Store.** `adb install HealthConnectToolbox-2_3_3.apk`. You already have `adb` set up for the probe app, so this is near-zero friction.
- **Its `DataOrigin` package name is `androidx.health.connect.client.devtool`** — that is the string to look for in the probe app and in the priority list, and it is not obviously "Toolbox", so don't scan for the wrong name.
- **It writes distance directly**, so it appears as a distinct source in the Activity priority list.
- **You control the exact value and time window**, which is the thing consumer apps deny you.
- ⚠️ Minor caveats: it *does* declare `INTERNET` (unlike OpenTracks), and it `targetSdkVersion 33` with a build dated 2025-12-03 — old enough to be worth a glance on Android 16, though 33 is comfortably above the minimum install threshold and this is Google's own tool.

### 5.1 Use it for objective 2, not objective 1

**It cannot do the calibration.** The distance is typed in by hand, not measured by GPS, so it carries no independent information about your stride length. Using it for calibration would be worse than circular — it would be measuring a number you invented.

**It is close to ideal for the dedup test**, and better than a second consumer app, because deduplication only engages when two sources have *overlapping time ranges* (§6). With Toolbox you can deliberately author a `DistanceRecord` that exactly overlaps the OpenTracks walk with a deliberately wrong, obviously-distinguishable value — say 9,999 m against a real 1,200 m walk. Then `aggregate(DISTANCE_TOTAL)` should return one or the other depending on priority order, never the sum. That is a clean, unambiguous signal. Trying to engineer the same overlap using two GPS apps means starting and stopping both at the same moment and hoping, and if the totals come out similar you learn nothing about which one won.

**Recommended combination:** OpenTracks Nightly for objective 1 (real GPS distance → stride length), Toolbox for objective 2 (a controlled second writer → dedup proof). These are complementary, and together they need no account of any kind and no third-party service.

---

## 6. Why `aggregate()` is the right call for the dedup test

Confirmed for the second objective. From [Read aggregated data](https://developer.android.com/health-and-fitness/health-connect/aggregate-data):

> "When you perform an aggregate read, the Aggregate API accounts for any duplicate data and keeps only the data from the app with the highest priority. Duplicate data could exist if the user has multiple apps writing the same kind of data—such as the number of steps taken or the distance covered—at the same time."

and, critically scoping it:

> "Only the Activity and Sleep data types are deduped by Health Connect, and the data totals shown are the values after the dedupe has been performed by the Aggregate API."
>
> "For other types of data, the aggregated results combine all data of the type in Health Connect from all apps which wrote the data."

Distance is an **Activity** data type ([data types](https://developer.android.com/health-and-fitness/health-connect/data-types)), so it *is* in the deduped set. Good — the test is meaningful.

Two things to hold onto while testing:

- **Only the end user can reorder priority — there is no API to force it.** "Only end users can alter these priority lists" ([Read aggregated data](https://developer.android.com/health-and-fitness/health-connect/aggregate-data)). The reordering has to be done by hand, and the exact path is ([Manage your Health Connect data](https://support.google.com/android/answer/12990553?hl=en)):

  > 1. Open Health Connect. 2. Under "Permissions and data," tap **Manage Data**. 3. Tap **Data sources and priority**. 4. Under "App sources," touch and hold next to the app name, then drag the Handle.

  So the test loop is: record → read aggregate → drag to reorder → read aggregate again → confirm the number flipped.
- **Dedup is on overlapping *time*, and OpenTracks writes one long interval spanning the whole walk.** If the phone's native step-derived data and OpenTracks' distance cover the same window, that is the overlap condition. But note the native recorder writes *steps*, not distance — so unless a second distance writer is present, there may be nothing to dedupe against and the aggregate will simply equal the OpenTracks figure. **To exercise dedup properly you need two apps both writing `DistanceRecord`.** That is what Health Connect Toolbox (§5) is for — it is a cheaper and more controllable second writer than Strava.

---

## 7. Test protocol

Ordered so that each step fails loudly rather than silently.

**Install (no account, no Play Store):**

```bash
curl -O https://fdroid.storchp.de/fdroid/repo/de.dennisguse.opentracks.playstore_v4.28.0-33-ga542f4566_6698-nightly-release.apk
adb install de.dennisguse.opentracks.playstore_v4.28.0-33-ga542f4566_6698-nightly-release.apk
adb shell dumpsys package de.dennisguse.opentracks.nightly | grep -i WRITE_DISTANCE   # sanity check
```

(Or add the nightly repo to an F-Droid client and let it track updates. Either is fine; the direct APK is fewer moving parts.)

**Configure — this is the step that gets skipped:**

1. OpenTracks → Settings → **Import and Export** → **Export to Health Connect**.
2. Tap **"Health Connect settings"** and grant the write permissions. Health Connect must be granted `WRITE_DISTANCE` specifically — if only Exercise appears in the grant sheet, you are on the wrong build; stop and check the version.
3. Turn **"Instant post-workout export"** ON. It is off by default (§2.7).
4. Grant location permission, and set it to **Precise** and **While using the app** at minimum.

**Record:** walk outdoors for 5–10 minutes with clear sky view. Straighter and longer is better — GPS noise inflates distance more on short, twisty routes, which would bias the stride figure. Stop and **save** the track (the export fires from `TrackStoppedActivity`, i.e. on save, not on pause).

**Verify — the specific check:** in the probe app, read `DistanceRecord` and inspect `metadata.dataOrigin.packageName` across the returned records. Success is a new package appearing:

```
de.dennisguse.opentracks.nightly
```

Export is enqueued through `WorkManager`, so allow a short delay; it is not synchronous with the save. If nothing appears after a few minutes, use **Settings → Import and Export → "Export all"** to force it, and check `adb logcat -s HealthConnectWorker`.

**Then calibrate — but check for contamination first.** Before dividing, confirm that `de.dennisguse.opentracks.nightly` is the *only* package writing `DistanceRecord` over that window. If Fitbit / Google Health is also present, the aggregate may be its step-derived figure instead (§4.3) and the result is worthless. Once clean: read `aggregate(StepsRecord.COUNT_TOTAL)` and `aggregate(DistanceRecord.DISTANCE_TOTAL)` over the walk's exact time window. Stride = distance ÷ steps. The steps come from Health Connect's native recorder; the distance comes from OpenTracks' GPS. These are genuinely independent measurements — that independence is the whole point (§2.3).

**Then test dedup:**

```bash
curl -L -o hct.zip https://goo.gle/health-connect-toolbox
unzip hct.zip && adb install HealthConnectToolbox-2_3_3.apk
```

Insert a `DistanceRecord` deliberately overlapping the walk with an obviously wrong value (Insert Health Record → Activity → Distance). Confirm `aggregate(DISTANCE_TOTAL)` returns one value or the other, never the sum. Then reorder the Activity priority list by hand (§6) and confirm the aggregate flips. You should see two packages as distance sources: `de.dennisguse.opentracks.nightly` and `androidx.health.connect.client.devtool`.

---

## Open questions / unverified

- **Nightly build stability is untested by anyone, including upstream.** `v4.28.0-33` is 33 commits past the last tag and contains two same-day bugfixes to the Health Connect path (2026-08-05). It is plausible that the export path is still in flux. Mitigation: if the record does not appear, use the manual "export all" button in settings before concluding anything, and check `adb logcat` for the worker's `TAG` (`HealthConnectWorker`).
- **No release date for v4.29.0 is published.** The distance commit has been on `main` since 2026-07-31 with no tag. Whether it ships in days or months is unknown. Re-check [releases](https://codeberg.org/OpenTracksApp/OpenTracks/releases) before assuming the stable channel is still unsuitable.
- **The exact GPS filtering/smoothing OpenTracks applies before accumulating distance was not audited.** There is a "recording distance interval" preference visible in `TrackStatisticsUpdater`, and OpenTracks applies accuracy thresholds. This affects the *absolute accuracy* of the calibration figure — GPS noise typically inflates measured distance on a walk — but not the *validity* of the method. If the derived stride length looks implausibly long, suspect GPS noise inflating the numerator, and re-run on a longer, straighter route with clear sky view.
- **Whether F-Droid nightly APKs verify against a reproducible build was not checked.** The main F-Droid entry `de.dennisguse.opentracks.playstore` is flagged as a reproducible build and `de.dennisguse.opentracks` as non-reproducible; the third-party nightly repo at `fdroid.storchp.de` makes no such claim. Installing it means trusting that repo's signing key. Acceptable for a throwaway test instrument on a dev device; note it and do not generalise.
- **No closed-source app could be verified at the manifest level.** Every APK-analysis mirror (AppBrain, APKPure, APKCombo, APKMonk) and the Wayback Machine blocked automated access with 403/CAPTCHA/DNS failures. So Strava, MapMyWalk, adidas Running and Nike Run Club rest entirely on vendor prose. The only way to close this is empirical: install, connect, and read the Health Connect permission sheet, which lists the exact data types requested. That is a five-minute check on-device and is worth doing before trusting any of them.
- **Samsung Health, Garmin Connect, Polar, Sports Tracker and Pacer were not resolved** (§4.4). In particular: whether Samsung Health functions at all on a non-Samsung device, whether Garmin Connect can record a phone-only GPS activity with no Garmin hardware, and whether Pacer's distance is step-derived (strongly suspected, but the help page 403'd on direct fetch). None of these affects the recommendation, but do not treat their absence from the "confirmed" list as a negative finding.
- **Fitbit MobileRun is unresolved.** Fitbit's GPS-based run mode may write a genuinely GPS-derived `DistanceRecord`, which would make it a valid instrument — but no first-party source was found establishing that, nor whether a MobileRun record is distinguishable from a step-derived MobileTrack record once both are in Health Connect. Treat Fitbit as contaminating until proven otherwise.
- **Health Connect Toolbox is dated.** The shipped APK is `2.3.3`, built 2025-12-03, `targetSdkVersion 33`. It was not tested on Android 16. If it misbehaves on a Pixel 9 Pro, the fallback for a second distance writer is Strava (§4.1), at the cost of an account.
- **Health Connect's behaviour when one app writes `DistanceRecord` and another writes only `StepsRecord` over the same window is not documented.** They are different data types, so no dedup interaction should occur, but this was not verified against source. Relevant because it is exactly our situation before a second distance writer is added.

---

## Sources

All accessed 2026-08-06.

**Google / primary platform documentation**
- [Health Connect data types](https://developer.android.com/health-and-fitness/health-connect/data-types) — `DistanceRecord`, mandatory fields, `READ_DISTANCE`/`WRITE_DISTANCE`
- [Read aggregated data](https://developer.android.com/health-and-fitness/health-connect/aggregate-data) — dedup by priority, Activity/Sleep-only scope
- [Track your steps with Health Connect](https://support.google.com/android/answer/16786157?hl=en) — native on-device recording is steps only
- [Manage your Health Connect data](https://support.google.com/android/answer/12990553?hl=en) — only end users can change priority order
- [Connect an app to Health Connect](https://support.google.com/android/answer/12201227) — no compatible-app list present
- [health.google/health-connect-android/](https://health.google/health-connect-android/) — no compatible-app list present

**OpenTracks (source of truth: local clone + F-Droid index, 2026-08-06)**
- [Repository](https://codeberg.org/OpenTracksApp/OpenTracks) / [Releases](https://codeberg.org/OpenTracksApp/OpenTracks/releases)
- [`AndroidManifest.xml` (main)](https://codeberg.org/OpenTracksApp/OpenTracks/raw/branch/main/src/main/AndroidManifest.xml)
- [`HealthConnectWorker.java`](https://codeberg.org/OpenTracksApp/OpenTracks/raw/branch/main/src/main/java/de/dennisguse/opentracks/io/healthconnect/exporter/HealthConnectWorker.java)
- [`HealthConnectUtils.java`](https://codeberg.org/OpenTracksApp/OpenTracks/raw/branch/main/src/main/java/de/dennisguse/opentracks/io/healthconnect/exporter/HealthConnectUtils.java)
- [`TrackStatisticsUpdater.java`](https://codeberg.org/OpenTracksApp/OpenTracks/raw/branch/main/src/main/java/de/dennisguse/opentracks/data/statistics/TrackStatisticsUpdater.java)
- Nightly F-Droid repo index: `https://fdroid.storchp.de/fdroid/repo/index.xml` (repo: `https://fdroid.storchp.de/fdroid/repo?fingerprint=99985A7E73DCB0B16C9BDDCE7A0B4996F88068AE7C771ED53E217E69CD1FF196`)
- [F-Droid: `de.dennisguse.opentracks.playstore`](https://f-droid.org/en/packages/de.dennisguse.opentracks.playstore/) / [`de.dennisguse.opentracks`](https://f-droid.org/en/packages/de.dennisguse.opentracks/)
- [Google Play: `de.dennisguse.opentracks.playstore`](https://play.google.com/store/apps/details?id=de.dennisguse.opentracks.playstore)

**Health Connect Toolbox**
- [Test your integration with the Health Connect Toolbox](https://developer.android.com/health-and-fitness/health-connect/test/health-connect-toolbox) — writes all data types
- Download: `https://goo.gle/health-connect-toolbox` → `https://www.gstatic.com/health-ecosystems/health_connect_toolbox.zip` (`HealthConnectToolbox-2_3_3.apk`, inspected with `aapt2`, declares `WRITE_DISTANCE`)

**Commercial apps**
- Play Store curated collection: [Works with Health Connect](https://play.google.com/store/apps/collection/promotion_all__health_connect) — names apps only, no data types
- Strava: [Health Connect and Strava](https://support.strava.com/en-us/articles/15401554-health-connect-and-strava); [Syncing Strava Activities with Health Connect](https://support.strava.com/hc/en-us/articles/43440435267597-Syncing-Strava-Activities-with-Health-Connect); [Is Strava free?](https://support.strava.com/hc/en-us/articles/216917627-Is-Strava-free); [Google Health and Strava](https://support.strava.com/en-us/articles/15401544-google-health-and-strava)
- Under Armour: [Google Health Connect](https://help.mapmyfitness.com/hc/en-us/articles/42203015991575-Google-Health-Connect); [Samsung Health Integration](https://help.mapmyfitness.com/hc/en-us/articles/36780316319767-Samsung-Health-Integration)
- Nike: [Connect NRC to partner apps and devices](https://www.nike.com/help/a/connect-nrc-partner-apps-devices); [Apple Health (iOS only)](https://www.nike.com/help/a/connect-nrc-health-app)
- komoot: [Connected Devices and Integrations](https://support.komoot.com/hc/en-us/categories/10073828995482-Connected-Devices-and-Integrations); [Sync Apple Health with komoot](https://support.komoot.com/hc/en-us/articles/10449275638170-Sync-Apple-Health-with-komoot); [2026 feature releases](https://support.komoot.com/hc/en-us/articles/10621431252250)
- adidas Running: [movezengo](https://support.movezengo.com/article/157-adidas-running-runtastic); [Personify Health](https://personifyhealth.zendesk.com/hc/en-us/articles/28015903036315-How-to-connect-Adidas-Running-to-Google-Fit) — third-party only, `help.runtastic.com` decommissioned
- Fitbit / Google Health: [How do I use Health Connect in the Fitbit app?](https://support.google.com/fitbit/answer/14506680?hl=en-AU) — explicit read/write data-type table, Distance in both; [Track steps with your phone](https://support.google.com/fitbit/answer/14236404) — "Your distance is estimated by your stride length… Your phone's location doesn't affect your step count"
- Google Fit API wind-down: [developers.google.com/fit](https://developers.google.com/fit)
- Pacer (⚠️ unverified, 403 on direct fetch): [Steps Count but Distance Does Not](https://support.mypacer.com/hc/en-us/articles/42318897381005-Steps-Count-but-Distance-Does-Not-Explanation-and-Fixes)

**Negatives**
- [FitoTrack](https://codeberg.org/jannis/FitoTrack) — cloned, zero Health Connect references
- [RunnerUp](https://github.com/jonasoreland/runnerup) — cloned, zero Health Connect references
- [komoot](https://support.komoot.com/) — help-centre search returns zero Health Connect articles
