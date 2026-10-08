import os
import re

from caminhos import PROTOTIPOS

AQUI = os.path.dirname(os.path.abspath(__file__))
PASTA = PROTOTIPOS
BASE = open(os.path.join(PASTA, "P009.html"), encoding="utf-8").read()
CABECA, RESTO = BASE.split("<main class=\"page\">", 1)
RODAPE = "</main>" + RESTO.split("</main>", 1)[1]
CABECA = re.sub(r"<style>.*?</style>", "<style>{estilo}</style>", CABECA, flags=re.S)
CABECA = CABECA.replace("<title>Instituto Aurora – Vocabulário</title>", "<title>{titulo}</title>")
CABECA = CABECA.replace('<li class="on">', "<li>")
CABECA = CABECA.replace('<div class="avatar">HP</div><div><b>Helena Prado</b> <span>· Administradora</span></div>',
                        '<div class="avatar">{iniciais}</div><div><b>{nome}</b> <span>· {papel}</span></div>')

ADMIN = ("HP", "Helena Prado", "Administradora")


def pagina(codigo, titulo, menu, usuario, conteudo, estilo="", altura=720):
    html = CABECA
    html = html.replace("{estilo}", f"body{{height:{altura}px}}" + estilo)
    html = html.replace("{titulo}", f"Instituto Aurora – {titulo}")
    html = html.replace("{iniciais}", usuario[0]).replace("{nome}", usuario[1]).replace("{papel}", usuario[2])
    html = re.sub(rf"<li>(<svg[^\n]*?</svg>){menu}</li>", rf'<li class="on">\1{menu}</li>', html)
    with open(os.path.join(PASTA, f"{codigo}.html"), "w", encoding="utf-8") as arquivo:
        arquivo.write(html + '<main class="page">' + conteudo + RODAPE)


ABAS = ["Identidade visual", "Usuários", "Papéis", "Vocabulário", "Módulos", "Regras"]


def abas(ativa):
    return '<div class="tabs">' + "".join(
        f'<div class="tab{" on" if a == ativa else ""}">{a}</div>' for a in ABAS) + "</div>"


pagina("P011", "Regras acadêmicas", "Configurações", ADMIN, f"""
<div class="crumb">Configurações › Regras acadêmicas</div>
<div class="ph"><h1>Configurações</h1></div>
{abas("Regras")}
<div class="row">
 <div class="card flex1">
  <h2>Regras acadêmicas da organização</h2>
  <div class="f"><label>Limite de matrículas ativas por aluno<span class="req">*</span></label>
   <div class="row" style="align-items:center"><input type="text" value="2" style="width:120px"><span class="small muted">Padrão do perfil “Projeto social”: 2</span></div>
   <div class="help">De 1 a 20 matrículas, ou “sem limite”. Vale para as próximas matrículas.</div></div>
  <div class="f"><label>Frequência mínima<span class="req">*</span></label>
   <div class="row" style="align-items:center"><div class="ig bad" style="width:160px"><input type="text" value="120"><span class="sfx">%</span></div><span class="small muted">Padrão do perfil: 75 %</span></div>
   <div class="errmsg">Informe um valor entre 0 e 100.</div></div>
  <div class="f"><label>Janela para lançamento de chamada<span class="req">*</span></label>
   <div class="row" style="align-items:center"><div class="ig" style="width:160px"><input type="text" value="7"><span class="sfx">dias</span></div><span class="small muted">Padrão do perfil: 7 dias</span></div>
   <div class="help">De 1 a 90 dias após a data da aula.</div></div>
  <div class="actions"><div class="btn primary">Salvar</div><div class="btn">Restaurar padrões</div></div>
 </div>
 <div class="card" style="width:330px;flex:none">
  <h2>Onde cada regra é aplicada</h2>
  <div class="note info" style="margin-bottom:10px">Matrículas: o limite é conferido ao confirmar a matrícula.</div>
  <div class="note info" style="margin-bottom:10px">Frequência: abaixo do mínimo, o aluno entra em “Alunos em risco” e o responsável vê um alerta.</div>
  <div class="note info">Chamada: fora da janela, o professor não consegue lançar a aula.</div>
  <div class="help" style="margin-top:12px">Toda alteração fica registrada na auditoria com o valor anterior e o novo.</div>
 </div>
</div>
""")

PERMISSOES = [
    ("Cadastros", [("consultar alunos", True), ("gerenciar alunos", False), ("gerenciar turmas", False)]),
    ("Acadêmico", [("matricular", False), ("registrar chamada", False), ("gerenciar lista de espera", False)]),
    ("Gestão", [("consultar painel", False), ("consultar relatórios", True), ("exportar dados", False)]),
    ("Configurações", [("configurar organização", False), ("gerenciar usuários", False), ("gerenciar papéis", False)]),
]
grupos = "".join(
    f'<div class="card" style="padding:12px"><div class="lbl">{g}</div>' + "".join(
        f'<div style="display:flex;gap:8px;align-items:center;margin-top:7px;font-size:14px">'
        f'<span style="width:16px;height:16px;border-radius:4px;border:1.5px solid {"var(--brand)" if m else "#94A3B8"};'
        f'background:{"var(--brand)" if m else "#fff"};display:inline-flex;align-items:center;justify-content:center;color:#fff;font-size:11px">{"✓" if m else ""}</span>{n}</div>'
        for n, m in itens) + "</div>"
    for g, itens in PERMISSOES)
pagina("P012", "Papéis", "Configurações", ADMIN, f"""
<div class="crumb">Configurações › Papéis e permissões</div>
<div class="ph"><h1>Configurações</h1></div>
{abas("Papéis")}
<div class="row">
 <div class="tbl-card" style="width:300px;flex:none"><table class="tbl"><thead><tr><th>Papel</th><th>Usuários</th></tr></thead><tbody>
  <tr><td class="n">Administrador</td><td>2</td></tr><tr><td class="n">Secretaria</td><td>3</td></tr>
  <tr><td class="n">Professor</td><td>14</td></tr><tr><td class="n">Financeiro</td><td>1</td></tr>
  <tr class="sel"><td class="n">Coordenação</td><td>1</td></tr></tbody></table>
  <div style="padding:10px 12px"><div class="btn sm">+ Novo papel</div></div></div>
 <div class="flex1">
  <div class="f"><label>Nome do papel<span class="req">*</span></label><input type="text" value="Coordenação" style="width:320px"><div class="help">Único na organização. Ex.: Coordenação, Portaria, Voluntário.</div></div>
  <div class="lbl">Permissões, por módulo</div>
  <div class="g2" style="gap:12px">{grupos}</div>
  <div class="actions" style="margin-top:12px"><div class="btn primary">Salvar</div><div class="btn">Cancelar</div></div>
 </div>
</div>
""")

FILA = [
    ("1", "Lucas Martins", "Renata Martins", "18/09", '<span class="badge b-blue">Convocado</span> <span class="small muted">responde até 12/10</span>'),
    ("2", "Sofia Almeida", "Carla Almeida", "20/09", '<span class="badge b-gray">Aguardando</span>'),
    ("3", "Davi Souza", "Marcos Souza", "24/09", '<span class="badge b-gray">Aguardando</span>'),
    ("–", "Pedro Nunes", "Ana Nunes", "02/09", '<span class="badge b-amber">Não respondeu</span>'),
    ("–", "Laura Castro", "Paulo Castro", "29/08", '<span class="badge b-green">Confirmado</span>'),
]
linhas = "".join(f"<tr><td>{a}</td><td class='n'>{b}</td><td>{c}</td><td>{d}</td><td>{e}</td></tr>" for a, b, c, d, e in FILA)
pagina("P013", "Lista de espera", "Lista de espera", ("MD", "Marina Duarte", "Secretaria"), f"""
<div class="crumb">Lista de espera › Violão – Nível 1</div>
<div class="ph" style="display:flex;align-items:center;gap:12px"><h1>Violão – Nível 1</h1><span class="badge b-red">Lotada · 15/15</span><div class="right btn primary">+ Incluir interessado</div></div>
<div class="note brand" style="margin-bottom:12px">Quando surge uma vaga, o primeiro da fila em “Aguardando” é convocado por e-mail e tem 3 dias para responder. A vaga fica reservada durante o prazo.</div>
<div class="tbl-card"><table class="tbl"><thead><tr><th style="width:70px">Posição</th><th>Interessado</th><th>Responsável</th><th>Incluído em</th><th>Situação</th></tr></thead><tbody>{linhas}</tbody></table></div>
<div class="row" style="margin-top:14px">
 <div class="card flex1"><h2>Incluir interessado</h2><div class="g3">
  <div class="f"><label>Nome do interessado<span class="req">*</span></label><input type="text" value="Sofia Almeida"></div>
  <div class="f"><label>Responsável<span class="req">*</span></label><input type="text" value="Carla Almeida"></div>
  <div class="f"><label>E-mail do responsável<span class="req">*</span></label><input type="text" value="carla.almeida@email.com" class="bad"><div class="errmsg">Este interessado já está na lista, na posição 2.</div></div>
 </div></div>
</div>
""")

ALUNOS = [("Bianca Rocha", True, ""), ("Caio Mendes", True, ""), ("Helena Costa", False, ""), ("Igor Alves", True, ""),
          ("Julia Prado", False, "Justificada — atestado"), ("Kevin Dias", True, ""), ("Laura Nunes", True, "")]
lin = ""
for nome, presente, obs in ALUNOS:
    seg = ('<div class="seg"><span class="{a}">Presente</span><span class="{b}">Falta</span></div>'
           .format(a="on" if presente else "", b="" if presente else "on"))
    sit = f'<span class="badge b-blue">{obs}</span>' if obs else ""
    lin += f"<tr><td class='n'>{nome}</td><td>{seg}</td><td>{sit}</td></tr>"
pagina("P014", "Chamada", "Chamada", ("CL", "Carlos Lima", "Professor"), f"""
<div class="crumb">Chamada › Violão – Nível 1</div>
<div class="ph" style="display:flex;align-items:center;gap:12px"><h1>Chamada</h1><select style="width:220px;margin-left:12px"><option>Violão – Nível 1</option></select><input type="text" value="07/10/2026" style="width:140px"><span class="right small muted">7 alunos · 5 presentes · 2 faltas</span></div>
<div class="note warn" style="margin-bottom:12px">Lançamento permitido até 7 dias após a aula (regra da escola).</div>
<div class="row">
 <div class="tbl-card flex1"><table class="tbl"><thead><tr><th>Aluno</th><th style="width:220px">Presença</th><th>Observação</th></tr></thead><tbody>{lin}</tbody></table></div>
 <div class="card" style="width:300px;flex:none"><h2>Aula de 07/10</h2>
  <div class="f"><label>Conteúdo da aula<span class="req">*</span></label><textarea style="height:110px">Acordes maiores e troca entre Dó e Sol.</textarea><div class="help">Até 255 caracteres.</div></div>
  <div class="btn primary block" style="margin-bottom:8px">Salvar chamada</div><div class="btn block">Salvar e fechar</div>
  <div class="help" style="margin-top:8px">Depois de fechada, só a coordenação pode reabrir.</div></div>
</div>
""", estilo=".seg{display:inline-flex;border:1px solid var(--line-2);border-radius:6px;overflow:hidden}.seg span{padding:4px 12px;font-size:13px;font-weight:600;color:#475569}.seg span.on{background:var(--brand);color:#fff}textarea{width:100%;border:1px solid var(--line-2);border-radius:6px;padding:8px;font:inherit;font-size:14px}")


def grafico_linhas():
    meses = ["Mai", "Jun", "Jul", "Ago", "Set", "Out"]
    valores = [88, 90, 86, 91, 93, 91]
    pts = " ".join(f"{40 + i * 62},{150 - (v - 80) * 9}" for i, v in enumerate(valores))
    eixo = "".join(f'<text x="{40 + i * 62}" y="172" font-size="11" text-anchor="middle" fill="#64748B">{m}</text>' for i, m in enumerate(meses))
    grade = "".join(f'<line x1="30" x2="370" y1="{150 - (v - 80) * 9}" y2="{150 - (v - 80) * 9}" stroke="#E2E8F0"/><text x="24" y="{154 - (v - 80) * 9}" font-size="11" text-anchor="end" fill="#64748B">{v}%</text>' for v in (80, 85, 90, 95))
    rot = "".join(f'<circle cx="{40 + i * 62}" cy="{150 - (v - 80) * 9}" r="3.5" fill="#0F766E"/>' for i, v in enumerate(valores))
    return f'<svg viewBox="0 0 380 180" width="100%" height="180">{grade}<polyline points="{pts}" fill="none" stroke="#0F766E" stroke-width="2.5"/>{rot}{eixo}</svg>'


def grafico_barras():
    novas = [14, 9, 5, 18, 11, 7]
    cancel = [3, 4, 2, 5, 3, 2]
    meses = ["Mai", "Jun", "Jul", "Ago", "Set", "Out"]
    barras = ""
    for i, (n, c) in enumerate(zip(novas, cancel)):
        x = 40 + i * 56
        barras += f'<rect x="{x}" y="{150 - n * 6.5}" width="18" height="{n * 6.5}" fill="#0F766E"/><rect x="{x + 20}" y="{150 - c * 6.5}" width="18" height="{c * 6.5}" fill="#F59E0B"/>'
        barras += f'<text x="{x + 19}" y="168" font-size="11" text-anchor="middle" fill="#64748B">{meses[i]}</text>'
    grade = "".join(f'<line x1="30" x2="370" y1="{150 - v * 6.5}" y2="{150 - v * 6.5}" stroke="#E2E8F0"/><text x="24" y="{154 - v * 6.5}" font-size="11" text-anchor="end" fill="#64748B">{v}</text>' for v in (0, 10, 20))
    legenda = '<rect x="230" y="4" width="10" height="10" fill="#0F766E"/><text x="244" y="13" font-size="11" fill="#334155">Novas</text><rect x="290" y="4" width="10" height="10" fill="#F59E0B"/><text x="304" y="13" font-size="11" fill="#334155">Cancelamentos</text>'
    return f'<svg viewBox="0 0 380 180" width="100%" height="180">{grade}{barras}{legenda}</svg>'


def grafico_ocupacao():
    dados = [("Violão", 100), ("Informática", 95), ("Pintura", 80), ("Robótica", 73), ("Futsal", 66)]
    corpo = ""
    for i, (nome, v) in enumerate(dados):
        y = 10 + i * 32
        corpo += f'<text x="86" y="{y + 14}" font-size="12" text-anchor="end" fill="#334155">{nome}</text><rect x="94" y="{y}" width="240" height="18" rx="3" fill="#E2E8F0"/><rect x="94" y="{y}" width="{240 * v / 100}" height="18" rx="3" fill="{"#B45309" if v == 100 else "#0F766E"}"/><text x="{340}" y="{y + 14}" font-size="12" fill="#334155">{v}%</text>'
    return f'<svg viewBox="0 0 380 170" width="100%" height="170">{corpo}</svg>'


pagina("P016", "Painel", "Painel", ADMIN, f"""
<div class="ph" style="display:flex;align-items:center;gap:10px"><h1>Painel gerencial</h1>
 <div class="right" style="display:flex;gap:8px;align-items:center"><span class="small muted">Período</span><select style="width:170px"><option>Últimos 6 meses</option></select><span class="small muted">Unidade</span><select style="width:150px"><option>Unidade Centro</option></select><div class="btn">Exportar</div></div></div>
<div class="g3" style="grid-template-columns:repeat(4,1fr);gap:12px;margin-bottom:12px">
 <div class="card"><div class="small muted">Alunos ativos</div><div style="font-size:28px;font-weight:700">184</div><div class="small" style="color:#15803D">+12 no período</div></div>
 <div class="card"><div class="small muted">Ocupação das turmas</div><div style="font-size:28px;font-weight:700">86 %</div><div class="small muted">212 de 246 vagas</div></div>
 <div class="card"><div class="small muted">Frequência média</div><div style="font-size:28px;font-weight:700">91 %</div><div class="small muted">mínima da escola: 75 %</div></div>
 <div class="card"><div class="small muted">Alunos em risco</div><div style="font-size:28px;font-weight:700;color:#B91C1C">12</div><div class="small muted">abaixo da frequência mínima</div></div>
</div>
<div class="g3" style="gap:12px">
 <div class="card"><h2>Frequência média por mês</h2>{grafico_linhas()}</div>
 <div class="card"><h2>Matrículas novas e cancelamentos</h2>{grafico_barras()}</div>
 <div class="card"><h2>Ocupação por oficina</h2>{grafico_ocupacao()}</div>
</div>
""")

CELULAR = """<div class="cel"><div class="tela"><div class="topo"><svg width="26" height="26" viewBox="0 0 40 40"><rect width="40" height="40" rx="10" fill="#0F766E"/><path d="M11 28a9 9 0 0 1 18 0z" fill="#FDE68A"/><path d="M8 30.5h24" stroke="#fff" stroke-width="2.4" stroke-linecap="round"/></svg><b>Instituto Aurora</b><span>Renata M. · Responsável</span></div>{corpo}</div></div>"""
AULAS = [("30/09", "Presente", "b-green"), ("25/09", "Falta", "b-red"), ("23/09", "Presente", "b-green"),
         ("18/09", "Justificada", "b-blue"), ("16/09", "Falta", "b-red"), ("11/09", "Presente", "b-green")]
freq = """<div class="pad"><div class="lbl">Aluno</div><select><option>Lucas Martins</option></select>
<div class="lbl" style="margin-top:8px">Período</div><select><option>01/09 a 30/09</option></select>
<div class="kpi"><div class="pct">72 %</div><div><b>de presença</b><br><span class="small muted">3 faltas em 11 aulas</span></div></div>
<div class="note err" style="font-size:12.5px">Abaixo da frequência mínima da escola (75 %).</div>
<div class="lista">""" + "".join(f'<div class="it"><span>Aula de {d}</span><span class="badge {c}">{s}</span></div>' for d, s, c in AULAS) + "</div></div>"
GRADE = [("Terça", [("14:00", "Violão – Nível 1", "Sala 3")]), ("Quarta", [("09:00", "Robótica Kids", "Laboratório")]),
         ("Quinta", [("14:00", "Violão – Nível 1", "Sala 3")]), ("Sexta", [("10:30", "Futsal", "Quadra")])]
grade = '<div class="pad"><div class="lbl">Grade horária · Lucas Martins</div>' + "".join(
    f'<div class="dia">{d}</div>' + "".join(f'<div class="aula"><b>{h}</b><div>{t}<br><span class="small muted">{s}</span></div></div>' for h, t, s in aulas)
    for d, aulas in GRADE) + '<div class="help" style="margin-top:8px">Somente turmas com matrícula ativa.</div></div>'
nav = '<div class="abas"><span class="{a}">Frequência</span><span class="{b}">Grade horária</span><span>Avisos</span></div>'
estilo_cel = """body{height:760px;background:#E2E8F0;display:flex;gap:70px;align-items:center;justify-content:center;font-size:14px}
.cel{width:360px;height:720px;border-radius:44px;background:#0F172A;padding:14px;box-shadow:0 10px 30px rgba(15,23,42,.25)}
.tela{width:100%;height:100%;border-radius:32px;background:#F8FAFC;overflow:hidden;display:flex;flex-direction:column}
.topo{display:flex;align-items:center;gap:8px;padding:16px 16px 10px;background:#fff;border-bottom:1px solid #E2E8F0;flex-wrap:wrap}
.topo b{font-size:15px}.topo span{width:100%;font-size:12px;color:#64748B}
.abas{display:flex;gap:4px;padding:8px 12px;background:#fff;border-bottom:1px solid #E2E8F0}.abas span{flex:1;text-align:center;padding:6px 0;font-size:12.5px;font-weight:600;color:#64748B;border-radius:6px}.abas span.on{background:#CCFBF1;color:#115E59}
.pad{padding:12px 14px;overflow:hidden}.pad select{width:100%}
.kpi{display:flex;align-items:center;gap:12px;margin:12px 0 8px}.pct{font-size:40px;font-weight:800;color:#B91C1C}
.lista{margin-top:10px;background:#fff;border:1px solid #E2E8F0;border-radius:8px}.it{display:flex;justify-content:space-between;padding:7px 10px;border-bottom:1px solid #EEF2F6;font-size:13px}
.dia{font-weight:700;font-size:13px;margin:10px 0 4px;color:#115E59}.aula{display:flex;gap:10px;background:#fff;border:1px solid #E2E8F0;border-left:4px solid #0F766E;border-radius:6px;padding:7px 10px;font-size:13px;margin-bottom:6px}"""
html = (f'<!DOCTYPE html><html lang="pt-BR"><head><meta charset="utf-8"><title>Instituto Aurora – Portal do responsável</title>'
        f'<link rel="stylesheet" href="sing.css"><style>{estilo_cel}</style></head><body>'
        + CELULAR.replace("{corpo}", nav.format(a="on", b="") + freq)
        + CELULAR.replace("{corpo}", nav.format(a="", b="on") + grade) + "</body></html>")
with open(os.path.join(PASTA, "P015.html"), "w", encoding="utf-8") as arquivo:
    arquivo.write(html)
