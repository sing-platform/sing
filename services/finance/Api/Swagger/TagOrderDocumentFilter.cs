using Microsoft.OpenApi.Models;
using Swashbuckle.AspNetCore.SwaggerGen;

namespace Sing.Finance.Api.Swagger;

public sealed class TagOrderDocumentFilter : IDocumentFilter
{
    private static readonly string[] Order =
    [
        "Cobranças (N2)",
        "Padrinhos",
        "Lançamentos",
        "Documentos fiscais",
        "Ops"
    ];

    public void Apply(OpenApiDocument swaggerDoc, DocumentFilterContext context)
    {
        swaggerDoc.Tags = Order
            .Select(name => new OpenApiTag
            {
                Name = name,
                Description = name switch
                {
                    "Cobranças (N2)" => "Contrato da matrícula criada. O core chama sem JWT na rede interna. Idempotente por `matriculaId`.",
                    "Padrinhos" => "Apadrinhamento de alunos: cadastro, atualização e cobranças geradas automaticamente.",
                    "Lançamentos" => "Contas a pagar/receber, baixa, resumo mensal e soft delete.",
                    "Documentos fiscais" => "Notas, recibos e faturas vinculados a lançamentos.",
                    "Ops" => "Health check no mesmo caminho do Actuator dos outros serviços.",
                    _ => null
                }
            })
            .ToList();
    }
}
