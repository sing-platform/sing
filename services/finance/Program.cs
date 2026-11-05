using Sing.Finance.Api;

var builder = WebApplication.CreateBuilder(args);
var app = builder.Build();

app.MapHealthEndpoints();

app.Run();
