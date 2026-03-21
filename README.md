# Tabidachi Android

Native Android companion app for [Tabidachi](https://github.com/xblackbytesx/tabidachi) — a self-hosted travel itinerary manager.

Built with Jetpack Compose and Material 3. Designed to render your travel itineraries beautifully on mobile. Offline-first so all data is cached locally after the first sync, so your itineraries are always available, even on a plane or in a foreign subway. At this point the app is read-only so modifications are to be done at your web-based instance and are then synched to your mobile app. In the future I'll be looking at a two-way sync.

## Features

- **Full itinerary timeline** — legs, days, activities, transit, accommodation, rendered as a rich vertical timeline
- **Offline-first** — Room database caches all trip data and detail JSON; works fully offline after first sync
- **Cover images** — trip and leg hero images with gradient overlays, loaded via Coil with disk caching
- **Transit cards** — transport mode icons (flight, train, bus, ferry, etc.), route visualization with dashed lines, carrier and flight number display
- **Event images** — thumbnails on activity cards with tap-to-open fullscreen lightbox (pinch-to-zoom)
- **Day navigation** — bottom carousel with numbered day tiles; tap to jump, auto-highlights today and current scroll position
- **Default trip** — skip the dashboard and open directly to a specific trip
- **Pull-to-refresh** — syncs fresh data from the API; shows "last synced" indicator when stale
- **PIN lock** — optional 4-digit PIN with PBKDF2 hashing (100K iterations) and exponential backoff on failed attempts
- **Biometric unlock** — fingerprint or face unlock via AndroidX BiometricPrompt
- **Auto-lock** — configurable timeout (30s / 1m / 2m / 5m) using ProcessLifecycleOwner
- **OLED dark theme** — pure black backgrounds for battery savings on AMOLED screens
- **Immersive mode** — status bar hidden for maximum screen real estate
- **FLAG_SECURE** — prevents screenshots and app switcher preview when PIN is enabled

## Screenshots

<table>
  <tr>
    <td><img src="docs/screenshots/01%20-%20Trips%20-%20Tabidachi.png" width="200" alt="Trip dashboard (OLED)"></td>
    <td><img src="docs/screenshots/02%20-%20Japan%20Itinerary%20-%20Tabidachi.png" width="200" alt="Itinerary hero header"></td>
    <td><img src="docs/screenshots/03%20-%20Japan%20Itinerary%20-%20Tabidachi.png" width="200" alt="Day view with events"></td>
    <td><img src="docs/screenshots/04%20-%20Japan%20Itinerary%20-%20Tabidachi.png" width="200" alt="Transit cards"></td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/07%20-%20Trips%20-%20non-oled%20-%20Tabidachi.png" width="200" alt="Trip dashboard (dark)"></td>
    <td><img src="docs/screenshots/08%20-%20Japan%20Itinerary%20-%20non-oled%20-%20Tabidachi.png" width="200" alt="Itinerary hero (dark)"></td>
    <td><img src="docs/screenshots/09%20-%20Japan%20Itinerary%20-%20non-oled%20-%20Tabidachi.png" width="200" alt="Day view (dark)"></td>
    <td><img src="docs/screenshots/10%20-%20Japan%20Itinerary%20-%20non-oled%20-%20Tabidachi.png" width="200" alt="Transit cards (dark)"></td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/05%20-%20Japan%20Itinerary%20-%20Tabidachi.png" width="200" alt="Accommodation banner"></td>
    <td><img src="docs/screenshots/06%20-%20Japan%20Itinerary%20-%20Tabidachi.png" width="200" alt="Itinerary detail"></td>
    <td><img src="docs/screenshots/11%20-%20Settings%20-%20Tabidachi.png" width="200" alt="Settings"></td>
    <td></td>
  </tr>
</table>

[See all screenshots →](docs/screenshots/)

## Requirements

- A running [Tabidachi](https://github.com/xblackbytesx/tabidachi) instance with the REST API enabled
- A Personal Access Token (PAT) generated from the Tabidachi web UI
- Docker and Docker Compose (for building)

## Build

The project uses a fully Dockerized build pipeline — no local Android SDK or Gradle installation needed.

### Debug build

```sh
make build
```

Produces `output/tabidachi-debug.apk`.

### Signed release build

1. Copy the environment file and fill in your signing credentials:

```sh
cp .env.example .env
```

2. Generate a keystore (one-time):

```sh
make keystore
```

3. Build and sign:

```sh
make all
```

Produces `output/tabidachi-signed.apk`.

### Other targets

| Command | Description |
|---------|-------------|
| `make build` | Debug APK |
| `make build-release` | Unsigned release APK |
| `make keystore` | Generate signing keystore |
| `make sign` | Sign an unsigned release APK |
| `make all` | Release build + sign |
| `make install` | Install debug APK via ADB |
| `make clean` | Remove build outputs |

### CI/CD (GitHub Actions)

Pushing a tag triggers an automated build that produces a signed APK attached to a GitHub Release.

**One-time setup** — add these as repository secrets (`Settings > Secrets and variables > Actions`):

| Secret | Value |
|--------|-------|
| `KEYSTORE_BASE64` | Base64-encoded keystore: `base64 -w 0 keystore/tabidachi.jks` |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias (e.g. `tabidachi`) |
| `KEY_PASSWORD` | Key password |

**Creating a release:**

```sh
git tag v1.0.0
git push origin v1.0.0
```

The workflow builds the release APK, signs it with your keystore, and publishes it as a GitHub Release with auto-generated release notes.

## Setup

1. Install the APK on your device
2. Enter your Tabidachi server URL (e.g. `https://trips.example.com`)
3. Enter your Personal Access Token
4. Optionally set a PIN and enable biometric unlock
5. Your trips sync automatically

## Tech Stack

| Component | Version |
|-----------|---------|
| Kotlin | 2.1.10 |
| AGP | 8.7.3 |
| Jetpack Compose | BOM 2025.01.01 |
| Material 3 | via Compose BOM |
| Compose Navigation | Type-safe routes with kotlinx.serialization |
| Ktor Client | 3.1.1 (Android engine) |
| Coil | 3.1.0 (Compose + Ktor network) |
| Room | 2.7.0 + KSP |
| BiometricPrompt | 1.1.0 |
| Target SDK | 35 |
| Min SDK | 26 (Android 8.0) |

## Project Structure

```
tabidachi-android/
├── docker/
│   ├── Dockerfile              # Android SDK + Gradle build image
│   └── docker-compose.yml      # Builder and signer services
├── scripts/
│   ├── keystore.sh             # Keystore generation script
│   └── sign.sh                 # APK signing script
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── res/                    # Icons, strings, network security config
│   └── java/com/example/tabidachi/
│       ├── TabidachiApp.kt     # Application: auth state, auto-lock, Coil
│       ├── MainActivity.kt     # Single Activity, immersive mode
│       ├── auth/               # PIN (PBKDF2), biometric, auth manager
│       ├── data/               # Room DB, DAO, repository, secure storage
│       ├── navigation/         # Type-safe routes, NavHost with auth guards
│       ├── network/            # Ktor API client, serializable models
│       └── ui/
│           ├── components/     # Reusable composables (cards, badges, lightbox)
│           ├── dashboard/      # Trip list with pull-to-refresh
│           ├── lock/           # PIN entry + biometric prompt
│           ├── settings/       # Theme, security, default trip, disconnect
│           ├── setup/          # Server URL + PAT onboarding, PIN enrollment
│           ├── theme/          # Dark + OLED color palettes, typography
│           └── trip/           # Full itinerary timeline with day navigation
├── Makefile                    # Build pipeline entry point
├── .env.example                # Signing credential template
├── build.gradle.kts            # Root Gradle config
├── settings.gradle.kts
└── gradle.properties
```

## API

The app communicates with the Tabidachi server via its REST API:

| Endpoint | Description |
|----------|-------------|
| `GET /api/v1/trips` | List all trips (summaries) |
| `GET /api/v1/trips/:id` | Full trip detail with nested legs/days/events |

All requests use `Authorization: Bearer <pat>` authentication. Image URLs are fully absolute as returned by the server.

## Architecture

```
UI (Compose) ← StateFlow ← ViewModel ← TripRepository
                                              ↓
                              Room DB ← API (Ktor) → Tabidachi Server
```

- **Offline-first**: UI observes Room Flows. Repository syncs from API in background and writes to Room.
- **Single source of truth**: Room database. API responses are cached as entities; trip detail is stored as a JSON blob column.
- **Image caching**: Coil handles disk caching automatically. Images viewed once are available offline.

## License

This project is licensed under the GNU General Public License v2.0 — see the [LICENSE](LICENSE) file for details.
