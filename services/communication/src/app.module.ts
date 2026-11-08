import { Module } from "@nestjs/common";
import { ClienteCore, ClienteCoreHttp } from "./core/core.client";
import { ConsumidorService } from "./consumidor.service";
import { OutboxEmailMongo } from "./email/email.outbox";
import { PublicadorEmailRabbit } from "./email/email.rabbit";
import { EmailService, OutboxEmail, PublicadorEmail } from "./email/email.service";
import { HealthController } from "./health.controller";
import { MongoService } from "./mongo/mongo.service";
import { MuralController } from "./mural/mural.controller";
import { RepositorioMuralMongo } from "./mural/mural.mongo";
import { MuralService } from "./mural/mural.service";
import { RepositorioMural } from "./mural/mural.tipos";
import { NotificacaoController } from "./notificacao.controller";
import { NotificacaoService } from "./notificacao.service";
import { NotificacaoUsuarioController } from "./notificacoes-usuario/notificacao-usuario.controller";
import {
  EventosProcessadosMongo,
  RepositorioNotificacoesUsuarioMongo,
} from "./notificacoes-usuario/notificacao-usuario.mongo";
import { NotificacaoUsuarioService } from "./notificacoes-usuario/notificacao-usuario.service";
import { EventosProcessados, RepositorioNotificacoesUsuario } from "./notificacoes-usuario/notificacao-usuario.tipos";
import { SolicitacaoConsumidor } from "./notificacoes-usuario/solicitacao.consumidor";

@Module({
  controllers: [NotificacaoController, HealthController, MuralController, NotificacaoUsuarioController],
  providers: [
    NotificacaoService,
    ConsumidorService,
    MongoService,
    { provide: ClienteCore, useClass: ClienteCoreHttp },
    { provide: PublicadorEmail, useClass: PublicadorEmailRabbit },
    { provide: OutboxEmail, useClass: OutboxEmailMongo },
    { provide: RepositorioMural, useClass: RepositorioMuralMongo },
    { provide: RepositorioNotificacoesUsuario, useClass: RepositorioNotificacoesUsuarioMongo },
    { provide: EventosProcessados, useClass: EventosProcessadosMongo },
    EmailService,
    MuralService,
    NotificacaoUsuarioService,
    SolicitacaoConsumidor,
  ],
})
export class AppModule {}
