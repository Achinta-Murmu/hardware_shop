# Shop app (`hardware_shop`): guide for AI agents and contributors

This is the single source of truth about this repo for any AI agent or new contributor.
`CLAUDE.md` only imports this file, so edit this one.

Last verified: 2026-11-14, against `main@270d72e`, `master@22237dc` and the first commit of
`android-native`.

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
| `master` | Flutter app, first commit only. GitHub's default branch. | Behind `main` by 2 commits |
| `main` | Flutter app plus sales billing, QR scanning, sales history, receipt printing | The most complete Flutter code |
| `android-native` | Native Android rebuild, from zero. Branched from `main`. | Created. Holds the Flutter code from `main` plus these docs. No native code yet. |

Note that `main` is ahead of `master`, although `master` is the default. Read `main` when you
want to know what the Flutter app does.

Remotes in the working clone: `origin` is Achinta's repo (read-only for the rebuild lead).
`fork` is `nayan-dey/hardware_shop`, where `android-native` is pushed and will be built.

Feature status:

| Feature | Flutter (`main`) | Native |
|---|---|---|
| Item list: name, category, brand, code, buy price, sell price, stock, minimum stock, unit | Built | Not started |
| Search items by name, category, brand, code | Built | Not started |
| Low-stock flag (stock at or below minimum) | Built, on the Stock list only | Not started |
| Restock list as its own view | Missing | Not started |
| QR sticker per item, printable (50 × 30 mm) | Built | Not started |
| Scan QR to add an item to a bill | Built | Not started |
| Purchases with supplier, stock goes up | Built | Not started |
| Sales bill, sales history, receipt print (80 mm) | Built | Not started |
| Dashboard totals | Placeholder, shows zeros | Not started |
| Daily and monthly income, expenditure, profit | Missing | Not started |
| Other expenses (rent, electricity and so on) | Missing | Not started |
| Backup and restore | Missing | Not started |

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

## 5. Design direction

**Planned. No design exists yet.** When the design system is built, replace this section's
plans with the real tokens (colours, type, shapes, spacing) and the file paths that hold them.

Fixed rules:

- Theme with `MaterialExpressiveTheme` and expressive motion.
- Use the Expressive version of a component wherever one exists: short navigation bar, buttons
  with shape change on press, icon buttons, chips, connected button groups, segmented list
  items, top app bar with subtitle, loading indicator, wavy progress, the Material shape library.
- Where Material has no Expressive version (switch, cards, text fields, slider, snackbar, bottom
  sheet, badges), use the standard Material 3 component.
- Text uses Material's type scale, including its emphasized styles. No one-off font sizes.
- The app has its own colour identity as the default, plus an optional "phone colours" mode that
  follows the wallpaper (Android 12 and newer).
- Light and dark themes both designed, not one derived carelessly from the other.
- Haptic feedback on meaningful touches (confirm sale, quantity steps, scan success, toggles),
  following the phone's touch-feedback setting.
- Set the content colour once at the app root so icons and text are never dark on dark.
- A shop counter is bright, busy and one-handed: large touch targets, big readable numbers,
  the most common action (start a sale, scan) reachable with a thumb.

## 6. Architecture

**Planned. Nothing below is built yet.** Confirm each choice when its module is written, then
turn it into fact.

- **One Gradle project at the repo root** of the `android-native` branch, single `app` module.
  The Flutter files are removed from that branch; they remain on `main`.
- **Stack**: Kotlin, Jetpack Compose, Navigation 3, Room (database), DataStore (settings),
  coroutines and Flow, Material 3 `1.5.x` (the first version with Expressive components).
  Start from the versions Kerb uses and check for newer stable releases at that time.
- **No dependency-injection framework.** One hand-written container object creates the database
  and repositories and hands them to ViewModels.
- **No network layer, and no internet permission.** Local first is then a fact anyone can check,
  not a promise.
- **Layers**, each depending only on the one below:

  | Folder | Job |
  |---|---|
  | `ui/` | Screens and ViewModels. A screen shows state and reports events. Nothing else. |
  | `ui/theme`, `ui/components` | Design system: tokens and shared building blocks |
  | `data/repo` | Repositories. The only thing ViewModels talk to. Business rules live here. |
  | `data/local` | Room entities, DAOs, migrations |
  | `core/model` | Plain Kotlin types (item, bill, money). No Android imports. |
  | `core/util` | Small helpers: formatting money, dates, IDs |

- **The database is the single source of truth.** Screens observe it as a stream, so every
  screen updates by itself after any write. No screen reloads by hand, and no screen opens its
  own database.
- **QR and printing**: on-device barcode scanning that works offline, QR generation on device,
  printing through Android's print system. Libraries to be chosen when that module is built.

## 7. Data rules

**Planned for native.** These are rules, not a schema; the schema is designed with its module
and then documented here.

- **Money is whole paise** stored as an integer (`Long`). Never floating point, which makes
  totals drift by a paisa.
- **Stock is a ledger.** Stock is never typed over. Every change is a row (purchase in, sale
  out, correction, return) and the current stock is the result. This is what makes restock
  lists, daily reports and "where did it go" answerable.
- **History is frozen.** A bill or purchase stores the item's name, unit, selling price and
  cost as they were at that moment, so old records and profit stay correct after edits.
- **Items with history are archived, not deleted.**
- **One action, one transaction.** A sale or purchase that touches several rows either fully
  happens or does not happen at all.
- **Item identity**: a short unique code created on the phone, never changed, printed under the
  QR code. The QR payload carries a format version so it can evolve.
- **Time** is stored as UTC milliseconds. "Today" and "this month" are worked out in the phone's
  timezone.
- **Profit** = sales minus cost of the goods sold, minus other expenses.
- Because data lives only on the phone, **backup and restore to a file the owner chooses** is a
  required feature, not an extra.

## 8. Build and test

Flutter branches (`main`, `master`):

- `flutter pub get`, then `flutter run`.
- After editing `lib/database/tables.dart`, regenerate the database code:
  `dart run build_runner build`. Never edit `lib/database/app_database.g.dart` by hand.
- `test/widget_test.dart` is the untouched Flutter template and does not match the app.

Native branch (**planned**):

- The lead's machine has no Flutter, JDK or Android SDK, and builds are not to run on it.
  Building happens in the cloud with GitHub Actions: a push builds the APK and publishes it to a
  rolling preview release for phones.
- Pushes go to the fork `nayan-dey/hardware_shop` (remote name `fork`), because the lead has
  read-only access to `Achinta-Murmu/hardware_shop`. The build workflow does not exist yet.
- Screens are checked with an automated walkthrough on a cloud emulator that saves screenshots.
  Write it with UI Automator (tap by on-screen text). Do not wait for the app to go "idle":
  never-ending animations make that wait hang.
- A build that compiles is not a screen that works. Say which screens were actually seen running.

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

1. **App name and app ID** for the native app.
2. **Fractional quantities**: can the shop sell 2.5 m of wire or 0.5 kg of nails? The Flutter
   app allows whole numbers only.
3. **Languages**: English only, or also Hindi, Bengali or others?
4. **Printers**: which sticker and receipt printers does the shop actually have?
5. **Tax (GST)** on bills: needed or not?
6. **Existing data**: is anyone already using the Flutter app with real data that must be
   carried over?

## 11. Change log

Newest first. One line per change to this file.

- 2026-11-14: Branch `android-native` created from `main` and pushed to the fork
  `nayan-dey/hardware_shop`. Open question about where to push is answered.
- 2026-11-14: Added the explicit "no login, no account, no server" rule to section 4.
- 2026-11-14: File created from a full read of `main` and `master`. Native rebuild rules,
  planned architecture and data rules recorded. Nothing native is built yet.
