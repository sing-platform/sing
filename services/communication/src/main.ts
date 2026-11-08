import "reflect-metadata";
import { NestFactory } from "@nestjs/core";
import { DocumentBuilder, SwaggerModule } from "@nestjs/swagger";
import { AppModule } from "./app.module";

async function bootstrap(): Promise<void> {
  const app = await NestFactory.create(AppModule);
  const config = new DocumentBuilder()
    .setTitle("SING Communication")
    .setDescription(
      "Mural institucional, notificacoes in-app e notificacao da matricula. Esta UI e publica. " +
        "As rotas do mural (/api/v1/mural) e das notificacoes (/api/v1/notifications) exigem JWT do core. " +
        "GET /api/v1/comunicacao/notificacoes/{eventId} (matricula) continua sem JWT.",
    )
    .setVersion("0.2.0")
    .addBearerAuth()
    .addServer("/", "Gateway (Traefik ou Ingress)")
    .build();
  const document = SwaggerModule.createDocument(app, config);
  SwaggerModule.setup("comunicacao/docs", app, document, {
    jsonDocumentUrl: "comunicacao/openapi.json",
  });
  const porta = Number(process.env.PORT ?? 3000);
  await app.listen(porta);
}

void bootstrap();
