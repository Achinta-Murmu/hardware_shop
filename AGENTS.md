# Shop app (`hardware_shop`): guide for AI agents and contributors

This is the single source of truth about this repo for any AI agent or new contributor.
`CLAUDE.md` only imports this file, so edit this one.

Last verified: 2026-11-14, against `main@270d72e`, `master@22237dc` and `android-native` after the
first native commit.

## 1. Keep this file true

This file is maintained as part of the work, not after it. The rules:

1. **Same commit.** If a change makes any line here wrong or incomplete, fix that line in the
   same commit as the change. A task is not done while this file is out of date.
2. **Code wins.** If this file and the code disagree, trust the code, then correct this file
   before doing anything else.
3. **Facts and plans stay apart.** Things that exist are written as facts. Things that do not
   exist yet are marked **Planned**. When a planned thing is built, move it to fact and remove
   the marker. Never describe a plan as if it were built.
4. **Stamp it.** After updating, change the "Last verified" line above (date and commit) and add
   one line to the change log in section 11.
5. **Keep it short.** Delete what is no longer true or no longer useful. Do not let it grow into
   a history book; git has the history.

What to update when:

| When you change | Update |
|---|---|
| A feature is added, finished or removed | Feature table in section 3 |
| A branch is created, renamed, merged or deleted | Section 3 |
| Database tables, columns or a data rule | Section 7 |
| A library, version, folder or layer | Section 6 |
| A design token, component choice or design rule | Section 5 |
| A screen or its wording | The matching step in `WalkthroughTest.kt` |
| How to build, test, release or get an APK | Section 8 |
| An open question gets an answer | Remove it from section 10, record the answer where it belongs |
| The owner states a new rule or preference | Section 4 |

## 2. What the app is

A phone app for a local shop owner, built first for a hardware shop, who runs the shop alone or
with a helper. It replaces the paper notebook. It must work with no internet.

What the owner needs to do with it:

- Keep a list of items with the **buying price** and the **selling price** of each.
- Know how much **stock** of each item is left, and see which items are low or sold out so they
  can be **restocked**.
- Give every item a **unique ID and a QR code**, print it as a sticker, and scan it at the counter.
- Record **purchases** (stock coming in from a supplier) and **sales** (bills to customers).
- See **daily and monthly income, expenditure and profit**.

Money is in Indian rupees (₹).

The repo owner is Achinta, who wrote the Flutter app. The native Android rebuild is a separate
effort on its own branch.

## 3. Branches and where things stand

| Branch | What it holds | State |
|---|---|---|
| `master` | Flutter app, first commit only. Default branch of Achinta's repo. | Behind `main` by 2 commits |
| `main` | Flutter app plus sales billing, QR scanning, sales history, receipt printing | The most complete Flutter code |
| `android-native` | **Galla**, the native Android rebuild. The Flutter files are removed from this branch. | Active. See the feature table. |

Read `main` when you want to know what the Flutter app does (`git show main:lib/...`).

Remotes in the working clone: `origin` is Achinta's repo (read-only for the rebuild lead).
`fork` is `nayan-dey/hardware_shop`, where `android-native` is pushed and built.

Feature status. "Seen" means it was seen working in walkthrough screenshots on a cloud
emulator, not only compiled. Nothing has been tried on a real phone by the agent.

| Feature | Flutter (`main`) | Native (`android-native`) |
|---|---|---|
| Item list: name, category, unit, buy price, sell price, stock, low level | Built | Written |
| Search items; filter by low and sold out | Search only | Written |
| Shelf level shown as a bar on every item (wavy and amber when low) | No | Written |
| Restock list with a suggested quantity, shareable as text | Missing | Written |
| Unique item code and QR sticker, printable (50 × 30 mm) | Built | Written |
| Scan QR to add to a bill (keeps scanning) or to find an item | Built | Written, cannot be tested on an emulator |
| Stock in from a supplier (stock up, buy price updated) | Built | Written |
| Count stock (correct the number, with a reason) | Missing | Written |
| Sales bill, receipt, print, send as text | Built | Written |
| Quantities with fractions (2.5 m, 0.5 kg) | No | Written |
| Today screen: sales, profit, spending, stock value, recent activity | Placeholder zeros | Written |
| Daily and monthly money in, money out, profit, chart, best sellers | Missing | Written |
| Other expenses (rent, electricity and so on) | Missing | Written |
| Backup to a file and restore from it | Missing | Written |
| Light and dark theme, optional phone (wallpaper) colours, haptics | No | Written |
| Sample shop for trying the app | No | Written |
| Languages other than English | No | Not started |
| GST on bills | No | Not started |

## 4. Rules for the native rebuild

These come from the person leading the rebuild. Do not reopen them unless they do.

- **Native Android**: Kotlin and Jetpack Compose. Not Flutter.
- **Material 3 Expressive**, using Google's real components. Never hand-draw a control that
  Material already provides.
- **Design from zero.** Do not copy or port the Flutter screens, layouts or colours. The Flutter
  app is a reference for what the shop owner needs, not for how it looks. The app should stand
  out, not look like a default template.
- **Local first.** All data lives on the phone. The app must be fully usable offline, forever.
- **No login, no account, no sign-up, no server.** The owner installs the app and starts using
  it. Nothing is asked for and nothing leaves the phone.
- **Clean architecture and clean design**, in the same style as the sibling project Kerb (a
  private native Android app by the same lead). The lessons taken from Kerb are written into
  sections 5, 6 and 8 so this file stands on its own.
- The Flutter branches stay untouched. The rebuild happens only on its own branch.
- Explain things in plain language. Avoid jargon, or explain it the first time it appears.

## 5. Design system

The identity is **"money green and lime on warm paper"**. Tokens live in
`app/src/main/java/com/galla/app/ui/theme/`.

- **Colour** (`Color.kt`): surfaces are warm cream in light and a green-tinted near-black in
  dark. Green is the brand (money in). **Lime `#C8F25C` is the signature**: the one big number
  on a screen sits on a lime "hero" card. Amber means "pay attention" (low stock). Red means
  "stop" (sold out, a loss). `GallaExtras` holds the colours Material has no role for.
- **Type** (`Type.kt`): one variable font, Bricolage Grotesque (`res/font`). `Bricolage` is the
  reading version; `BricolageDisplay` is narrower and heavy, for big rupee amounts. Use
  `MaterialTheme.typography` or `GallaType` (hero, figure, amount, eyebrow). No one-off sizes,
  except the hero number which shrinks by length so it always fits one line.
- **Shape** (`Theme.kt`): round and chunky, radii 8 / 14 / 22 / 30 / 38. Items are shown as a
  letter on a shape from Material's shape library; items in one category share a shape and
  colour. Receipts have a torn zigzag bottom edge (`ReceiptShape`).
- **Motion**: `MotionScheme.expressive()`. Numbers roll when they change, chart bars spring up,
  the shape in the hero card turns slowly, a saved sale pops a tick.
- **Phone colours**: an optional setting swaps Galla green for the wallpaper palette
  (Android 12 and newer). Galla green is the default.

Rules:

- Use Google's real Material components. `ui/components/` only picks a component and fills its
  slots; it draws nothing by hand except the chart, the QR code, the logo and the receipt edge,
  which Material has no component for.
- Use the Expressive version where one exists: `ShortNavigationBar`, buttons and icon buttons
  with `shapes`, `ToggleButton` groups, `SegmentedListItem`, `TopAppBar` with subtitle,
  `LinearWavyProgressIndicator`, `MaterialShapes`. Otherwise use the standard Material 3 one
  (switch, cards, text fields, snackbar, dialogs).
- Before using a Material API not already used in this repo, check its exact signature in the
  library source (Material 3 `1.5.0-rc01`). There is no local compiler to catch a wrong guess.
- Haptics go through `rememberHaptics()` and follow the phone's touch-feedback setting.
- The root `Surface` sets the content colour once, so icons and text are never dark on dark.
- A shop counter is bright, busy and one-handed: large touch targets, big numbers, and the main
  action reachable with a thumb.
- The four tabs are Today, Sell, Items, Money. The navigation bar shows on those only.

## 6. Architecture

One Gradle project at the repo root, single `app` module, package `com.galla.app`
(app id `com.galla.app`, app name "Galla", the word for a shop's cash box).

- **Stack**: Kotlin, Jetpack Compose, Navigation 3, Room, DataStore, coroutines and Flow,
  Material 3 `1.5.0-rc01`, CameraX and ZXing (QR), `androidx.print`. Versions are in
  `gradle/libs.versions.toml`.
- **No dependency-injection framework.** `AppContainer` builds everything once.
- **No network layer and no internet permission.** The manifest also strips that permission if
  a library tries to add it. Keep it that way.
- **The database is the single source of truth.** Screens observe it as a Flow, so every screen
  updates by itself after any write.

| Folder (under `app/src/main/java/com/galla/app/`) | Job |
|---|---|
| `ui/<screen>/` | A screen and its ViewModel. A screen shows state and reports events. |
| `ui/components/`, `ui/theme/`, `ui/icons/` | The design system |
| `ui/nav/` | Routes (one key per screen), the `Navigator`, the navigation bar, snackbar messages |
| `data/repo/` | `ShopRepository` holds the shop's rules. `BackupRepository`, `SampleData`. |
| `data/local/db/` | Room tables (`Entities.kt`), queries (`Daos.kt`), the database |
| `data/local/prefs/` | Settings in DataStore |
| `core/model/`, `core/util/`, `core/state/` | Units, settings; money, quantity, date and code helpers; the open bill (`CartStore`) |
| `hardware/` | QR making and reading, sticker and receipt printing |

Two things differ from the tidy picture and are deliberate:

- Screens use the Room entity classes directly. There is no separate set of "domain" classes
  and mappers, because here they would be copies of the same fields.
- Three small screens have no ViewModel and call the repository themselves: the scanner, add
  expense and settings. They keep no state worth a ViewModel.

Icons are Material Symbols Rounded as vector drawables, listed in `ui/icons/Ic.kt`. To add one,
drop its outline and filled XML in `res/drawable` (without the `android:tint` line) and add its
name to the enum.

## 7. Data rules

These are built, in `data/repo/ShopRepository.kt` and `data/local/db/Entities.kt`.

- **Money is whole paise** in a `Long` (`core/util/Money`). Never floating point.
- **Quantities are whole thousandths of a unit** in a `Long` (`core/util/Qty`), so 2.5 m is
  2500. Units marked `fractional` in `core/model/Units` can be sold in parts; the rest are
  whole numbers only.
- **Stock is a ledger.** `items.stockMilli` never changes without a row in `movements`
  (opening, purchase, sale, adjust), written in the same transaction. Each row also stores the
  balance after it.
- **History is frozen.** A sale line stores the item's name, unit, price and cost at that
  moment, so old bills and their profit survive later edits.
- **Items are archived, never deleted.**
- **One action, one transaction** (`db.withTransaction`). A sale checks every line first and
  writes nothing if any line fails.
- **A bill cannot take more than is in stock.**
- **Item identity**: a short code like `K7Q-3MX`, made on the phone, never changed. The QR
  payload is `galla:1:<code>`; the `1` is a format version.
- **Time** is UTC milliseconds. "Today" and "this month" are worked out in the phone's
  timezone (`core/util/Dates`).
- **Profit** = sales, minus what those goods cost, minus expenses. Stock bought is money out
  but only becomes a cost in profit when it is sold.
- **Backup** is one JSON file (`BackupFile`, `format = 1`) holding every table. Restore
  replaces everything.

Tables: `items`, `movements`, `sales`, `sale_lines`, `purchases`, `expenses`. Schema version 1.

**Must fix before a real shop relies on the app:** `GallaDatabase` uses
`fallbackToDestructiveMigration`, which wipes the data if the table layout changes. That is
acceptable only while the app is a preview. From the first real use, every schema change needs
a proper Room migration and that line must go.

## 8. Build and test

- The lead's machine has no JDK or Android SDK, and builds are not to run on it. **Never try to
  compile locally.**
- Push to `fork` (`nayan-dey/hardware_shop`), branch `android-native`.
  `.github/workflows/android.yml` builds the release APK and publishes it to the rolling
  `preview` release:
  https://github.com/nayan-dey/hardware_shop/releases/download/preview/galla-preview.apk
- Pushes that change only `.md` files do not start a build.
- To see the screens, push a commit whose message contains `[shots]`.
  `.github/workflows/walkthrough.yml` runs `app/src/androidTest/.../WalkthroughTest.kt` on a
  cloud emulator and uploads screenshots as the `galla-walkthrough` artifact
  (`gh run download <run id> -R nayan-dey/hardware_shop`).
- The walkthrough uses UI Automator (tap by on-screen text). Do not wait for the app to go
  "idle": the turning hero shape and wavy bars never stop, so that wait would hang.
- When a screen or its wording changes, update the matching step in `WalkthroughTest.kt`.
- A build that compiles is not a screen that works. Say which screens were actually seen.
- The signing key in `keystore/` is a throwaway so preview builds install over each other. It
  is not a Play Store key.
- The Flutter branches build the usual way: `flutter pub get`, `flutter run`.

## 9. Flutter app reference (behaviour only)

Use this to understand the shop owner's needs. Do not copy its design.

- Four tabs: Dashboard, Stock, Sales, Purchases (`lib/main.dart`).
- Database: Drift (SQLite), file `hardware_manager`, schema version 3, tables `Products`,
  `Purchases`, `Sales`, `SaleItems` (`lib/database/tables.dart`, `app_database.dart`).
- QR payload: `hardware_shop:product:<id>`, where `<id>` is the database row number.
- Sticker is 50 × 30 mm; receipt is 80 mm wide thermal paper. Both are made as PDFs.
- A sale checks stock, saves the bill and lowers stock in one transaction (`completeSale`).
- 24 units to choose from: pcs, box, pkt, set, pair, doz, kg, g, qtl, ton, m, cm, ft, in, l, ml,
  roll, bundle, bag, sheet, bar, bottle, can, drum. Quantities are whole numbers only.
- Default minimum stock is 5.

Weak spots found by reading the code (not by running it). The native app must not repeat them:

- Dashboard numbers are fixed at zero; its bell and quick actions do nothing.
- No expenses and no daily or monthly reports exist.
- Each tab opens and closes its own database connection.
- Saving a purchase is two separate writes, not one transaction.
- After "Confirm Sale" the quantities are cleared but the bill's item list is not.
- Money is stored as floating point.
- Deleting an item leaves its purchases pointing at nothing ("Unknown Product").
- `lib/models/product.dart` is an unused old copy of the Stock screen.
- The app ID is still the placeholder `com.example.hardware_shop`; the README is the template.

## 10. Open questions

Remove each one when it is answered, and record the answer in the right section.

1. **App name and app ID.** "Galla" and `com.galla.app` were chosen by the agent for the
   preview. Changing the app ID later makes it a different app on the phone, so settle it
   before real use.
2. **Languages**: English only, or also Hindi, Bengali or others? All wording is still in the
   Kotlin code, not in string resources, so translation needs that moved first.
3. **Printers**: which sticker and receipt printers does the shop actually have? Printing goes
   through Android's print screen, which suits Wi-Fi printers and "Save as PDF". Small Bluetooth
   thermal printers usually need their own driver.
4. **Tax (GST)** on bills: needed or not?
5. **Selling more than the recorded stock**: the app refuses. Should it allow it and warn?
6. **Existing data**: is anyone using the Flutter app with real data that must be carried over?

## 11. Change log

Newest first. One line per change to this file.

- 2026-11-14: Native app "Galla" written. Sections 3, 5, 6, 7, 8 and 10 rewritten from plans into
  facts. Fractional quantities answered (supported).
- 2026-11-14: Branch `android-native` created from `main` and pushed to the fork
  `nayan-dey/hardware_shop`. Open question about where to push is answered.
- 2026-11-14: Added the explicit "no login, no account, no server" rule to section 4.
- 2026-11-14: File created from a full read of `main` and `master`. Native rebuild rules,
  planned architecture and data rules recorded. Nothing native is built yet.
