using Sing.Finance.Application.Contracts;
using Sing.Finance.Application.Dtos;
using Sing.Finance.Application.Services;
using Sing.Finance.Domain;
using Xunit;

namespace Sing.Finance.Tests;

public class SponsorServiceTests
{
    [Fact]
    public async Task Create_quando_aluno_existe_retorna_201()
    {
        var students = new MapStudentLookup();
        students.Add(10, "Ana");
        var service = new SponsorService(new FakeSponsorRepository(), students, new FakeEntryRepository());

        var result = await service.CreateAsync(
            new SponsorCreateRequest(10, "Padrinho", null, null, 50m, SponsorFrequency.MONTHLY, new DateOnly(2026, 1, 1)),
            bearer: null,
            userId: 1,
            CancellationToken.None);

        Assert.True(result.IsSuccess);
        Assert.Equal(201, result.StatusCode);
        Assert.Equal("Ana", result.Value!.StudentName);
    }

    [Fact]
    public async Task Create_quando_aluno_nao_existe_retorna_404()
    {
        var service = new SponsorService(new FakeSponsorRepository(), new MapStudentLookup(), new FakeEntryRepository());

        var result = await service.CreateAsync(
            new SponsorCreateRequest(99, "Padrinho", null, null, 50m, SponsorFrequency.MONTHLY, new DateOnly(2026, 1, 1)),
            null,
            1,
            CancellationToken.None);

        Assert.False(result.IsSuccess);
        Assert.Equal(404, result.StatusCode);
    }

    [Fact]
    public async Task GetById_inexistente_retorna_404()
    {
        var service = new SponsorService(new FakeSponsorRepository(), new MapStudentLookup(), new FakeEntryRepository());

        var result = await service.GetByIdAsync(1, CancellationToken.None);

        Assert.Equal(404, result.StatusCode);
    }

    [Fact]
    public async Task Delete_desativa_padrinho()
    {
        var repo = new FakeSponsorRepository();
        var students = new MapStudentLookup();
        students.Add(10, "Ana");
        var service = new SponsorService(repo, students, new FakeEntryRepository());
        var created = await service.CreateAsync(
            new SponsorCreateRequest(10, "Padrinho", null, null, 50m, SponsorFrequency.MONTHLY, new DateOnly(2026, 1, 1)),
            null,
            1,
            CancellationToken.None);

        var deleted = await service.DeleteAsync(created.Value!.Id, 1, CancellationToken.None);

        Assert.True(deleted.IsSuccess);
        Assert.Equal(204, deleted.StatusCode);
        Assert.False((await repo.GetByIdAsync(created.Value.Id, CancellationToken.None))!.IsActive);
    }

    private sealed class MapStudentLookup : IStudentLookup
    {
        private readonly Dictionary<long, StudentInfo> _map = new();

        public void Add(long id, string name) => _map[id] = new StudentInfo(id, name);

        public Task<StudentInfo?> FindAsync(long studentId, string? bearerToken, CancellationToken ct) =>
            Task.FromResult(_map.TryGetValue(studentId, out var s) ? s : null);
    }

    private sealed class FakeSponsorRepository : ISponsorRepository
    {
        private readonly List<Sponsor> _items = [];
        private long _nextId = 1;

        public Task<List<Sponsor>> ListAsync(CancellationToken ct) =>
            Task.FromResult(_items.Where(x => x.IsActive).OrderBy(x => x.Name).ToList());

        public Task<Sponsor?> GetByIdAsync(long id, CancellationToken ct) =>
            Task.FromResult(_items.FirstOrDefault(x => x.Id == id));

        public Task<List<Sponsor>> ListActiveAsync(CancellationToken ct) =>
            Task.FromResult(_items.Where(x => x.IsActive).ToList());

        public Task AddAsync(Sponsor sponsor, CancellationToken ct)
        {
            sponsor.Id = _nextId++;
            _items.Add(sponsor);
            return Task.CompletedTask;
        }

        public Task SaveChangesAsync(CancellationToken ct) => Task.CompletedTask;
    }

    private sealed class FakeEntryRepository : IFinancialEntryRepository
    {
        public Task AddAsync(FinancialEntry entry, CancellationToken ct) => Task.CompletedTask;

        public Task<bool> ExistsSponsorDueDateAsync(long sponsorId, DateOnly dueDate, CancellationToken ct) =>
            Task.FromResult(false);

        public Task<List<FinancialEntry>> FindPendingOverdueAsync(DateOnly referenceDate, CancellationToken ct) =>
            Task.FromResult(new List<FinancialEntry>());

        public Task<FinancialEntry?> GetByIdAsync(long id, CancellationToken ct) =>
            Task.FromResult<FinancialEntry?>(null);

        public Task<List<FinancialEntry>> ListBySponsorAsync(long sponsorId, CancellationToken ct) =>
            Task.FromResult(new List<FinancialEntry>());

        public Task SaveChangesAsync(CancellationToken ct) => Task.CompletedTask;

        public Task<List<FinancialEntry>> SearchAsync(
            DateOnly? start,
            DateOnly? finish,
            EntryDirection? direction,
            EntryStatus? status,
            string? category,
            CancellationToken ct) =>
            Task.FromResult(new List<FinancialEntry>());
    }
}
