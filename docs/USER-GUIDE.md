# Glide User Guide

Glide is a macOS desktop app for running a class-based business: leads, enrollments, scheduling, attendance, and billing in one workspace. Data is saved automatically to your Mac.

In the app, open **⋯** → **User guide…** (this document on GitHub) or **Documentation on GitHub…** (all docs in the repo).

---

## Getting started

### Run the app

```bash
./gradlew :glide:run
```

### First launch — company settings

On first run you must enter:

- **Legal company name**
- **Company email** (also used for data backups)
- **Company phone number**
- **FPS number** (shown on invoices so customers can pay you)

Open these later from the menu (**⋯** → **Company settings…**) or **Set up company** in the toolbar.

Until company settings are complete, **Email data backup** is disabled.

---

## How the app is organized

### Two view modes

Use the segmented control in the top bar (or keyboard shortcuts):

| Mode | Purpose |
|------|---------|
| **Customers** | Leads, plans, clients, students, sold plans, billing |
| **Scheduling** | Classes, term calendar, sold plans on classes, terms, locations, attendance |

- **⌘1** (Ctrl+1 on non-Mac) — Customers  
- **⌘2** — Scheduling  

### Floating panels

Each area opens as a **floating panel** you can move, resize, dock, and stack. The bottom **tab bar** switches between open panels.

**Menu (⋯):**

- **Panels** — show or hide each panel for the current mode  
- **Save workspace layout…** / **Saved layouts…** / **Restore default layout**  
- **Google Calendar sync…**  
- **Company settings…**  
- **Email data backup…**  

### Keyboard shortcuts

| Shortcut | Action |
|----------|--------|
| ⌘1 / Ctrl+1 | Customers mode |
| ⌘2 / Ctrl+2 | Scheduling mode |
| ⌘3–7 / Ctrl+3–7 | Focus panel 1–5 (order matches the tab bar) |
| ⌘⇧3–7 / Ctrl+Shift+3–7 | Focus and fill the window with that panel |
| ⌘⇧0 / Ctrl+Shift+0 | Restore default layout for the current mode |

Panel order in **Customers**: Leads, Plans, Clients, Sold Plans, Students (Billing opens when you work on a sold plan).  
Panel order in **Scheduling**: Classes, Term calendar, Sold Plans, Terms, Locations (Attendance opens for a class session).

---

## Core concepts

| Term | Meaning |
|------|---------|
| **Lead** | A household interested in a plan — draft client details until you mark them sold |
| **Sold plan** | An active enrollment: main client + students on a plan with a start date |
| **Client** | The main contact (parent/guardian or adult student) |
| **Student** | A person who attends classes (may be linked to a client) |
| **Plan** | A product template (price, lesson count, rolling or fixed) |
| **Enrollment** | Billing record for a sold plan (plan snapshot, period, status) |
| **Class** | A recurring schedule slot; sold plans are assigned here |
| **Term** | A date range for organizing the calendar |
| **Location** | Where classes run |

**Typical flow:** Lead → mark **Sold** → client created → sold plan + first bill → assign to a **class** → take **attendance** → **issue** bill → **payment** → invoice/receipt.

---

## Customers mode

### Leads

Track prospects before they become customers.

- Enter main client name, email, phone, date of birth (as needed)
- Add **students** to the household
- Choose a **plan** and **plan start date**
- Set whether the **main client attends class** (students always can)
- Track **status**: New, Contacted, Waiting Reply
- Add **notes**

**Mark as sold** when ready. Requirements:

- Plan selected  
- Valid plan start date  
- Main client details (name; email and phone for new clients)  
- At least one class participant (main client and/or students)  
- Save any pending edits first  

Converting a lead creates a **client** (if new), a **sold plan**, and an initial **scheduled bill**.

### Plans

Define what you sell:

| Plan kind | Typical use |
|-----------|-------------|
| **Multi lesson plan** | Package of classes; optional **rolling** (sessions roll within the plan) |
| **Single lesson plan** | One class |
| **Camp** | Multi-day camp (lesson count = days) |

Set name, lesson count, price, currency, and notes. Plans linked to sold plans cannot be edited or deleted until those sold plans are removed.

### Clients and students

- **Clients** — contacts; created at lead conversion or manually  
- **Students** — attendees; link to households via sold plans  

Records on active sold plans cannot be deleted.

### Sold plans

Locked after creation (same household and plan selection). To change terms for an existing customer, **clone to lead** and sell again, or use scheduling/billing tools.

**Revert to lead** removes billing and class enrollment and restores a lead (destructive).

Open **Billing** from a sold plan to manage bills and payments.

### Billing (per sold plan)

Opens as its own panel for the selected sold plan.

**Bill lifecycle:**

1. **Scheduled** — edit line items and amounts; apply attendance credits  
2. **Issued** — frozen for the customer; generate invoice  
3. **Paid** — record payment; generate receipt  
4. **Void** — only while still scheduled  

**Actions:**

- Toggle **Issued to customer** when ready to send  
- **Generate invoice** (PDF under `~/Documents/Glide/invoices/`)  
- **Record payment** (cash, bank transfer, card, other)  
- **Generate receipt** after payment (`~/Documents/Glide/receipts/`)  
- Add renewal bills for fixed plans; rolling plans renew automatically when the plan period ends  
- **Cancel plan** for rolling enrollments (completes when billing rules allow)  

**Attendance credits:** When attendance is submitted, absentees may earn per-session credits that reduce the next bill. Credits appear on scheduled bills before issue.

**Blocking rules:** Banners and billing messages may block issuing bills until you:

- Submit outstanding past attendance  
- Assign the sold plan to a class (and meet rolling-term coverage rules)  

---

## Scheduling mode

### Terms

Define non-overlapping date ranges. Terms in use by classes cannot be edited or deleted.

### Locations

Venues for classes. Locations in use cannot be deleted.

### Classes

Create classes with schedule, term(s), location, and color. Assign **sold plans** and set which **session dates** each sold plan attends.

Sold plans must be on a class before attendance and many billing steps work.

### Term calendar

Read-only calendar across terms, classes, and attendance — not a separate data type.

### Sold plans (scheduling)

Same households as in Customers mode, focused on **class assignment** and **transfer**:

- **Transfer** moves a sold plan to another class (preview effective date; requires past attendance submitted on the old class)  

### Attendance

Open attendance for a **class session** (from Classes or alert banners).

- Before the class **end time**, the session is preview-only  
- After end time, mark each attendee **Present** or **Absent**  
- **Submit attendance** locks the session (cannot be changed)  
- On submit, eligible **absent** attendees may receive **attendance credits** toward billing  

Past sessions without submitted attendance trigger reminders in Customers mode and can block bill issuance.

---

## Alerts (top of window)

Yellow banners summarize work that needs attention. Click **Open** to jump to the right place.

| Alert | Meaning |
|-------|---------|
| Pending attendance | Past class sessions still need submission |
| Bills to issue | Scheduled bills ready to issue |
| Overdue payments | Issued bills past due |
| Rolling term coverage | Rolling sold plan needs class assignment across terms |
| Unassigned sold plans | Sold plan not on any class |

In Scheduling mode, a compact pending-attendance strip appears above the panels.

---

## Settings and integrations

### Google Calendar sync

**⋯** → **Google Calendar sync…**

- Connect your Google account  
- Enable **Keep Google Calendar updated**  
- Glide writes class sessions to a dedicated Glide calendar  
- **Glide is the source of truth** — edits in Google Calendar may be overwritten on sync  
- Use **Sync now** or rely on scheduled sync when enabled  

OAuth setup (admin): place `clientId` and `clientSecret` in  
`~/Library/Application Support/Glide/google-oauth-client.properties`,  
or save Google’s desktop client JSON as `google-oauth-client.json` in that folder.

### Email data backup

**⋯** → **Email data backup…**

- Flushes and zips Application Support data and Documents exports  
- Opens a draft email to your **company email** with the zip attached  
- A copy is saved under `~/Documents/Glide/backups/`  

You may be prompted to back up on first session and when quitting.

---

## Where your data lives

| What | Location |
|------|----------|
| Database | `~/Library/Application Support/Glide/data/glide-db.json` |
| App settings | `~/Library/Application Support/Glide/settings.properties` |
| Invoices | `~/Documents/Glide/invoices/` |
| Receipts | `~/Documents/Glide/receipts/` |
| Backup zips | `~/Documents/Glide/backups/` |

Saves are automatic (debounced after changes). Quitting flushes pending saves.

Panel positions and which mode you are in are **not** stored in the database; workspace layouts are stored in app settings.

For technical persistence details, see [DATA-PERSISTENCE.md](./DATA-PERSISTENCE.md).

---

## Quick reference — daily workflow

1. **Leads** — capture inquiries; set plan and start date  
2. **Mark sold** — creates client, sold plan, first bill  
3. **Classes** — assign sold plan and session dates  
4. **Attendance** — submit after each session  
5. **Billing** — issue bill, email/send invoice, record payment, receipt  
6. **Backup** — email zip periodically via the menu  

---

## Troubleshooting

| Problem | What to check |
|---------|----------------|
| Cannot mark lead sold | Plan, start date, client contact fields, participants, save edits |
| Cannot issue bill | Submit past attendance; assign sold plan to class; read billing panel messages |
| Cannot transfer class | Submit attendance on the old class for all past sessions |
| Backup disabled | Complete company settings with a valid email |
| Calendar not connecting | OAuth files in Application Support/Glide; read error in sync dialog |
| Plan cannot edit/delete | Remove or revert all sold plans using that plan |
