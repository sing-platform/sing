import { Injectable } from "@nestjs/common";
import { ObjectId } from "mongodb";
import { MongoService } from "../mongo/mongo.service";
import { EmailPendente, MensagemEmail, OutboxEmail } from "./email.service";

interface DocumentoOutbox extends MensagemEmail {
  _id: ObjectId;
  createdAt: Date;
}

@Injectable()
export class OutboxEmailMongo extends OutboxEmail {
  constructor(private readonly mongo: MongoService) {
    super();
  }

  async guardar(mensagem: MensagemEmail): Promise<void> {
    await this.colecao().insertOne({ ...mensagem, createdAt: new Date() } as DocumentoOutbox);
  }

  async pendentes(limite: number): Promise<EmailPendente[]> {
    const docs = await this.colecao().find().sort({ createdAt: 1 }).limit(limite).toArray();
    return docs.map((doc) => ({
      id: doc._id.toHexString(),
      mensagem: { to: doc.to, subject: doc.subject, body: doc.body, html: doc.html },
    }));
  }

  async remover(id: string): Promise<void> {
    await this.colecao().deleteOne({ _id: new ObjectId(id) });
  }

  private colecao() {
    return this.mongo.db().collection<DocumentoOutbox>("email_outbox");
  }
}
