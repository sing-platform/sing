namespace Sing.Finance.Domain;

public enum SponsorFrequency
{
    MONTHLY,
    BIMONTHLY,
    QUARTERLY,
    SEMIANNUAL,
    ANNUAL
}

public static class SponsorFrequencyExtensions
{
    public static DateOnly NextDueDate(this SponsorFrequency frequency, DateOnly from)
    {
        var months = frequency switch
        {
            SponsorFrequency.MONTHLY => 1,
            SponsorFrequency.BIMONTHLY => 2,
            SponsorFrequency.QUARTERLY => 3,
            SponsorFrequency.SEMIANNUAL => 6,
            SponsorFrequency.ANNUAL => 12,
            _ => throw new ArgumentOutOfRangeException(nameof(frequency))
        };

        return from.AddMonths(months);
    }
}
