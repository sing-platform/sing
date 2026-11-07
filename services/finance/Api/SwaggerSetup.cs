using Microsoft.OpenApi.Models;
using Sing.Finance.Api.Swagger;

namespace Sing.Finance.Api;

public static class SwaggerSetup
{
    public static IServiceCollection AddFinanceSwagger(this IServiceCollection services)
    {
        services.AddEndpointsApiExplorer();
        services.AddSwaggerGen(options =>
        {
            options.SwaggerDoc("v1", new OpenApiInfo
            {
                Title = "SING Finance",
                Version = "v1",
                Description =
                    """
                    Microsserviço financeiro (.NET 8 + MySQL).

                    ## Categorias
                    - **Cobranças (N2)** — `POST /cobrancas`, idempotente por matrícula
                    - **Padrinhos** — apadrinhamento e geração automática de recebíveis
                    - **Lançamentos** — contas a pagar/receber e baixa
                    - **Documentos fiscais** — NFe, recibos e afins
                    - **Ops** — health

                    ## Autenticação
                    Rotas `/api/v1/finance/*` exigem JWT HS256 emitido pelo **core**
                    (`ROLE_ADMIN` ou `ROLE_MANAGER`). Use **Authorize** e cole o token.

                    `POST /cobrancas` e o health **não** pedem token (rede interna / ops).
                    """
            });

            options.TagActionsBy(api =>
            {
                var tag = api.GroupName
                    ?? api.ActionDescriptor.EndpointMetadata
                        .OfType<Microsoft.AspNetCore.Http.Metadata.ITagsMetadata>()
                        .FirstOrDefault()?.Tags.FirstOrDefault();
                return tag is null ? ["Outros"] : [tag];
            });

            options.DocInclusionPredicate((_, _) => true);
            options.OrderActionsBy(api => $"{api.GroupName}_{api.RelativePath}_{api.HttpMethod}");
            options.OperationFilter<AllowAnonymousOperationFilter>();
            options.DocumentFilter<TagOrderDocumentFilter>();

            options.AddSecurityDefinition("Bearer", new OpenApiSecurityScheme
            {
                Description = "JWT do core. Cole apenas o token (Swagger adiciona `Bearer`).",
                Name = "Authorization",
                In = ParameterLocation.Header,
                Type = SecuritySchemeType.Http,
                Scheme = "bearer",
                BearerFormat = "JWT"
            });

            options.AddSecurityRequirement(new OpenApiSecurityRequirement
            {
                {
                    new OpenApiSecurityScheme
                    {
                        Reference = new OpenApiReference
                        {
                            Type = ReferenceType.SecurityScheme,
                            Id = "Bearer"
                        }
                    },
                    Array.Empty<string>()
                }
            });
        });

        return services;
    }

    public static WebApplication UseFinanceSwagger(this WebApplication app)
    {
        app.UseSwagger(options => options.RouteTemplate = "finance/swagger/{documentName}/swagger.json");
        app.UseSwaggerUI(options =>
        {
            options.SwaggerEndpoint("/finance/swagger/v1/swagger.json", "SING Finance v1");
            options.RoutePrefix = "finance/swagger";
            options.DocumentTitle = "SING Finance";
            options.DisplayRequestDuration();
            options.EnableTryItOutByDefault();
            options.DefaultModelsExpandDepth(1);
        });
        return app;
    }
}
