namespace Sing.Finance.Domain;

public enum SponsorFrequency
{
    Monthly,
    Bimonthly,
    Quarterly,
    Semiannual,
    Annual
}

public static class SponsorFrequencyExtensions
{
    public static DateOnly NextDueDate(this SponsorFrequency frequency, DateOnly from)
    {
        var months = frequency switch
        {
            SponsorFrequency.Monthly => 1,
            SponsorFrequency.Bimonthly => 2,
            SponsorFrequency.Quarterly => 3,
            SponsorFrequency.Semiannual => 6,
            SponsorFrequency.Annual => 12,
            _ => throw new ArgumentOutOfRangeException(nameof(frequency))
        };

        return from.AddMonths(months);
    }
}
