using Microsoft.AspNetCore.Mvc;
using MongoDB.Bson;
using MongoDB.Driver;
using SmartSolarMicrogrid.API.Services;

namespace SmartSolarMicrogrid.API.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class HealthController : ControllerBase
    {
        private readonly MongoDbService _mongoDbService;

        public HealthController(MongoDbService mongoDbService)
        {
            _mongoDbService = mongoDbService;
        }

        [HttpGet]
        public async Task<IActionResult> Get()
        {
            try
            {
                var database = _mongoDbService.GetDatabase();

                await database.RunCommandAsync<BsonDocument>(
                    new BsonDocument("ping", 1));

                return Ok(new
                {
                    status = "Healthy",
                    api = "Running",
                    database = "Connected"
                });
            }
            catch (Exception)
            {
                return StatusCode(500, new
                {
                    status = "Unhealthy",
                    api = "Running",
                    database = "Disconnected"
                });
            }
        }
    }
}