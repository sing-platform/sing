namespace Sing.Finance.Application.Contracts;

public interface IStudentLookup
{
    Task<StudentInfo?> FindAsync(long studentId, string? bearerToken, CancellationToken ct);
}

public sealed record StudentInfo(long Id, string Name);
