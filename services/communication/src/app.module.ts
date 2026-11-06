import { Module } from "@nestjs/common";
import { ConsumidorService } from "./consumidor.service";
import { HealthController } from "./health.controller";
import { NotificacaoController } from "./notificacao.controller";
import { NotificacaoService } from "./notificacao.service";

@Module({
  controllers: [NotificacaoController, HealthController],
  providers: [NotificacaoService, ConsumidorService],
})
export class AppModule {}
