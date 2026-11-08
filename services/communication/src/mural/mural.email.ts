import { MensagemEmail } from "../email/email.service";
import { PostMural } from "./mural.tipos";

export function montarEmailMural(post: PostMural, destinatario: string): MensagemEmail {
  const data = post.createdAt.toISOString().slice(0, 10);
  const body =
    "<html><body>\n" +
    "<h2>Nova publicação no Mural da escola</h2>\n" +
    `<h3>${post.title}</h3>\n` +
    `<p>${post.content.replace(/\n/g, "<br/>")}</p>\n` +
    "<hr/>\n" +
    `<p><small>Publicado por <strong>${post.authorName}</strong> em ${data}.<br/>\n` +
    "Acesse o sistema para visualizar.</small></p>\n" +
    "</body></html>\n";
  return {
    to: destinatario,
    subject: `Nova publicação no mural: ${post.title}`,
    body,
    html: true,
  };
}
