using System.Text.Json.Serialization;
using Sing.Finance.Api;
using Sing.Finance.Infrastructure;

var builder = WebApplication.CreateBuilder(args);

builder.Services.ConfigureHttpJsonOptions(options =>
{
    options.SerializerOptions.Converters.Add(new JsonStringEnumConverter());
});

builder.Services.AddFinanceJwt(builder.Configuration);
builder.Services.AddFinance(builder.Configuration);

var app = builder.Build();

await app.Services.EnsureFinanceSchemaAsync();

app.UseAuthentication();
app.UseAuthorization();

app.MapHealthEndpoints();
app.MapCobrancaEndpoints();
app.MapSponsorEndpoints();
app.MapFinancialEntryEndpoints();
app.MapFiscalDocumentEndpoints();

app.Run();
