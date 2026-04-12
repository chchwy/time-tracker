# Time Tracker App — 開發計畫

靈感來自彼得杜拉克《有效的主管》第一章：高效能人士知道自己的時間用去哪了。
像記帳軟體一樣，手動輸入或計時追蹤每一筆時間紀錄，並搭配分析頁面洞察時間的分佈。

---

## Tech Stack

| Component | 選擇 |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Repository pattern |
| Database | Room |
| DI | Hilt (KSP) |
| Navigation | Navigation Compose |
| Charts | Vico (Phase 5) |
| Min SDK | 26 (Android 8.0) |
| i18n | 繁體中文 + English |

---

## 資料庫結構

**單一 `time_records` 表**（睡眠紀錄用 nullable 欄位區分）

### `time_records`
| 欄位 | 型別 | 說明 |
|---|---|---|
| id | Long PK | 自動產生 |
| type | String | `NORMAL` 或 `SLEEP` |
| categoryId | Long? FK | 參照 categories.id |
| title | String? | 選填標題 |
| startTime | Long | Epoch millis |
| endTime | Long | Epoch millis |
| durationMinutes | Int | 方便查詢，預先計算儲存 |
| note | String? | 備註 |
| createdAt / updatedAt | Long | |
| **睡眠專用（nullable）** | | |
| childInterrupted | Boolean? | 半夜小孩干擾 |
| usedComputerBeforeBed | Boolean? | 睡前開電腦 |
| readBookBeforeBed | Boolean? | 睡前讀書 |
| chattedWithWife | Boolean? | 跟老婆聊天 |
| stayUpLateReason | String? | 熬夜原因 |
| morningEnergyIndex | Int? | 起床能量指數 1–10 |

### `categories`
預設 12 個分類：工作、學習、運動、休息、通勤、家務、娛樂、社交、閱讀、睡眠、陪伴家人、其他

### `tags` + `record_tag_cross_ref`
每筆紀錄可選一個分類、多個標籤（M:N 關聯）

---

## 畫面架構

底部導覽列 5 個 Tab：

| Tab | Route | 說明 |
|---|---|---|
| 紀錄 | `records` | 所有時間紀錄，依日期分組 |
| 計時 | `timer` | 碼表模式，開始/停止自動建立紀錄 |
| 新增 | `add_record` | 手動輸入表單 |
| 睡眠 | `sleep` | 睡眠紀錄專用表單 + 歷史 |
| 分析 | `analytics` | 圓餅圖、長條圖，日/週/月切換 |

---

## Phase 1：專案骨架 + 資料庫 ✅

**目標**：App 能啟動，底部導覽正常，資料庫初始化並植入預設分類。

### 完成項目
- Gradle Kotlin DSL + Version Catalog（AGP 8.5.2、Kotlin 2.0.21）
- Gradle wrapper 8.9
- `local.properties` SDK 路徑設定
- **Room 實體**：`TimeRecordEntity`、`CategoryEntity`、`TagEntity`、`RecordTagCrossRef`、`RecordWithTags`
- **DAO**：`TimeRecordDao`（含分析彙總查詢）、`CategoryDao`、`TagDao`
- **AppDatabase**：`onCreate` callback 自動植入 12 個預設分類
- **Domain 層**：`TimeRecord`、`Category`、`Tag`、`RecordType` 模型
- **Repository 介面與實作**：`TimeRecordRepositoryImpl`、`CategoryRepositoryImpl`、`TagRepositoryImpl`
- **Hilt DI 模組**：`DatabaseModule`、`RepositoryModule`
- **UI**：Material 3 主題、底部導覽、5 個 Tab stub 畫面
- **多語系**：`values/strings.xml`（英文）、`values-zh-rTW/strings.xml`（繁體中文）
- **Launcher icon**：Adaptive icon（向量圖）

### 驗證
```
BUILD SUCCESSFUL in 1m 43s
```

---

## Phase 2：手動新增紀錄 + 紀錄清單 ✅

**目標**：使用者能手動新增時間紀錄，在清單中查看、編輯、刪除。

### 完成項目

#### 共用元件
- `CategoryChip` / `CategorySelector`：帶顏色圓點的分類選擇 Chip（FlowRow）
- `TagSelector`：多選標籤 Chip，含「新增標籤」Dialog
- `DateTimeField`：日期選擇器 + 時間選擇器（Material 3 DatePickerDialog + TimePicker）
- `DurationText`：將分鐘數格式化為「Xh Ym」
- `TimeRecordCard`：顯示分類顏色、時間範圍、時長、標籤、睡眠圖示

#### 新增紀錄畫面（`AddRecordScreen`）
- 欄位：標題、分類選擇、開始/結束時間、自動計算時長、標籤多選、備註
- 驗證：必須選分類、結束時間不可早於開始時間
- 儲存後返回紀錄清單

#### 紀錄清單畫面（`RecordListScreen`）
- 依日期分組，日期標題
- 左滑刪除（SwipeToDismissBox）
- 點擊紀錄進入編輯模式（帶 `recordId` 參數導航至 `AddRecordScreen`）
- 空狀態提示

#### 導覽更新
- 新增 `edit_record/{recordId}` 路由
- AddRecord 儲存後回到 Records tab

### 驗證
```
BUILD SUCCESSFUL in 53s
```

---

## Phase 3：碼表計時器（待實作）

**目標**：使用者可以按「開始」計時，按「停止」自動建立一筆時間紀錄。

### 計畫項目

1. **`TimerScreen`**
   - 大型數字計時顯示（HH:MM:SS）
   - 分類選擇器（橫向捲動 Chips）
   - 標籤多選
   - 大按鈕：開始 / 停止

2. **`TimerViewModel`**
   - 用 Coroutine 每秒更新計時器 `StateFlow`
   - 記錄 `startTime = System.currentTimeMillis()`
   - 停止時計算 duration，呼叫 `AddRecordUseCase` 建立紀錄
   - 用 `SavedStateHandle` 保存計時狀態，避免螢幕旋轉遺失

3. **停止後確認流程**
   - 停止計時 → 顯示確認 Snackbar（「已建立 X 分鐘的紀錄」）
   - 可選擇跳到紀錄編輯頁補充分類/標籤

4. **（進階）前景服務**
   - `TimerForegroundService`：App 切換到背景後計時繼續
   - 通知列顯示當前計時時間

---

## Phase 4：睡眠追蹤（待實作）

**目標**：專屬睡眠紀錄表單，追蹤睡眠品質與睡前習慣。

### 計畫項目

1. **`SleepScreen`**
   - 入睡時間、起床時間選擇器
   - Toggle 開關：半夜小孩干擾、睡前開電腦、睡前讀書、跟老婆聊天
   - 文字輸入：熬夜原因（選填）
   - Slider：起床能量指數 1–10
   - 儲存後自動設定 `type = SLEEP`、`categoryId` = 睡眠分類

2. **睡眠歷史**
   - 最近睡眠紀錄列表，顯示睡眠時長 + 習慣指標 icon
   - 在紀錄清單中有月亮 icon 區別睡眠紀錄

3. **快速洞察**
   - 本週平均睡眠時長
   - 平均起床能量
   - 連續讀書天數 streak

---

## Phase 5：分析（待實作）

**目標**：圖表化呈現時間分佈，支援日/週/月切換。

### 計畫項目

1. 加入 **Vico** 圖表函式庫（Compose-native）

2. **`AnalyticsScreen`**
   - 期間選擇器：日 / 週 / 月（SegmentedButton）
   - 左右箭頭切換日期範圍
   - **圓餅圖**：各分類時間佔比
   - **長條圖**：每日時間總量
   - 彙總卡片：已追蹤總時數、日均時數

3. **睡眠分析區塊**
   - 睡眠時長趨勢折線圖
   - 平均起床能量指數
   - 習慣達成率：讀書 %、不開電腦 %、聊天 %
   - 習慣與能量的相關性提示（例：讀書的夜晚平均能量 7.2 vs 未讀書 5.8）

4. **`AnalyticsViewModel`**
   - 彙總查詢：`getCategorySummary(from, to)`
   - 支援 Day / Week / Month 三種時間範圍

---

## 專案結構

```
app/src/main/java/com/mattchang/timetracker/
├── TimeTrackerApp.kt
├── MainActivity.kt
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt
│   │   ├── converter/Converters.kt
│   │   ├── dao/  (TimeRecordDao, CategoryDao, TagDao)
│   │   └── entity/  (TimeRecordEntity, CategoryEntity, TagEntity,
│   │                  RecordTagCrossRef, RecordWithTags)
│   └── repository/  (TimeRecordRepositoryImpl, CategoryRepositoryImpl,
│                      TagRepositoryImpl)
├── domain/
│   ├── model/  (TimeRecord, Category, Tag, RecordType)
│   └── repository/  (TimeRecordRepository, CategoryRepository, TagRepository)
├── di/  (DatabaseModule, RepositoryModule)
└── ui/
    ├── navigation/  (AppNavGraph, Screen, BottomNavBar)
    ├── theme/  (Theme, Color, Type)
    ├── components/  (TimeRecordCard, CategoryChip, TagSelector,
    │                 DateTimePicker, DurationText)
    ├── records/  (RecordListScreen, RecordListViewModel)
    ├── addrecord/  (AddRecordScreen, AddRecordViewModel)
    ├── timer/  (TimerScreen — Phase 3)
    ├── sleep/  (SleepScreen — Phase 4)
    └── analytics/  (AnalyticsScreen — Phase 5)
```
