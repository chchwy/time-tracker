# Time Tracker（時間追蹤器）

A modern Android time tracking app built with **Jetpack Compose** and **Kotlin**. Designed to go beyond simple stopwatch logging — track categories, projects (via tags), sleep quality, and get honest analytics that account for unrecorded time.

## Features

### Time Tracking

- **Manual & Timer modes** — log past activities or start a live timer
- **Rich metadata** — Category, Title, Notes, and Tags on every record
- **Custom Categories** — create and edit categories with Material Design color codes
- **Tags as projects** — use tags to track sub-categories or project-level time (e.g. a "Work" category with a "Fruitesse" tag)

### Sleep Tracking

Sleep is treated as a first-class record type, not just another activity:

- **Pre-sleep habits** — toggles for Read Book, Used Computer, Chatted with Partner
- **Interruptions** — log child interruptions or reasons for staying up late
- **Morning energy index** — rate your energy (1–10) the next morning
- **Reading log** — automatically tracks which books you read before bed

### Analytics

The analytics page gives an honest picture of how your time is spent:

- **Category distribution** — animated donut chart showing each category's share of elapsed time (not just tracked time)
- **Unknown time block** — unrecorded time is explicitly shown as "Unknown" so categories never falsely add up to 100%
- **Overnight sleep handling** — sleep that starts before midnight is correctly split across both days
- **Future time excluded** — today's denominator is only the hours that have already passed, not the full 24h
- **Tag cumulative duration** — total time per tag across the period, useful for project-level tracking
- **Recording rate** — shows what percentage of elapsed time you actually logged, with a progress bar
- **Sleep analysis** — average duration, morning energy, and bedtime habit rates
- **Day / Week / Month views** with navigation on both the Records and Analytics pages

### Data

- **Local first** — all data lives on-device via Room (SQLite)
- **CSV export & import** — one-tap export with UTF-8 BOM for Excel compatibility
- **Multilingual** — English and Traditional Chinese (zh-rTW)

## Tech Stack

| Layer | Library |
|---|---|
| UI | Jetpack Compose (Material 3) |
| Language | Kotlin |
| Architecture | MVVM + Clean Architecture |
| Database | Room (SQLite) |
| DI | Dagger Hilt |
| Async | Coroutines + Flow |

## Getting Started

### Prerequisites

- Android Studio (Koala or newer)
- Min SDK 24 / Target SDK 34

### Installation

1. Clone the repo: `git clone https://github.com/mchang/matt-timer-trcker.git`
2. Open in Android Studio
3. Let Gradle sync, then click **Run ▶** to deploy to emulator or device

## Roadmap

- [x] Manual & timer-based time records
- [x] Custom categories with color codes
- [x] Tags for project/sub-category tracking
- [x] Sleep tracking with habit metadata and morning energy
- [x] Donut chart analytics with honest unknown-time accounting
- [x] Tag cumulative duration analytics
- [x] Recording rate indicator
- [x] Day / Week / Month views on Records and Analytics
- [x] CSV export & import
- [x] Traditional Chinese localization
