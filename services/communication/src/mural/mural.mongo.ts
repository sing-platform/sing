import { Injectable } from "@nestjs/common";
import { MongoService } from "../mongo/mongo.service";
import { proximoId } from "../mongo/sequencia";
import {
  LinhaEngajamento,
  NovoPostMural,
  PostComLeitura,
  PostMural,
  RepositorioMural,
  ViewMural,
} from "./mural.tipos";

type DocumentoPost = Omit<PostMural, "id"> & { _id: number };

@Injectable()
export class RepositorioMuralMongo extends RepositorioMural {
  constructor(private readonly mongo: MongoService) {
    super();
  }

  async criarPost(dados: NovoPostMural): Promise<PostMural> {
    const id = await proximoId(this.mongo.db(), "mural_posts");
    await this.posts().insertOne({ _id: id, ...dados });
    return { id, ...dados };
  }

  async buscarPost(id: number): Promise<PostMural | null> {
    const doc = await this.posts().findOne({ _id: id });
    return doc ? paraPost(doc) : null;
  }

  async salvarPost(post: PostMural): Promise<void> {
    const { id, ...resto } = post;
    await this.posts().replaceOne({ _id: id }, resto);
  }

  async listarAtivos(): Promise<PostMural[]> {
    const docs = await this.posts().find({ active: true }).sort({ createdAt: -1, _id: -1 }).toArray();
    return docs.map(paraPost);
  }

  async listarComLeituraDoUsuario(userId: number): Promise<PostComLeitura[]> {
    const views = await this.views().find({ userId }).toArray();
    if (views.length === 0) {
      return [];
    }
    const leituraPorPost = new Map(views.map((view) => [view.postId, view.viewedAt]));
    const docs = await this.posts()
      .find({ _id: { $in: [...leituraPorPost.keys()] }, active: true })
      .sort({ createdAt: -1, _id: -1 })
      .toArray();
    return docs.map((doc) => ({ post: paraPost(doc), viewedAt: leituraPorPost.get(doc._id) ?? null }));
  }

  async buscarView(postId: number, userId: number): Promise<ViewMural | null> {
    return this.views().findOne({ postId, userId }, { projection: { _id: 0 } });
  }

  async criarViewsNovas(postId: number, userIds: number[]): Promise<number[]> {
    if (userIds.length === 0) {
      return [];
    }
    const resultado = await this.views().bulkWrite(
      userIds.map((userId) => ({
        updateOne: {
          filter: { postId, userId },
          update: { $setOnInsert: { postId, userId, viewedAt: null } },
          upsert: true,
        },
      })),
      { ordered: false },
    );
    return Object.keys(resultado.upsertedIds).map((indice) => userIds[Number(indice)]);
  }

  async marcarTodasLidas(userId: number, agora: Date): Promise<void> {
    await this.views().updateMany({ userId, viewedAt: null }, { $set: { viewedAt: agora } });
  }

  async contarNaoLidas(userId: number): Promise<number> {
    return this.views().countDocuments({ userId, viewedAt: null });
  }

  async engajamento(inicio: Date, fimExclusivo: Date): Promise<LinhaEngajamento[]> {
    const docs = await this.posts()
      .find({ active: true, createdAt: { $gte: inicio, $lt: fimExclusivo } })
      .sort({ createdAt: -1, _id: -1 })
      .toArray();
    if (docs.length === 0) {
      return [];
    }
    const totais = await this.views()
      .aggregate<{ _id: number; views: number; reads: number }>([
        { $match: { postId: { $in: docs.map((doc) => doc._id) } } },
        {
          $group: {
            _id: "$postId",
            views: { $sum: 1 },
            reads: { $sum: { $cond: [{ $ne: ["$viewedAt", null] }, 1, 0] } },
          },
        },
      ])
      .toArray();
    const porPost = new Map(totais.map((total) => [total._id, total]));
    return docs.map((doc) => ({
      postId: doc._id,
      title: doc.title,
      createdAt: doc.createdAt,
      authorUsername: doc.authorEmail ?? null,
      viewCount: porPost.get(doc._id)?.views ?? 0,
      readCount: porPost.get(doc._id)?.reads ?? 0,
    }));
  }

  private posts() {
    return this.mongo.db().collection<DocumentoPost>("mural_posts");
  }

  private views() {
    return this.mongo.db().collection<ViewMural>("mural_views");
  }
}

function paraPost(doc: DocumentoPost): PostMural {
  const { _id, ...resto } = doc;
  return { id: _id, ...resto };
}
