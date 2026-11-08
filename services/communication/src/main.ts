import "reflect-metadata";
import { NestFactory } from "@nestjs/core";
import { DocumentBuilder, SwaggerModule } from "@nestjs/swagger";
import { AppModule } from "./app.module";

async function bootstrap(): Promise<void> {
  const app = await NestFactory.create(AppModule);
  const config = new DocumentBuilder()
    .setTitle("SING Communication")
    .setDescription(
      "Notificacao da matricula. Esta UI e publica. " +
        "GET /api/v1/comunicacao/notificacoes/{eventId} continua sem JWT.",
    )
    .setVersion("0.1.0")
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
