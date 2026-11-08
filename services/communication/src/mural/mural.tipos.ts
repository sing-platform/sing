export interface PostMural {
  id: number;
  title: string;
  content: string;
  authorUserId: number;
  authorName: string;
  authorEmail: string;
  createdAt: Date;
  updatedAt: Date | null;
  audienceRoles: string[];
  audienceSchoolroomIds: number[];
  active: boolean;
}

export type NovoPostMural = Omit<PostMural, "id">;

export interface PostComLeitura {
  post: PostMural;
  viewedAt: Date | null;
}

export interface ViewMural {
  postId: number;
  userId: number;
  viewedAt: Date | null;
}

export interface LinhaEngajamento {
  postId: number;
  title: string;
  createdAt: Date;
  authorUsername: string | null;
  viewCount: number;
  readCount: number;
}

export interface RespostaMural {
  id: number;
  title: string;
  content: string;
  authorName: string;
  createdAt: string;
  updatedAt: string | null;
  audienceRoles: string[];
  audienceSchoolroomIds: number[];
  viewed: boolean;
}

export interface RespostaEngajamento {
  postId: number;
  title: string;
  createdAt: string;
  authorUsername: string | null;
  viewCount: number;
  readCount: number;
}

export interface CorpoPublicacao {
  title: string;
  content: string;
  audienceRoles: string[];
  audienceSchoolroomIds: number[];
}

export abstract class RepositorioMural {
  abstract criarPost(dados: NovoPostMural): Promise<PostMural>;
  abstract buscarPost(id: number): Promise<PostMural | null>;
  abstract salvarPost(post: PostMural): Promise<void>;
  abstract listarAtivos(): Promise<PostMural[]>;
  abstract listarComLeituraDoUsuario(userId: number): Promise<PostComLeitura[]>;
  abstract buscarView(postId: number, userId: number): Promise<ViewMural | null>;
  abstract criarViewsNovas(postId: number, userIds: number[]): Promise<number[]>;
  abstract marcarTodasLidas(userId: number, agora: Date): Promise<void>;
  abstract contarNaoLidas(userId: number): Promise<number>;
  abstract engajamento(inicio: Date, fimExclusivo: Date): Promise<LinhaEngajamento[]>;
}
