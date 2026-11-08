using Sing.Finance.Application.Contracts;
using Sing.Finance.Application.Dtos;
using Sing.Finance.Application.Services;
using Sing.Finance.Domain;
using Xunit;

namespace Sing.Finance.Tests;

public class FinancialEntryServiceTests
{
    [Fact]
    public async Task Create_retorna_201()
    {
        var service = new FinancialEntryService(new FakeEntries(), new FakeSponsors(), new FakeFiscals());

        var result = await service.CreateAsync(
            new FinancialEntryCreateRequest(
                EntryDirection.RECEIVABLE,
                EntryStatus.PENDING,
                new DateOnly(2026, 11, 1),
                null,
                100m,
                "Mensalidade",
                null,
                null,
                null),
            userId: 1,
            CancellationToken.None);

        Assert.True(result.IsSuccess);
        Assert.Equal(201, result.StatusCode);
    }

    [Fact]
    public async Task Create_com_sponsor_inexistente_retorna_404()
    {
        var service = new FinancialEntryService(new FakeEntries(), new FakeSponsors(), new FakeFiscals());

        var result = await service.CreateAsync(
            new FinancialEntryCreateRequest(
                EntryDirection.RECEIVABLE,
                EntryStatus.PENDING,
                new DateOnly(2026, 11, 1),
                null,
                100m,
                "Mensalidade",
                null,
                99,
                null),
            1,
            CancellationToken.None);

        Assert.Equal(404, result.StatusCode);
    }

    [Fact]
    public async Task MonthSummary_soma_receitas_e_despesas()
    {
        var entries = new FakeEntries();
        var service = new FinancialEntryService(entries, new FakeSponsors(), new FakeFiscals());
        await service.CreateAsync(
            new FinancialEntryCreateRequest(
                EntryDirection.RECEIVABLE, EntryStatus.PENDING, new DateOnly(2026, 11, 10), null, 100m, "A", null, null, null),
            1, CancellationToken.None);
        await service.CreateAsync(
            new FinancialEntryCreateRequest(
                EntryDirection.PAYABLE, EntryStatus.PENDING, new DateOnly(2026, 11, 12), null, 40m, "B", null, null, null),
            1, CancellationToken.None);

        var summary = await service.MonthSummaryAsync("2026-11", CancellationToken.None);

        Assert.True(summary.IsSuccess);
        Assert.Equal(100m, summary.Value!.Income);
        Assert.Equal(40m, summary.Value.Expense);
        Assert.Equal(60m, summary.Value.Balance);
    }

    [Fact]
    public async Task MarkAsPaid_atualiza_status()
    {
        var service = new FinancialEntryService(new FakeEntries(), new FakeSponsors(), new FakeFiscals());
        var created = await service.CreateAsync(
            new FinancialEntryCreateRequest(
                EntryDirection.RECEIVABLE, EntryStatus.PENDING, new DateOnly(2026, 11, 1), null, 100m, "A", null, null, null),
            1, CancellationToken.None);

        var paid = await service.MarkAsPaidAsync(created.Value!.Id, new DateOnly(2026, 11, 2), CancellationToken.None);

        Assert.True(paid.IsSuccess);
        Assert.Equal(EntryStatus.PAID, paid.Value!.Status);
        Assert.Equal(new DateOnly(2026, 11, 2), paid.Value.PaidAt);
    }

    [Fact]
    public async Task List_mes_invalido_retorna_400()
    {
        var service = new FinancialEntryService(new FakeEntries(), new FakeSponsors(), new FakeFiscals());

        var result = await service.ListAsync("2026-13", null, null, null, CancellationToken.None);

        Assert.Equal(400, result.StatusCode);
    }

    private sealed class FakeEntries : IFinancialEntryRepository
    {
        private readonly List<FinancialEntry> _items = [];
        private long _nextId = 1;

        public Task AddAsync(FinancialEntry entry, CancellationToken ct)
        {
            entry.Id = _nextId++;
            _items.Add(entry);
            return Task.CompletedTask;
        }

        public Task<bool> ExistsSponsorDueDateAsync(long sponsorId, DateOnly dueDate, CancellationToken ct) =>
            Task.FromResult(_items.Any(x => x.SponsorId == sponsorId && x.DueDate == dueDate && x.IsActive));

        public Task<List<FinancialEntry>> FindPendingOverdueAsync(DateOnly referenceDate, CancellationToken ct) =>
            Task.FromResult(_items.Where(x => x.IsActive && x.Status == EntryStatus.PENDING && x.DueDate < referenceDate).ToList());

        public Task<FinancialEntry?> GetByIdAsync(long id, CancellationToken ct) =>
            Task.FromResult(_items.FirstOrDefault(x => x.Id == id));

        public Task<List<FinancialEntry>> ListBySponsorAsync(long sponsorId, CancellationToken ct) =>
            Task.FromResult(_items.Where(x => x.SponsorId == sponsorId).ToList());

        public Task SaveChangesAsync(CancellationToken ct) => Task.CompletedTask;

        public Task<List<FinancialEntry>> SearchAsync(
            DateOnly? start,
            DateOnly? finish,
            EntryDirection? direction,
            EntryStatus? status,
            string? category,
            CancellationToken ct)
        {
            IEnumerable<FinancialEntry> query = _items.Where(x => x.IsActive);
            if (start is not null) query = query.Where(x => x.DueDate >= start);
            if (finish is not null) query = query.Where(x => x.DueDate <= finish);
            if (direction is not null) query = query.Where(x => x.Direction == direction);
            if (status is not null) query = query.Where(x => x.Status == status);
            if (!string.IsNullOrWhiteSpace(category))
            {
                var term = category.Trim().ToLowerInvariant();
                query = query.Where(x => x.Category.ToLowerInvariant().Contains(term));
            }

            return Task.FromResult(query.ToList());
        }
    }

    private sealed class FakeSponsors : ISponsorRepository
    {
        public Task AddAsync(Sponsor sponsor, CancellationToken ct) => Task.CompletedTask;
        public Task<Sponsor?> GetByIdAsync(long id, CancellationToken ct) => Task.FromResult<Sponsor?>(null);
        public Task<List<Sponsor>> ListActiveAsync(CancellationToken ct) => Task.FromResult(new List<Sponsor>());
        public Task<List<Sponsor>> ListAsync(CancellationToken ct) => Task.FromResult(new List<Sponsor>());
        public Task SaveChangesAsync(CancellationToken ct) => Task.CompletedTask;
    }

    private sealed class FakeFiscals : IFiscalDocumentRepository
    {
        public Task AddAsync(FiscalDocument document, CancellationToken ct) => Task.CompletedTask;
        public Task<FiscalDocument?> GetByIdAsync(long id, CancellationToken ct) => Task.FromResult<FiscalDocument?>(null);
        public Task<List<FiscalDocument>> ListAsync(FiscalDocumentType? type, string? documentNumber, CancellationToken ct) =>
            Task.FromResult(new List<FiscalDocument>());
        public Task SaveChangesAsync(CancellationToken ct) => Task.CompletedTask;
    }
}
