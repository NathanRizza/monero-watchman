# Monero Watchman

An Android app that watches a Monero node and notifies you when the chain reorganizes.

Commissioned by the Monero community:
https://bounties.monero.social/posts/203/0-429m-a-monero-attack-alert-app

## How it works

Once you hit **Launch**, a foreground service polls your node once a minute over
JSON-RPC:

1. `get_info` gives the current chain height.
2. `get_block_headers_range` pulls the last `threshold + 10` block headers, which
   are kept as the baseline.
3. On the next poll the same range is fetched again and compared to the baseline
   hash by hash.
4. If the number of differing blocks reaches your reorg threshold, you get a
   notification with the reorg length and the fork point.

The service keeps running with the app closed. If it can't reach the node on the
first poll it sends a "Failed to connect to server" notification and stops, so
you always know whether monitoring is actually running.

## Features

* Notification on reorg, and on failure to connect to the node
* User-defined reorg threshold (1–50 blocks)
* Any node over HTTP or HTTPS
* Optional SOCKS proxy — point it at Tor (`127.0.0.1:9050`) for onion nodes
* Start on Boot, so monitoring resumes after a restart
* Settings persist across app closes and reboots
* "Default Values" button to get back to sane settings
* Prompts for the battery optimization exemption it needs to keep polling

## Settings

| Setting | Default | Notes |
| --- | --- | --- |
| Node Address | `https://xmrnode.shork.ch` | No trailing slash; `/json_rpc` is appended |
| Reorg Threshold | 3 | Blocks that must differ before you're alerted; capped at 50 |
| Proxy Address | `127.0.0.1:9050` | `host:port`, SOCKS |
| Proxy | off | |
| Start on Boot | off | Asked once on first launch |

Check interval is fixed at 1 minute.

Settings are saved when you press **Launch** or **Default Values** — changing a
field alone doesn't save it or restart the service. Press **Launch** again after
any change.

## Building

Requires JDK 17. The Gradle wrapper is included, so Android Studio isn't needed.

Debug APK:

```sh
./gradlew assembleDebug
# app/build/outputs/apk/debug/app-debug.apk
```

Signed release APK:

```sh
./build_signed_apk.sh
# app/build/outputs/apk/release/app-release.apk
```

The script prompts for your JDK path, keystore path, and passwords. It detects
the key alias itself, injects signing into the build, and writes nothing to
disk. If you don't have a keystore yet:

```sh
keytool -genkeypair -v -keystore ~/keys/monero-watchman-release.jks \
    -alias monero-watchman -keyalg RSA -keysize 2048 -validity 10000
```

CI builds a debug APK on every push and PR to `main` and uploads it as an
artifact.

## Layout

```
app/src/main/java/com/example/monerowatchman/
├── MainActivity.kt                # Compose UI, settings, service launch
├── services/
│   ├── ReorgCheckService.kt       # Polling loop, reorg detection, notifications
│   └── BootReceiver.kt            # Restarts the service after a reboot
├── utils/
│   ├── NetworkUtilities.kt        # OkHttp JSON-RPC client, proxy support
│   └── JsonUtilities.kt           # JSON field lookup, block header parsing
└── data/BlockDataEntry.kt         # height + hash
```

minSdk 24, targetSdk 36. Kotlin, Jetpack Compose, OkHttp.

## Support

Issues and feature requests: https://github.com/NathanRizza/monero-watchman

Monero donations:

```
86rBr8eqGFbLNgR9VTm6XbdPBFc5hGqMrGjQh1Pv8UVuQRd5oTMRYZHUdQqpJDRRukc3R2EcTWTHq1cjVGiLdSm9EdtVFTu
```

## License

GPLv3. See [LICENSE](LICENSE).
