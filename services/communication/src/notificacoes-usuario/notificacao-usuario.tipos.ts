export interface NotificacaoUsuario {
  id: number;
  userId: number;
  title: string;
  message: string;
  type: string;
  actionUrl: string | null;
  referenceKey: string;
  readAt: Date | null;
  createdAt: Date;
}

export type NovaNotificacaoUsuario = Omit<NotificacaoUsuario, "id">;

export interface RespostaNotificacaoUsuario {
  id: number;
  title: string;
  message: string;
  type: string;
  actionUrl: string | null;
  readAt: string | null;
  createdAt: string;
}

export interface SolicitacaoNotificacao {
  eventId: string;
  type: string;
  title: string;
  message: string;
  actionUrl: string | null;
  referenceKey: string;
  recipientUserIds: number[];
}

export type ResultadoSolicitacao = "processada" | "duplicada" | "ignorada";

export abstract class RepositorioNotificacoesUsuario {
  abstract listar(userId: number): Promise<NotificacaoUsuario[]>;
  abstract buscar(id: number): Promise<NotificacaoUsuario | null>;
  abstract marcarLida(id: number, agora: Date): Promise<void>;
  abstract marcarTodasLidas(userId: number, agora: Date): Promise<void>;
  abstract existeNaoLida(userId: number, referenceKey: string): Promise<boolean>;
  abstract inserir(dados: NovaNotificacaoUsuario): Promise<void>;
}

export abstract class EventosProcessados {
  abstract jaProcessado(eventId: string): Promise<boolean>;
  abstract registrar(eventId: string): Promise<void>;
}
