# Time Tracker (時間追蹤器)

A modern, highly customizable Android Time Tracking application built purely with **Jetpack Compose** and **Kotlin**. Designed to move past traditional "accounting-style" time tracking, this app focuses on **energy management, sleep quality context, and true productivity analytics.**

## 🌟 Key Features

### 1. ⏱️ Time Tracking
* **Manual & Timer Modes:** Seamlessly start a live timer or log past activities.
* **Rich Metadata:** Add Categories, Titles, free-text Notes, and Tags to any activity.
* **Custom Categories:** Fully management system to create and edit your own categories with personalized Material Design color codes.

### 2. 🛏️ Context-Aware Sleep Tracking
Traditional time trackers just log "8 hours of sleep." This app treats sleep as the foundation of your productivity by tracking:
* **Pre-sleep Habits:** Toggles for "Read Book," "Used Computer," or "Chatted with Partner". 
* **Interruptions:** Log if you were woken up by a child or state your "Reason for staying up late."
* **Morning Energy:** Rate your energy index (1-10) the next morning to map habits to outcomes.

### 3. 📊 Advanced Analytics & Stacked Bar Charts
* **Hourly & Daily Distribution:** Visualize your time via a dynamic **Stacked Bar Chart**. Avoid the trap of "24-hour total logging"—instead, see exactly which *colors (categories)* dominated your morning, afternoon, or week.
* **Dynamic Insights:** Check your weekly average sleep duration, reading streak, and correlation between pre-bed habits and morning energy.

### 4. 🗄️ Full Data Ownership 
* **Local First:** All your data lives locally on your device via the Android Room Database.
* **CSV Export:** One-tap export of your entire database layout to a `.csv` file. Built-in UTF-8 BOM encoding ensures flawless opening in Microsoft Excel without Chinese character garbling.

## 🛠️ Tech Stack & Architecture
* **UI Toolkit:** Jetpack Compose (Material 3)
* **Language:** Kotlin
* **Architecture:** MVVM (Model-View-ViewModel) + Clean Architecture approach
* **Database:** Room (SQLite) for robust local persistence 
* **Dependency Injection:** Dagger Hilt
* **Coroutines & Flow:** Completely reactive UI streams from DB to View

## 📦 Getting Started

### Prerequisites
* Android Studio (Koala or newer)
* Minimum SDK 24 / Target SDK 34

### Installation
1. Clone the repository: `git clone https://github.com/mchang/matt-timer-trcker.git`
2. Open the project in **Android Studio**.
3. Let Gradle sync and then click **Run (▶)** to deploy the app to your emulator or physical Android device.

## 📝 Roadmap
* [x] Basic CRUD for Time Records
* [x] Sleep Tracking & Metadata
* [x] Analytics Engine & Visualizations (Stacked Bar Charts)
* [x] Category & UI Settings
* [x] CSV Data Export
* [ ] Home Screen Widgets for 1-tap logging
* [ ] Android UsageStats Integration for passive screen-time tracking
