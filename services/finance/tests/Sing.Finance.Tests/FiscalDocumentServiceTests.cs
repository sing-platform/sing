using Sing.Finance.Application.Contracts;
using Sing.Finance.Application.Dtos;
using Sing.Finance.Application.Services;
using Sing.Finance.Domain;
using Xunit;

namespace Sing.Finance.Tests;

public class FiscalDocumentServiceTests
{
    [Fact]
    public async Task Create_retorna_201()
    {
        var service = new FiscalDocumentService(new FakeFiscals());

        var result = await service.CreateAsync(
            new FiscalDocumentCreateRequest(
                FiscalDocumentType.NFE,
                "NF-1",
                new DateOnly(2026, 11, 1),
                150m,
                "Fornecedor",
                "00",
                "https://files/nf.pdf"),
            userId: 1,
            CancellationToken.None);

        Assert.True(result.IsSuccess);
        Assert.Equal(201, result.StatusCode);
        Assert.Equal("NF-1", result.Value!.DocumentNumber);
    }

    [Fact]
    public async Task GetById_inexistente_retorna_404()
    {
        var service = new FiscalDocumentService(new FakeFiscals());

        var result = await service.GetByIdAsync(9, CancellationToken.None);

        Assert.Equal(404, result.StatusCode);
    }

    [Fact]
    public async Task Update_e_delete_funcionam()
    {
        var repo = new FakeFiscals();
        var service = new FiscalDocumentService(repo);
        var created = await service.CreateAsync(
            new FiscalDocumentCreateRequest(
                FiscalDocumentType.NFE, "NF-1", new DateOnly(2026, 11, 1), 150m, null, null, null),
            1,
            CancellationToken.None);

        var updated = await service.UpdateAsync(
            created.Value!.Id,
            new FiscalDocumentUpdateRequest(
                FiscalDocumentType.RECEIPT, "NF-2", new DateOnly(2026, 11, 2), 160m, null, null, null),
            CancellationToken.None);

        Assert.True(updated.IsSuccess);
        Assert.Equal("NF-2", updated.Value!.DocumentNumber);

        var deleted = await service.DeleteAsync(created.Value.Id, 1, CancellationToken.None);
        Assert.True(deleted.IsSuccess);
        Assert.False((await repo.GetByIdAsync(created.Value.Id, CancellationToken.None))!.IsActive);
    }

    [Fact]
    public async Task Create_valor_invalido_retorna_400()
    {
        var service = new FiscalDocumentService(new FakeFiscals());

        var result = await service.CreateAsync(
            new FiscalDocumentCreateRequest(
                FiscalDocumentType.NFE, "NF-1", new DateOnly(2026, 11, 1), 0m, null, null, null),
            1,
            CancellationToken.None);

        Assert.Equal(400, result.StatusCode);
    }

    private sealed class FakeFiscals : IFiscalDocumentRepository
    {
        private readonly List<FiscalDocument> _items = [];
        private long _nextId = 1;

        public Task AddAsync(FiscalDocument document, CancellationToken ct)
        {
            document.Id = _nextId++;
            _items.Add(document);
            return Task.CompletedTask;
        }

        public Task<FiscalDocument?> GetByIdAsync(long id, CancellationToken ct) =>
            Task.FromResult(_items.FirstOrDefault(x => x.Id == id));

        public Task<List<FiscalDocument>> ListAsync(FiscalDocumentType? type, string? documentNumber, CancellationToken ct)
        {
            IEnumerable<FiscalDocument> query = _items.Where(x => x.IsActive);
            if (type is not null) query = query.Where(x => x.DocumentType == type);
            if (!string.IsNullOrWhiteSpace(documentNumber))
            {
                var term = documentNumber.Trim().ToLowerInvariant();
                query = query.Where(x => x.DocumentNumber.ToLowerInvariant().Contains(term));
            }

            return Task.FromResult(query.ToList());
        }

        public Task SaveChangesAsync(CancellationToken ct) => Task.CompletedTask;
    }
}
