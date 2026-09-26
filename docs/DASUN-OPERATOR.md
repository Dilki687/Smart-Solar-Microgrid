# Dasun: Grid Operator and energy transfer operations

Ownership: Grid Operator dashboard, assigned-station availability, QR generation/display/scanning/verification, energy transfer completion, and one-time completion protection. Authentication, station management, and the reservation workflow remain the existing team implementations. Both clients communicate through the C# API; Android has no MongoDB access.

## Actual API routes

| Method and route | Role | Request / response |
| --- | --- | --- |
| `POST /api/transactions/qr` | `PROSUMER`, reservation owner | `{ "reservationId": "RES-..." }`; transaction ID, `qrToken`, statuses, generation and expiry times |
| `POST /api/transactions/verify` | `GRID_OPERATOR`, assigned to station | `{ "qrToken": "TRX:..." }`; authoritative booking/Prosumer/station/transaction details |
| `POST /api/transactions/{transactionId}/complete` | Same assigned operator who verified | No request body; completion details and timestamp |
| `GET /api/operator/dashboard` | `GRID_OPERATOR` | Actual counts, `today`, `upcoming`, assigned `stations`, active upcoming `slots` |
| `PATCH /api/stations/{stationId}/availability` | Assigned `GRID_OPERATOR` | `{ "slotId": "SLOT-...", "totalCapacity": 10 }`; updated slot |

Existing `/api/auth/login`, `/api/auth/logout`, and reservation endpoints are reused. Requests can be exercised with [Operator.http](../Backend/SmartSolarMicrogrid.API/Operator.http) or the existing Swagger bearer authentication UI.

New endpoints retain the existing service result tuple and JSON `{ "message": "..." }` errors. Model validation produces the existing ASP.NET validation response. Status codes: 400 invalid input, 401 absent/expired/logged-out JWT, 403 role/ownership/assignment, 404 missing resource, 409 conflicting/expired/used state, 500 generic unexpected failure.

## Backend design and business rules

`EnergyTransaction` is an optional embedded subdocument of the existing `EnergyReservations` collection. It has an independent `TRX-...` application ID, reservation/station/slot references, Prosumer NIC, booking snapshot, token, states, and UTC audit metadata. Existing reservations need no backfill. Partial unique indexes enforce transaction ID/token uniqueness without indexing legacy documents that lack these fields.

Embedding deliberately avoids a second reservation collection and a two-document completion failure window. A single conditional `FindOneAndUpdate` changes all of:

- reservation `Status = COMPLETED`;
- transaction `TransactionStatus = COMPLETED`;
- transaction `QRStatus = USED`;
- completion operator ID, UTC time, and reservation update time.

The filter requires the same confirmed reservation snapshot, no pending change, and the same verified transaction. Competing requests cannot both match. This works on standalone MongoDB as well as replica sets, following [MongoDB's single-document atomicity model](https://www.mongodb.com/docs/manual/core/write-operations-atomicity/).

QR payloads contain only `TRX:` plus 32 cryptographically random bytes encoded as hex. Tokens are excluded from all shared reservation JSON using `JsonIgnore`; only the authenticated owner receives their token through the generation endpoint. Duplicate generation returns the same active token. When approved changes alter station/slot/time/energy details, the old token fails verification and owner generation replaces it. A token expires at the reservation's scheduled end; expired or cancelled reservations cannot generate usable tokens. The reservation remains the authority for cancellation; no separate transaction cancellation job is needed.

Generation, verification, and completion check a confirmed reservation, no pending change, an active Prosumer account, an active station/slot, matching schedule, and expiry. Verification/completion also check an active Grid Operator account and the station's existing `OperatorUserId`. Reverification by the same operator is allowed to recover from navigation/rotation; completion remains one-time. A different operator cannot complete the first operator's verification.

No additional restriction requiring the slot to have started was invented. Operational confirmation remains the operator's responsibility. Completion does not release the consumed booking capacity. Capacity in the existing model counts reservations, **not battery kWh**. Availability adjusts total slot capacity and derives the remainder, preserving existing reserved/consumed units, schedules, and station configuration. Conditional writes reject a racing capacity change.

Dashboard scope follows existing station assignments. An operator with no assigned stations sees an empty dashboard. Today is the Sri Lankan calendar day (`Asia/Colombo`, UTC+05:30); timestamps remain UTC. Today/upcoming lists include pending and confirmed reservations. Pending count covers all pending assigned reservations; completed count covers all completed assigned reservations.

## Shared integration changes

- `EnergyReservation.cs`: optional transaction field hidden from shared JSON.
- `MongoDbService.cs`: transaction/token indexes on the existing reservation collection.
- `BookingService.cs`: conditional reservation writes reject stale status changes; completed reservations cannot be reopened by stale approval/cancellation/change requests. Slot updates also compare the original capacity before writing. Existing 7-day, 12-hour, approval, modification, and capacity rules are preserved.
- `StationService.cs` / `StationsController.cs`: narrow assigned-operator availability method/route, without changing station configuration.
- `Program.cs`: new service registration and generic exception response; existing JWT/roles and Swagger configuration are reused.
- Android `UserDao.kt`: synchronize each existing DAO operation. Device logs exposed overlapping Retrofit token reads closing another request's SQLite cursor; this small integration fix preserves the session schema and authentication implementation.

All business decisions are in services; new controllers only validate identity, delegate, and return the service's HTTP result. Concurrent station reassignment/account deactivation is read from separate documents, so those checks are not a cross-document serializable transaction. Reservation/QR/completion state itself is atomic.

## Android implementation

Kotlin/XML Activities, existing Retrofit client/auth interceptor, SQLite `SessionManager`, shared reservation adapter/detail screen, and existing theme are retained.

- `OperatorDashboardActivity`: assigned jobs, actual counts, refresh, scan, availability, and logout.
- `OperatorAvailabilityActivity`: assigned active slot selection and total-capacity update.
- `QrTransferActivity`: camera permission, scan/retry, server verification, authoritative booking details, physical-transfer confirmation, completion receipt, duplicate prevention, and safe error messages. Activity recreation re-verifies pending details with the server. A failed completion requires another scan because the server may already have committed.
- `ReservationQrActivity`: owner-only QR generation/display from confirmed reservation details. Refresh fetches authoritative state. The QR is hidden from screenshots/recent-task previews.
- `OperatorBaseActivity`: existing session/role checks, 401 handling, API logout, local clearing, and login navigation with `NEW_TASK | CLEAR_TASK`.

The scanner and encoder use [ZXing Android Embedded 4.3.0](https://github.com/journeyapps/zxing-android-embedded), via `ScanContract`/`ScanOptions` and `BarcodeEncoder`. It supports the project's existing minimum SDK 24. No second Retrofit client or authentication system was introduced. Camera access is optional for installation and requested at runtime; denial leaves a retry/settings explanation.

## File inventory

Backend additions under `Backend/SmartSolarMicrogrid.API`:

- `Models/EnergyTransaction.cs`
- `DTOs/Transactions/TransactionRequests.cs`
- `DTOs/Stations/UpdateAvailabilityRequest.cs`
- `Services/TransactionService.cs`, `Services/OperatorService.cs`
- `Controllers/TransactionsController.cs`, `Controllers/OperatorController.cs`
- `Operator.http`

Backend modified: `Models/EnergyReservation.cs`, `Services/MongoDbService.cs`, `Services/BookingService.cs`, `Services/StationService.cs`, `Controllers/StationsController.cs`, `Program.cs`.

Android additions under `Android/app/src/main`:

- `java/com/smartsolar/microgrid/model/OperatorModels.kt`
- `java/com/smartsolar/microgrid/data/remote/OperatorApi.kt`
- `java/com/smartsolar/microgrid/data/repository/OperatorRepository.kt`
- `java/com/smartsolar/microgrid/ui/operator/OperatorBaseActivity.kt`
- `java/com/smartsolar/microgrid/ui/operator/OperatorDashboardActivity.kt`
- `java/com/smartsolar/microgrid/ui/operator/OperatorAvailabilityActivity.kt`
- `java/com/smartsolar/microgrid/ui/operator/QrTransferActivity.kt`
- `java/com/smartsolar/microgrid/ui/prosumer/ReservationQrActivity.kt`
- `res/layout/activity_operator_dashboard.xml`, `activity_operator_availability.xml`, `activity_qr_transfer.xml`, `activity_reservation_qr.xml`
- `res/values/strings_operator.xml`

Android modified: `MainActivity.kt`, `ApiClient.kt`, `AuthApi.kt`, `AuthRepository.kt`, `UserDao.kt`, `ReservationDetailsActivity.kt`, its XML layout, `AndroidManifest.xml`, `app/build.gradle.kts`, `gradle/libs.versions.toml`.

Tests: `Backend/SmartSolarMicrogrid.OperatorTests/{SmartSolarMicrogrid.OperatorTests.csproj,Program.cs}`, Android `OperatorWorkflowTest.kt` and `OperatorDeviceTest.kt`.

## Reproducible tests

Validation recorded on 2026-09-26:

| Check | Result |
| --- | --- |
| Backend restore | Passed |
| Backend Release build | Passed, zero compiler warnings/errors |
| Real HTTP/JWT/MongoDB integration runner | 74 assertions passed |
| Concurrent QR generation | 16 requests returned the same active QR |
| Concurrent completion | Exactly 1 success and 15 HTTP 409 responses |
| Reused QR / repeated completion | Rejected with HTTP 409 |
| Standard Android `assembleDebug` | Passed |
| Android JVM tests | 4 passed, including 3 new operator tests |
| Connected-device original operator workflow | Passed: login, dashboard, availability, verification, completion, reused QR, logout/session guard |
| Connected-device Prosumer QR display | Passed independently |
| Connected-device concurrent session reads | Passed independently |
| Expanded native camera test | Final rerun requires the connected phone to be unlocked |
| Android lint | Fails on 6 existing `IncludeLayoutParam` errors in unchanged `activity_backoffice_dashboard.xml`, lines 194/197/200/203/206/209 |

The initial Debug backend build encountered the executable locked by an already running development API. Release output was used for validation without stopping that process. The test-runner restore reported NU1900 when the network blocked NuGet vulnerability metadata; package resolution and execution still succeeded. `dotnet test` on the original solution found no configured test project; the 74 checks are from the explicit integration runner.

The standard APK is `Android/app/build/outputs/apk/debug/app-debug.apk`. Restart the development API normally to load the new routes; the already running Debug process was intentionally left running during isolated testing.

Backend restore/build:

```powershell
dotnet restore SmartSolarMicrogrid.sln
dotnet build SmartSolarMicrogrid.sln -c Release --no-restore
```

The repository had no .NET test project. A dependency-free integration runner now exercises real HTTP, the real login/JWT/role middleware, and real MongoDB. Run a disposable MongoDB listener on `127.0.0.1:27028`, then from the repository root:

```powershell
dotnet run --project Backend/SmartSolarMicrogrid.OperatorTests -c Release
```

`OPERATOR_TEST_MONGO` can override the listener. The runner creates a randomly named `OperatorTests_*` database and deletes only that database in `finally`. It starts/stops its own API on port 5159 with random test credentials and a random signing key. It does not use or change application configuration/User Secrets. This runner is invoked with `dotnet run`, not `dotnet test`.

Android:

```powershell
cd Android
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

For isolated connected-device tests, build `assembleDebug assembleDebugAndroidTest -PoperatorCheck=true`; this uses application ID `com.smartsolar.microgrid.operatorcheck` and port 5160, leaving the normal installed app/session intact. Start the backend runner with `-- --android`, forward device port 5160 to host 5159, and provide the generated ignored `artifacts/operator-device-fixture.json` values as instrumentation arguments `password`, `qrToken`, and `displayReservationId`. Start with a fresh isolated app install/data so the camera permission dialog appears. Create `artifacts/operator-device-stop` afterward to clean up the fixture/API/database. Never commit the generated fixture file.

The connected test exercises the real API through the UI; it supplies decoded QR results at the camera Activity boundary. The JVM test separately encodes and decodes a real QR image. Physical camera focus/lighting and optical capture still require a real QR presented to the device.

## Manual optical acceptance check

1. Ensure Backoffice has assigned the station's `OperatorUserId` to the operator's application user ID.
2. Log in as a Prosumer, open a confirmed reservation with no pending change, and select **Show Energy Transfer QR**.
3. On a second device, log in as its Grid Operator and choose **Scan Prosumer QR**.
4. Deny permission once and verify the explanation; grant permission and scan the displayed QR.
5. Check Prosumer/NIC/station/slot/time/energy details, perform the physical transfer, and confirm completion.
6. Verify the receipt, refreshed completed dashboard count, and Prosumer's completed reservation.
7. Scan the same QR and repeat the completion HTTP request: both must be rejected with 409.
8. Log out, press Back, and verify the dashboard cannot reopen. Also test rotation, offline errors, and expired sessions.

## Suggested commits

- `feat(transactions): add secure QR lifecycle and atomic one-time completion`
- `feat(operator): add assigned-station dashboard and availability APIs`
- `fix(bookings): guard reservation state and capacity integration writes`
- `feat(android): add operator dashboard and QR transfer workflow`
- `test(operator): cover HTTP lifecycle, concurrency, and Android workflow`

No commits are created automatically.
