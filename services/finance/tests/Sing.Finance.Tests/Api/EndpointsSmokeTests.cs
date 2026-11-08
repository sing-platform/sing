using System.Net;
using System.Net.Http.Headers;
using System.Net.Http.Json;
using System.Text.Json;
using Sing.Finance.Tests.Support;
using Xunit;

namespace Sing.Finance.Tests.Api;

public sealed class EndpointsSmokeTests : IClassFixture<FinanceApiFactory>
{
    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        PropertyNameCaseInsensitive = true
    };

    private readonly FinanceApiFactory _factory;
    private readonly HttpClient _client;

    public EndpointsSmokeTests(FinanceApiFactory factory)
    {
        _factory = factory;
        _client = factory.CreateClient();
        _client.DefaultRequestHeaders.Authorization =
            new AuthenticationHeaderValue("Bearer", TestJwt.CreateAdmin());
        _factory.Students.Add(10, "Aluno Teste");
    }

    [Fact]
    public async Task Health_retorna_UP()
    {
        var response = await _client.GetAsync("/actuator/health");

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        var body = await response.Content.ReadFromJsonAsync<JsonElement>(JsonOptions);
        Assert.Equal("UP", body.GetProperty("status").GetString());
    }

    [Fact]
    public async Task Cobrancas_ciclo_idempotente_e_conflito()
    {
        var anon = _factory.CreateClient();
        var matriculaId = Random.Shared.NextInt64(100_000, 999_999);
        var payload = new
        {
            matriculaId,
            valorCentavos = 15000,
            vencimento = "2026-11-15"
        };

        var created = await anon.PostAsJsonAsync("/cobrancas", payload);
        Assert.Equal(HttpStatusCode.Created, created.StatusCode);

        var replay = await anon.PostAsJsonAsync("/cobrancas", payload);
        Assert.Equal(HttpStatusCode.OK, replay.StatusCode);

        var conflict = await anon.PostAsJsonAsync("/cobrancas", new
        {
            matriculaId,
            valorCentavos = 16000,
            vencimento = "2026-11-15"
        });
        Assert.Equal(HttpStatusCode.Conflict, conflict.StatusCode);
    }

    [Fact]
    public async Task Sponsors_crud_completo()
    {
        var create = await _client.PostAsJsonAsync("/api/v1/finance/sponsors", new
        {
            studentId = 10,
            name = "Padrinho API",
            email = "p@sing.local",
            phone = "11999999999",
            amount = 100.50m,
            frequency = "MONTHLY",
            startDate = "2026-01-01"
        });
        Assert.Equal(HttpStatusCode.Created, create.StatusCode);
        var created = await create.Content.ReadFromJsonAsync<JsonElement>(JsonOptions);
        var id = created.GetProperty("id").GetInt64();

        var list = await _client.GetAsync("/api/v1/finance/sponsors");
        Assert.Equal(HttpStatusCode.OK, list.StatusCode);

        var get = await _client.GetAsync($"/api/v1/finance/sponsors/{id}");
        Assert.Equal(HttpStatusCode.OK, get.StatusCode);

        var entries = await _client.GetAsync($"/api/v1/finance/sponsors/{id}/entries");
        Assert.Equal(HttpStatusCode.OK, entries.StatusCode);

        var update = await _client.PutAsJsonAsync($"/api/v1/finance/sponsors/{id}", new
        {
            studentId = 10,
            name = "Padrinho Atualizado",
            email = "p@sing.local",
            phone = "11999999999",
            amount = 120m,
            frequency = "MONTHLY",
            startDate = "2026-01-01",
            active = true
        });
        Assert.Equal(HttpStatusCode.OK, update.StatusCode);

        var delete = await _client.DeleteAsync($"/api/v1/finance/sponsors/{id}");
        Assert.Equal(HttpStatusCode.NoContent, delete.StatusCode);
    }

    [Fact]
    public async Task Sponsors_sem_jwt_retorna_401()
    {
        var anon = _factory.CreateClient();
        var response = await anon.GetAsync("/api/v1/finance/sponsors");
        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
    }

    [Fact]
    public async Task Entries_crud_resumo_e_baixa()
    {
        var create = await _client.PostAsJsonAsync("/api/v1/finance/entries", new
        {
            direction = "RECEIVABLE",
            status = "PENDING",
            dueDate = "2026-11-01",
            paidAt = (string?)null,
            amount = 75.25m,
            category = "Mensalidade",
            description = "Teste",
            sponsorId = (long?)null,
            fiscalDocumentId = (long?)null
        });
        Assert.Equal(HttpStatusCode.Created, create.StatusCode);
        var created = await create.Content.ReadFromJsonAsync<JsonElement>(JsonOptions);
        var id = created.GetProperty("id").GetInt64();

        var list = await _client.GetAsync("/api/v1/finance/entries?month=2026-11");
        Assert.Equal(HttpStatusCode.OK, list.StatusCode);

        var summary = await _client.GetAsync("/api/v1/finance/entries/summary?month=2026-11");
        Assert.Equal(HttpStatusCode.OK, summary.StatusCode);

        var get = await _client.GetAsync($"/api/v1/finance/entries/{id}");
        Assert.Equal(HttpStatusCode.OK, get.StatusCode);

        var update = await _client.PutAsJsonAsync($"/api/v1/finance/entries/{id}", new
        {
            direction = "RECEIVABLE",
            status = "PENDING",
            dueDate = "2026-11-05",
            paidAt = (string?)null,
            amount = 80m,
            category = "Mensalidade",
            description = "Atualizado",
            sponsorId = (long?)null,
            fiscalDocumentId = (long?)null,
            active = true
        });
        Assert.Equal(HttpStatusCode.OK, update.StatusCode);

        var pay = await _client.PatchAsJsonAsync($"/api/v1/finance/entries/{id}/pay", new { paidAt = "2026-11-06" });
        Assert.Equal(HttpStatusCode.OK, pay.StatusCode);

        var delete = await _client.DeleteAsync($"/api/v1/finance/entries/{id}");
        Assert.Equal(HttpStatusCode.NoContent, delete.StatusCode);
    }

    [Fact]
    public async Task FiscalDocuments_crud_completo()
    {
        var create = await _client.PostAsJsonAsync("/api/v1/finance/fiscal-documents", new
        {
            documentType = "NFE",
            documentNumber = "NF-100",
            issueDate = "2026-11-01",
            totalAmount = 200m,
            supplierName = "Fornecedor",
            supplierDoc = "123",
            fileUrl = "https://example.local/nf.pdf"
        });
        Assert.Equal(HttpStatusCode.Created, create.StatusCode);
        var created = await create.Content.ReadFromJsonAsync<JsonElement>(JsonOptions);
        var id = created.GetProperty("id").GetInt64();

        var list = await _client.GetAsync("/api/v1/finance/fiscal-documents?type=NFE");
        Assert.Equal(HttpStatusCode.OK, list.StatusCode);

        var get = await _client.GetAsync($"/api/v1/finance/fiscal-documents/{id}");
        Assert.Equal(HttpStatusCode.OK, get.StatusCode);

        var update = await _client.PutAsJsonAsync($"/api/v1/finance/fiscal-documents/{id}", new
        {
            documentType = "RECEIPT",
            documentNumber = "NF-100-B",
            issueDate = "2026-11-02",
            totalAmount = 210m,
            supplierName = "Fornecedor",
            supplierDoc = "123",
            fileUrl = "https://example.local/nf2.pdf"
        });
        Assert.Equal(HttpStatusCode.OK, update.StatusCode);

        var delete = await _client.DeleteAsync($"/api/v1/finance/fiscal-documents/{id}");
        Assert.Equal(HttpStatusCode.NoContent, delete.StatusCode);
    }
}
