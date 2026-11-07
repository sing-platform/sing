using System.Net.Http.Headers;
using System.Text.Json;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;
using Sing.Finance.Application.Configuration;
using Sing.Finance.Application.Contracts;

namespace Sing.Finance.Infrastructure.Core;

public sealed class HttpStudentLookup : IStudentLookup
{
    private readonly HttpClient _http;
    private readonly ILogger<HttpStudentLookup> _log;

    public HttpStudentLookup(HttpClient http, IOptions<CoreOptions> options, ILogger<HttpStudentLookup> log)
    {
        _http = http;
        _log = log;
        _http.BaseAddress = new Uri(options.Value.BaseUrl.TrimEnd('/') + "/");
        _http.Timeout = TimeSpan.FromSeconds(options.Value.TimeoutSeconds);
    }

    public async Task<StudentInfo?> FindAsync(long studentId, string? bearerToken, CancellationToken ct)
    {
        using var request = new HttpRequestMessage(HttpMethod.Get, $"api/v1/students/{studentId}");
        if (!string.IsNullOrWhiteSpace(bearerToken))
        {
            request.Headers.Authorization = AuthenticationHeaderValue.Parse(bearerToken);
        }

        try
        {
            using var response = await _http.SendAsync(request, ct);
            if (!response.IsSuccessStatusCode)
            {
                _log.LogWarning("core respondeu {Status} ao buscar aluno {StudentId}", (int)response.StatusCode, studentId);
                return null;
            }

            await using var stream = await response.Content.ReadAsStreamAsync(ct);
            using var doc = await JsonDocument.ParseAsync(stream, cancellationToken: ct);
            var root = doc.RootElement;
            var id = root.TryGetProperty("id", out var idProp) ? idProp.GetInt64() : studentId;
            var name = root.TryGetProperty("name", out var nameProp)
                ? nameProp.GetString() ?? ""
                : "";
            return new StudentInfo(id, name);
        }
        catch (Exception ex)
        {
            _log.LogWarning(ex, "core indisponível ao buscar aluno {StudentId}", studentId);
            return null;
        }
    }
}
