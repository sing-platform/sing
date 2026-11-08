import { Db } from "mongodb";

export async function proximoId(db: Db, nome: string): Promise<number> {
  const contador = await db
    .collection<{ _id: string; seq: number }>("counters")
    .findOneAndUpdate({ _id: nome }, { $inc: { seq: 1 } }, { upsert: true, returnDocument: "after" });
  if (!contador) {
    throw new Error(`contador ${nome} nao retornou`);
  }
  return contador.seq;
}
