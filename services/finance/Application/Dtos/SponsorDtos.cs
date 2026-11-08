using Sing.Finance.Domain;

namespace Sing.Finance.Application.Dtos;

public sealed record SponsorCreateRequest(
    long StudentId,
    string Name,
    string? Email,
    string? Phone,
    decimal Amount,
    SponsorFrequency Frequency,
    DateOnly StartDate);

public sealed record SponsorUpdateRequest(
    long StudentId,
    string Name,
    string? Email,
    string? Phone,
    decimal Amount,
    SponsorFrequency Frequency,
    DateOnly StartDate,
    bool Active);

public sealed record SponsorResponse(
    long Id,
    long StudentId,
    string? StudentName,
    string Name,
    string? Email,
    string? Phone,
    decimal Amount,
    SponsorFrequency Frequency,
    DateOnly StartDate,
    DateOnly? LastGeneratedDate,
    bool Active)
{
    public static SponsorResponse From(Sponsor sponsor, string? studentName = null) => new(
        sponsor.Id,
        sponsor.StudentId,
        studentName,
        sponsor.Name,
        sponsor.Email,
        sponsor.Phone,
        sponsor.Amount,
        sponsor.Frequency,
        sponsor.StartDate,
        sponsor.LastGeneratedDate,
        sponsor.IsActive);
}
