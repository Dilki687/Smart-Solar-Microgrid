using MongoDB.Driver;

namespace SmartSolarMicrogrid.API.Services
{
    public class MongoDbService
    {
        private readonly IMongoDatabase _database;

        public MongoDbService(IConfiguration configuration)
        {
            var connectionString =
                configuration["MongoDb:ConnectionString"];

            var databaseName =
                configuration["MongoDb:DatabaseName"];

            if (string.IsNullOrWhiteSpace(connectionString))
            {
                throw new InvalidOperationException(
                    "MongoDb:ConnectionString is not configured.");
            }

            if (string.IsNullOrWhiteSpace(databaseName))
            {
                throw new InvalidOperationException(
                    "MongoDb:DatabaseName is not configured.");
            }

            var client = new MongoClient(connectionString);

            _database = client.GetDatabase(databaseName);
        }

        public IMongoDatabase GetDatabase()
        {
            return _database;
        }
    }
}