using System.Diagnostics;
using System.Net.Http.Headers;
using System.Net.Http.Json;
using System.Security.Cryptography;
using System.Text.Json.Nodes;
using MongoDB.Driver;
using SmartSolarMicrogrid.API.Models;
using SmartSolarMicrogrid.API.Services;

// Real HTTP + real MongoDB regression runner, without replacing authentication or persistence.
// Usage: dotnet run --project Backend/SmartSolarMicrogrid.OperatorTests -c Release
// Requires a disposable/local MongoDB listener; never uses the application's configured database.
var mongoUrl = Environment.GetEnvironmentVariable("OPERATOR_TEST_MONGO") ?? "mongodb://127.0.0.1:27028/?serverSelectionTimeoutMS=5000";
var dbName = "OperatorTests_" + Guid.NewGuid().ToString("N");
var client = new MongoClient(mongoUrl);
var db = client.GetDatabase(dbName);
var users = db.GetCollection<User>("Users");
var stations = db.GetCollection<SolarStationInfo>("SolarStations");
var slots = db.GetCollection<EnergyBookingSlot>("EnergyBookingSlots");
var reservations = db.GetCollection<EnergyReservation>("EnergyReservations");
var password = Convert.ToHexString(RandomNumberGenerator.GetBytes(24));
var hasher = new PasswordService();
var port = 5159;
using var http = new HttpClient { BaseAddress = new Uri($"http://127.0.0.1:{port}"), Timeout = TimeSpan.FromSeconds(20) };
Process? api = null;
var assertions = 0;
try
{
    foreach (var (id, role) in new[] { ("OP1", UserRole.GridOperator), ("OP2", UserRole.GridOperator),
                 ("P1", UserRole.Prosumer), ("P2", UserRole.Prosumer), ("BO", UserRole.Backoffice) })
        await users.InsertOneAsync(new User { UserId = id, NIC = id, Name = "Test " + id,
            Email = id + "@example.invalid", Role = role, AccountStatus = AccountStatus.Active,
            PasswordHash = hasher.HashPassword(password) });
    await stations.InsertManyAsync(new[] {
        new SolarStationInfo { StationId = "ST1", Name = "Assigned station", Status = AccountStatus.Active, OperatorUserId = "OP1" },
        new SolarStationInfo { StationId = "ST2", Name = "Other station", Status = AccountStatus.Active, OperatorUserId = "OP2" }
    });
    var start = DateTime.UtcNow.AddDays(1);
    start = new DateTime(start.Ticks - start.Ticks % TimeSpan.TicksPerMillisecond, DateTimeKind.Utc);
    await slots.InsertManyAsync(new[] {
        new EnergyBookingSlot { SlotId = "SL1", StationId = "ST1", StartTime = start, EndTime = start.AddHours(1), TotalCapacity = 100, AvailableCapacity = 50 },
        new EnergyBookingSlot { SlotId = "SL2", StationId = "ST1", StartTime = start.AddHours(2), EndTime = start.AddHours(3), TotalCapacity = 100, AvailableCapacity = 50 }
    });
    var info = new ProcessStartInfo("dotnet") { UseShellExecute = false, CreateNoWindow = true,
        RedirectStandardOutput = true, RedirectStandardError = true };
    info.ArgumentList.Add(Path.GetFullPath("Backend/SmartSolarMicrogrid.API/bin/Release/net8.0/SmartSolarMicrogrid.API.dll"));
    info.Environment["ASPNETCORE_URLS"] = http.BaseAddress.ToString();
    info.Environment["ASPNETCORE_ENVIRONMENT"] = "Development";
    info.Environment["MongoDb__ConnectionString"] = mongoUrl;
    info.Environment["MongoDb__DatabaseName"] = dbName;
    info.Environment["Jwt__Key"] = Convert.ToHexString(RandomNumberGenerator.GetBytes(32));
    info.Environment["Jwt__Issuer"] = "OperatorTests";
    info.Environment["Jwt__Audience"] = "OperatorTests";
    info.Environment["BootstrapAdmin__Enabled"] = "false";
    info.Environment["Logging__LogLevel__Default"] = "Warning";
    api = Process.Start(info)!;
    var logs = new System.Collections.Concurrent.ConcurrentQueue<string>();
    api.OutputDataReceived += (_, e) => { if (e.Data != null) logs.Enqueue(e.Data); };
    api.ErrorDataReceived += (_, e) => { if (e.Data != null) logs.Enqueue(e.Data); };
    api.BeginOutputReadLine(); api.BeginErrorReadLine();
    for (var i = 0; i < 60; i++)
    {
        if (api.HasExited) throw new Exception("Test API failed to start: " + string.Join('\n', logs));
        try { using var ready = await http.GetAsync("/api/health"); break; }
        catch (HttpRequestException) { await Task.Delay(200); }
    }

    var op = await Login("OP1"); var op2 = await Login("OP2");
    var prosumer = await Login("P1"); var other = await Login("P2"); var backoffice = await Login("BO");
    await Expect("anonymous dashboard", "GET", "/api/operator/dashboard", null, null, 401);
    await Expect("prosumer dashboard forbidden", "GET", "/api/operator/dashboard", prosumer, null, 403);
    await Expect("backoffice verify forbidden", "POST", "/api/transactions/verify", backoffice, new { qrToken = "fake" }, 403);
    await Expect("missing reservation", "POST", "/api/transactions/qr", prosumer, new { reservationId = "missing" }, 404);
    foreach (var status in new[] { BookingStatus.PENDING, BookingStatus.CANCELLED, BookingStatus.COMPLETED })
    {
        var id = await Seed(status);
        await Expect("generation rejects " + status, "POST", "/api/transactions/qr", prosumer, new { reservationId = id }, 409);
    }
    var mainId = await Seed();
    await Expect("generation ownership", "POST", "/api/transactions/qr", other, new { reservationId = mainId }, 403);
    await Expect("operator cannot mint prosumer QR", "POST", "/api/transactions/qr", op, new { reservationId = mainId }, 403);
    var qr = await Expect("confirmed QR generation", "POST", "/api/transactions/qr", prosumer, new { reservationId = mainId }, 201);
    var token = qr!["qrToken"]!.GetValue<string>(); var tid = qr["transactionId"]!.GetValue<string>();
    Check(token.Length == 68 && !token.Contains(mainId), "opaque random token");
    var again = await Expect("generation reuses QR", "POST", "/api/transactions/qr", prosumer, new { reservationId = mainId }, 200);
    Check(again!["qrToken"]!.GetValue<string>() == token, "same active QR");
    await Expect("empty QR input", "POST", "/api/transactions/verify", op, new { qrToken = "" }, 400);
    await Expect("fake QR", "POST", "/api/transactions/verify", op, new { qrToken = "TRX:" + new string('A', 64) }, 404);
    await Expect("prosumer cannot verify", "POST", "/api/transactions/verify", prosumer, new { qrToken = token }, 403);
    await Expect("wrong station operator", "POST", "/api/transactions/verify", op2, new { qrToken = token }, 403);
    await Expect("unverified completion", "POST", $"/api/transactions/{tid}/complete", op, null, 409);
    await Expect("invalid transaction ID", "POST", "/api/transactions/missing/complete", op, null, 404);
    await Expect("prosumer cannot complete", "POST", $"/api/transactions/{tid}/complete", prosumer, null, 403);
    var verified = await Expect("valid verification", "POST", "/api/transactions/verify", op, new { qrToken = token }, 200);
    Check(verified!["prosumerNIC"]!.GetValue<string>() == "P1" && verified["stationName"]!.GetValue<string>() == "Assigned station", "authoritative details");
    await Expect("repeat scan by same operator", "POST", "/api/transactions/verify", op, new { qrToken = token }, 200);
    var complete = await Expect("verified completion", "POST", $"/api/transactions/{tid}/complete", op, null, 200);
    Check(complete!["transactionStatus"]!.GetValue<string>() == "COMPLETED" && complete["qrStatus"]!.GetValue<string>() == "USED", "completion response states");
    var stored = await reservations.Find(x => x.ReservationId == mainId).FirstAsync();
    Check(stored.Status == BookingStatus.COMPLETED && stored.Transaction!.CompletedByOperatorId == "OP1" && stored.Transaction.CompletedAt != null, "atomic persisted completion + audit");
    await Expect("duplicate completion", "POST", $"/api/transactions/{tid}/complete", op, null, 409);
    await Expect("used QR rejected", "POST", "/api/transactions/verify", op, new { qrToken = token }, 409);
    await Expect("completed reservation cannot mint QR", "POST", "/api/transactions/qr", prosumer, new { reservationId = mainId }, 409);
    await Expect("completed reservation cannot cancel", "POST", $"/api/bookings/reservations/{mainId}/cancel", prosumer, new { reason = "test" }, 400);

    var raceId = await Seed();
    var generation = await Task.WhenAll(Enumerable.Range(0, 16).Select(_ => Send("POST", "/api/transactions/qr", prosumer, new { reservationId = raceId })));
    Check(generation.All(x => x.Code is 200 or 201) && generation.Select(x => x.Body!["qrToken"]!.GetValue<string>()).Distinct().Count() == 1, "16 concurrent QR requests create one QR");
    var raceQr = generation[0].Body!;
    await Expect("race QR verify", "POST", "/api/transactions/verify", op, new { qrToken = raceQr["qrToken"]!.GetValue<string>() }, 200);
    var raceTid = raceQr["transactionId"]!.GetValue<string>();
    var completions = await Task.WhenAll(Enumerable.Range(0, 16).Select(_ => Send("POST", $"/api/transactions/{raceTid}/complete", op, null)));
    Check(completions.Count(x => x.Code == 200) == 1 && completions.Count(x => x.Code == 409) == 15, "16 concurrent completions: exactly one success, 15 conflicts");

    foreach (var state in new[] { BookingStatus.CANCELLED, BookingStatus.COMPLETED, BookingStatus.PENDING })
    {
        var id = await Seed(); var generated = await Generate(id);
        await reservations.UpdateOneAsync(x => x.ReservationId == id, Builders<EnergyReservation>.Update.Set(x => x.Status, state));
        await Expect("verify rejects changed state " + state, "POST", "/api/transactions/verify", op, new { qrToken = generated["qrToken"]!.GetValue<string>() }, 409);
    }
    var changedId = await Seed(); var oldQr = await Generate(changedId);
    await reservations.UpdateOneAsync(x => x.ReservationId == changedId, Builders<EnergyReservation>.Update
        .Set(x => x.SlotId, "SL2").Set(x => x.ScheduledStartTime, start.AddHours(2)).Set(x => x.ScheduledEndTime, start.AddHours(3)));
    await Expect("changed slot invalidates QR", "POST", "/api/transactions/verify", op, new { qrToken = oldQr["qrToken"]!.GetValue<string>() }, 409);
    var newQr = await Generate(changedId);
    Check(oldQr["qrToken"]!.GetValue<string>() != newQr["qrToken"]!.GetValue<string>(), "changed reservation gets new token");
    await Expect("old token gone after regeneration", "POST", "/api/transactions/verify", op, new { qrToken = oldQr["qrToken"]!.GetValue<string>() }, 404);
    var pendingId = await Seed();
    await reservations.UpdateOneAsync(x => x.ReservationId == pendingId, Builders<EnergyReservation>.Update.Set(x => x.HasPendingChange, true));
    await Expect("pending change blocks generation", "POST", "/api/transactions/qr", prosumer, new { reservationId = pendingId }, 409);
    var expiredId = await Seed(); var expiredQr = await Generate(expiredId);
    await reservations.UpdateOneAsync(x => x.ReservationId == expiredId, Builders<EnergyReservation>.Update.Set(x => x.Transaction!.ExpiresAt, DateTime.UtcNow.AddSeconds(-1)));
    await Expect("expired token", "POST", "/api/transactions/verify", op, new { qrToken = expiredQr["qrToken"]!.GetValue<string>() }, 409);
    var inactiveId = await Seed();
    await users.UpdateOneAsync(x => x.UserId == "P1", Builders<User>.Update.Set(x => x.AccountStatus, AccountStatus.Inactive));
    await Expect("inactive prosumer generation", "POST", "/api/transactions/qr", prosumer, new { reservationId = inactiveId }, 409);
    await users.UpdateOneAsync(x => x.UserId == "P1", Builders<User>.Update.Set(x => x.AccountStatus, AccountStatus.Active));

    var invalidId = await Seed(); var invalidQr = await Generate(invalidId);
    await reservations.UpdateOneAsync(x => x.ReservationId == invalidId, Builders<EnergyReservation>.Update.Set(x => x.Transaction!.QRStatus, "INVALID"));
    await Expect("invalid QR state", "POST", "/api/transactions/verify", op, new { qrToken = invalidQr["qrToken"]!.GetValue<string>() }, 409);
    await Expect("invalid transaction cannot complete", "POST", $"/api/transactions/{invalidQr["transactionId"]!.GetValue<string>()}/complete", op, null, 409);
    await Expect("invalid transaction cannot regenerate", "POST", "/api/transactions/qr", prosumer, new { reservationId = invalidId }, 409);
    var cancelledAfterVerify = await Seed(); var cancelQr = await Generate(cancelledAfterVerify);
    await Expect("verify before cancellation", "POST", "/api/transactions/verify", op, new { qrToken = cancelQr["qrToken"]!.GetValue<string>() }, 200);
    await reservations.UpdateOneAsync(x => x.ReservationId == cancelledAfterVerify, Builders<EnergyReservation>.Update.Set(x => x.Status, BookingStatus.CANCELLED));
    await Expect("cancelled reservation blocks completion", "POST", $"/api/transactions/{cancelQr["transactionId"]!.GetValue<string>()}/complete", op, null, 409);
    await Expect("backoffice cannot complete", "POST", $"/api/transactions/{tid}/complete", backoffice, null, 403);
    await users.UpdateOneAsync(x => x.UserId == "OP1", Builders<User>.Update.Set(x => x.AccountStatus, AccountStatus.Inactive));
    await Expect("inactive operator dashboard", "GET", "/api/operator/dashboard", op, null, 403);
    await users.UpdateOneAsync(x => x.UserId == "OP1", Builders<User>.Update.Set(x => x.AccountStatus, AccountStatus.Active));

    var mine = await Expect("existing reservation endpoint still works", "GET", "/api/bookings/reservations/my", prosumer, null, 200);
    Check(!mine!.ToJsonString().Contains("qrToken", StringComparison.OrdinalIgnoreCase) && !mine.ToJsonString().Contains(token), "QR tokens absent from reservation lists");
    var dashboard = await Expect("operator dashboard", "GET", "/api/operator/dashboard", op, null, 200);
    Check(dashboard!["completedBookings"]!.GetValue<int>() >= 2 && dashboard["stations"]!.AsArray().Count == 1, "dashboard actual counts and assigned stations");
    await Expect("availability wrong operator", "PATCH", "/api/stations/ST1/availability", op2, new { slotId = "SL1", totalCapacity = 120 }, 403);
    await Expect("availability wrong role", "PATCH", "/api/stations/ST1/availability", prosumer, new { slotId = "SL1", totalCapacity = 120 }, 403);
    await Expect("availability below reserved", "PATCH", "/api/stations/ST1/availability", op, new { slotId = "SL1", totalCapacity = 1 }, 400);
    var available = await Expect("availability capacity update", "PATCH", "/api/stations/ST1/availability", op, new { slotId = "SL1", totalCapacity = 120 }, 200);
    Check(available!["slot"]!["availableCapacity"]!.GetValue<int>() == 70, "availability preserves 50 reserved units");
    Check((await stations.Find(x => x.StationId == "ST1").FirstAsync()).Name == "Assigned station", "availability leaves station config intact");
    await Expect("existing logout", "POST", "/api/auth/logout", op, null, 200);
    await Expect("logged-out token rejected", "GET", "/api/operator/dashboard", op, null, 401);
    Console.WriteLine($"PASS: {assertions} assertions against real HTTP/JWT/MongoDB. One-time completion verified.");

    if (args.Contains("--android"))
    {
        var deviceId = await Seed();
        var deviceQr = await Generate(deviceId);
        var displayReservationId = await Seed();
        Directory.CreateDirectory("artifacts");
        await File.WriteAllTextAsync("artifacts/operator-device-fixture.json", System.Text.Json.JsonSerializer.Serialize(new
        {
            password, qrToken = deviceQr["qrToken"]!.GetValue<string>(), reservationId = deviceId,
            displayReservationId
        }));
        Console.WriteLine("Device fixture ready on port 5159. Create artifacts/operator-device-stop to stop and clean up.");
        while (!File.Exists("artifacts/operator-device-stop")) await Task.Delay(1000);
        File.Delete("artifacts/operator-device-fixture.json");
        File.Delete("artifacts/operator-device-stop");
    }

    async Task<string> Seed(BookingStatus status = BookingStatus.CONFIRMED)
    {
        var r = new EnergyReservation { ReservationId = "RES-" + Guid.NewGuid().ToString("N"),
            StationId = "ST1", SlotId = "SL1", ProsumerUserId = "P1", Status = status,
            ScheduledStartTime = start, ScheduledEndTime = start.AddHours(1), EnergyAmountKwh = 5 };
        await reservations.InsertOneAsync(r); return r.ReservationId;
    }
    async Task<JsonNode> Generate(string id) => (await Expect("generate fixture QR", "POST", "/api/transactions/qr", prosumer, new { reservationId = id }, 201))!;
}
finally
{
    if (api is { HasExited: false }) { api.Kill(true); await api.WaitForExitAsync(); }
    api?.Dispose();
    // Only the unique database created by this runner is removed.
    await client.DropDatabaseAsync(dbName);
}

async Task<string> Login(string id)
{
    var result = await Expect("existing login " + id, "POST", "/api/auth/login", null, new { identifier = id, password }, 200);
    return result!["token"]!.GetValue<string>();
}
async Task<(int Code, JsonNode? Body)> Send(string method, string path, string? token, object? body)
{
    using var request = new HttpRequestMessage(new HttpMethod(method), path);
    if (token != null) request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", token);
    if (body != null) request.Content = JsonContent.Create(body);
    using var response = await http.SendAsync(request);
    var content = await response.Content.ReadAsStringAsync();
    return ((int)response.StatusCode, string.IsNullOrWhiteSpace(content) ? null : JsonNode.Parse(content));
}
async Task<JsonNode?> Expect(string name, string method, string path, string? token, object? body, int expected)
{
    var result = await Send(method, path, token, body);
    Check(result.Code == expected, $"{name}: expected {expected}, got {result.Code}");
    return result.Body;
}
void Check(bool passed, string name)
{
    if (!passed) throw new Exception("FAIL: " + name);
    assertions++; Console.WriteLine("PASS: " + name);
}
