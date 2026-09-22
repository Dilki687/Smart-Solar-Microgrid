using System.Text;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.IdentityModel.Tokens;
using Microsoft.OpenApi.Models;
using SmartSolarMicrogrid.API.Services;

var builder = WebApplication.CreateBuilder(args);

// Add MVC controllers to the application.
builder.Services.AddControllers();

// Register MongoDB access as a singleton service.
builder.Services.AddSingleton<MongoDbService>();

// Register password hashing service.
builder.Services.AddSingleton<PasswordService>();

// Register token blacklist service.
builder.Services.AddSingleton<TokenBlacklistService>();

// Register authentication service.
builder.Services.AddScoped<AuthService>();

// Register user and prosumer business logic service.
builder.Services.AddScoped<UserService>();

// Read JWT configuration from application configuration and User Secrets.
var jwtKey = builder.Configuration["Jwt:Key"];
var jwtIssuer = builder.Configuration["Jwt:Issuer"];
var jwtAudience = builder.Configuration["Jwt:Audience"];

if (string.IsNullOrWhiteSpace(jwtKey))
{
    throw new InvalidOperationException(
        "Jwt:Key is not configured.");
}

// Configure JWT Bearer authentication.
builder.Services
    .AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
    .AddJwtBearer(options =>
    {
        options.TokenValidationParameters =
            new TokenValidationParameters
            {
                ValidateIssuerSigningKey = true,
                IssuerSigningKey =
                    new SymmetricSecurityKey(
                        Encoding.UTF8.GetBytes(jwtKey)),

                ValidateIssuer = true,
                ValidIssuer = jwtIssuer,

                ValidateAudience = true,
                ValidAudience = jwtAudience,

                ValidateLifetime = true,

                ClockSkew = TimeSpan.Zero
            };

        options.Events = new JwtBearerEvents
        {
            OnTokenValidated = context =>
            {
                var blacklistService =
                    context.HttpContext.RequestServices
                        .GetRequiredService<TokenBlacklistService>();

                var tokenId =
                    context.Principal?
                        .FindFirst(
                            System.IdentityModel.Tokens.Jwt.JwtRegisteredClaimNames.Jti)
                        ?.Value;

                if (!string.IsNullOrWhiteSpace(tokenId) &&
                    blacklistService.IsBlacklisted(tokenId))
                {
                    context.Fail("Token has been logged out.");
                }

                return Task.CompletedTask;
            }
        };
    });

// Configure role-based authorization.
builder.Services.AddAuthorization();

// Enable endpoint discovery.
builder.Services.AddEndpointsApiExplorer();

// Configure Swagger.
builder.Services.AddSwaggerGen(options =>
{
    options.AddSecurityDefinition(
        "Bearer",
        new OpenApiSecurityScheme
        {
            Name = "Authorization",
            Type = SecuritySchemeType.Http,
            Scheme = "bearer",
            BearerFormat = "JWT",
            In = ParameterLocation.Header,
            Description =
                "Enter JWT token as: Bearer {your token}"
        });

    options.AddSecurityRequirement(
        new OpenApiSecurityRequirement
        {
            {
                new OpenApiSecurityScheme
                {
                    Reference =
                        new OpenApiReference
                        {
                            Type = ReferenceType.SecurityScheme,
                            Id = "Bearer"
                        }
                },
                Array.Empty<string>()
            }
        });
});

var app = builder.Build();

// Enable Swagger during development.
if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

// Redirect HTTP requests to HTTPS when HTTPS is configured.
app.UseHttpsRedirection();

// Enable authentication before authorization.
app.UseAuthentication();

// Enable role-based authorization.
app.UseAuthorization();

// Map controller endpoints.
app.MapControllers();

app.Run();