using Sing.Finance.Application.Contracts;
using Sing.Finance.Application.Dtos;
using Sing.Finance.Application.Services;
using Sing.Finance.Domain;
using Xunit;

namespace Sing.Finance.Tests;

public class CobrancaServiceTests
{
    private static readonly DateOnly Vencimento = new(2026, 11, 15);

    [Fact]
    public async Task Create_primeira_vez_retorna_201()
    {
        var repo = new FakeCobrancaRepository();
        var service = new CobrancaService(repo);

        var result = await service.CreateAsync(new CobrancaCreateRequest(9001, 15000, Vencimento), CancellationToken.None);

        Assert.True(result.IsSuccess);
        Assert.Equal(201, result.StatusCode);
        Assert.Equal("pendente", result.Value!.Status);
        Assert.Equal(1, repo.Count);
    }

    [Fact]
    public async Task Create_replay_idempotente_retorna_200()
    {
        var repo = new FakeCobrancaRepository();
        var service = new CobrancaService(repo);
        var request = new CobrancaCreateRequest(9001, 15000, Vencimento);

        await service.CreateAsync(request, CancellationToken.None);
        var replay = await service.CreateAsync(request, CancellationToken.None);

        Assert.True(replay.IsSuccess);
        Assert.Equal(200, replay.StatusCode);
        Assert.Equal(1, repo.Count);
    }

    [Fact]
    public async Task Create_divergente_retorna_409()
    {
        var repo = new FakeCobrancaRepository();
        var service = new CobrancaService(repo);

        await service.CreateAsync(new CobrancaCreateRequest(9001, 15000, Vencimento), CancellationToken.None);
        var conflict = await service.CreateAsync(new CobrancaCreateRequest(9001, 16000, Vencimento), CancellationToken.None);

        Assert.False(conflict.IsSuccess);
        Assert.Equal(409, conflict.StatusCode);
        Assert.Equal(1, repo.Count);
    }

    [Fact]
    public async Task Create_corrida_mesmo_payload_retorna_200()
    {
        var winner = Cobranca.Create(9001, 15000, Vencimento, DateTime.UtcNow);
        winner.Id = 44;
        var repo = new FakeCobrancaRepository
        {
            SimulateRaceOnSave = true,
            WinnerAfterConflict = winner
        };

        var service = new CobrancaService(repo);
        var result = await service.CreateAsync(new CobrancaCreateRequest(9001, 15000, Vencimento), CancellationToken.None);

        Assert.True(result.IsSuccess);
        Assert.Equal(200, result.StatusCode);
        Assert.Equal(44, result.Value!.Id);
    }

    [Fact]
    public async Task Create_corrida_payload_divergente_retorna_409()
    {
        var winner = Cobranca.Create(9001, 15000, Vencimento, DateTime.UtcNow);
        winner.Id = 44;
        var repo = new FakeCobrancaRepository
        {
            SimulateRaceOnSave = true,
            WinnerAfterConflict = winner
        };

        var service = new CobrancaService(repo);
        var result = await service.CreateAsync(new CobrancaCreateRequest(9001, 99999, Vencimento), CancellationToken.None);

        Assert.False(result.IsSuccess);
        Assert.Equal(409, result.StatusCode);
    }

    [Fact]
    public async Task Create_vencimento_invalido_retorna_400()
    {
        var service = new CobrancaService(new FakeCobrancaRepository());

        var result = await service.CreateAsync(
            new CobrancaCreateRequest(9001, 15000, new DateOnly(1999, 1, 1)),
            CancellationToken.None);

        Assert.False(result.IsSuccess);
        Assert.Equal(400, result.StatusCode);
    }

    private sealed class FakeCobrancaRepository : ICobrancaRepository
    {
        private readonly List<Cobranca> _items = [];
        private Cobranca? _pending;
        private long _nextId = 1;

        public bool SimulateRaceOnSave { get; init; }
        public Cobranca? WinnerAfterConflict { get; init; }

        public int Count => _items.Count;

        public Task<Cobranca?> GetByMatriculaIdAsync(long matriculaId, CancellationToken ct) =>
            Task.FromResult(_items.FirstOrDefault(x => x.MatriculaId == matriculaId));

        public Task AddAsync(Cobranca cobranca, CancellationToken ct)
        {
            _pending = cobranca;
            return Task.CompletedTask;
        }

        public Task SaveChangesAsync(CancellationToken ct)
        {
            if (SimulateRaceOnSave)
            {
                if (WinnerAfterConflict is not null
                    && _items.All(x => x.MatriculaId != WinnerAfterConflict.MatriculaId))
                {
                    _items.Add(WinnerAfterConflict);
                }

                throw new CobrancaConflictException();
            }

            if (_pending is null)
            {
                return Task.CompletedTask;
            }

            if (_items.Any(x => x.MatriculaId == _pending.MatriculaId))
            {
                throw new CobrancaConflictException();
            }

            _pending.Id = _nextId++;
            _items.Add(_pending);
            _pending = null;
            return Task.CompletedTask;
        }

        public void DiscardChanges() => _pending = null;
    }
}
