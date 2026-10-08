import math

from gera_modelo_dados import (
    LINHA_CAIXA,
    TINTA,
    Caixa,
    Svg,
    medir,
    relacao,
    titulo,
)

TAM_MEMBRO = 12.5
TAM_NOME = 13.5
ALTURA_LINHA = 16.5
RECUO_CONTINUACAO = 16
MARGEM_X = 9
COMPARTIMENTO_VAZIO = 8

CORES = {
    "api": "#dbe9f8",
    "aplicacao": "#d9efd6",
    "dominio": "#fbe9c4",
    "infra": "#e4dcf3",
    "externo": "#ececec",
}


def dividir_parametros(texto):
    partes, atual, profundidade = [], "", 0
    for caractere in texto:
        if caractere in "<(":
            profundidade += 1
        elif caractere in ">)":
            profundidade -= 1
        if caractere == "," and profundidade == 0:
            partes.append(atual.strip() + ",")
            atual = ""
        else:
            atual += caractere
    if atual.strip():
        partes.append(atual.strip())
    return partes


def quebrar(texto, largura_maxima):
    if medir(texto, TAM_MEMBRO) <= largura_maxima or "(" not in texto:
        return [(texto, 0)]
    cabeca, resto = texto.split("(", 1)
    corpo, cauda = resto.rsplit(")", 1)
    linhas = []
    atual = cabeca + "("
    recuo = 0
    for parametro in dividir_parametros(corpo):
        candidato = atual + ("" if atual.endswith("(") else " ") + parametro
        if medir(candidato, TAM_MEMBRO) + recuo <= largura_maxima or atual.endswith("("):
            atual = candidato
        else:
            linhas.append((atual, recuo))
            recuo = RECUO_CONTINUACAO
            atual = parametro
    final = atual + ")" + cauda
    if medir(final, TAM_MEMBRO) + recuo <= largura_maxima:
        linhas.append((final, recuo))
    else:
        linhas.append((atual + ")", recuo))
        linhas.append((cauda.lstrip(": ").strip() and ": " + cauda.lstrip(": ").strip(), RECUO_CONTINUACAO))
    return linhas


def normalizar(membro):
    if isinstance(membro, tuple):
        return membro
    return (membro, False)


class Classe(Caixa):
    def __init__(self, nome, x, y, camada="dominio", estereotipo=None, atributos=(), operacoes=(),
                 abstrata=False, enumeracao=False, largura=None, largura_maxima=340, tres_compartimentos=True):
        self.nome = nome
        self.estereotipo = estereotipo
        self.abstrata = abstrata
        self.enumeracao = enumeracao
        self.camada = camada
        self.tres = tres_compartimentos and not enumeracao
        self.atributos = [(quebrar(t, largura_maxima), est) for t, est in map(normalizar, atributos)]
        self.operacoes = [(quebrar(t, largura_maxima), est) for t, est in map(normalizar, operacoes)]
        textos = [
            (texto, recuo)
            for grupo in (self.atributos, self.operacoes)
            for linhas, _ in grupo
            for texto, recuo in linhas
        ]
        largura_texto = max([medir(t, TAM_MEMBRO) + r for t, r in textos], default=0)
        largura_nome = medir(nome, TAM_NOME, negrito=True, italico=abstrata)
        if estereotipo:
            largura_nome = max(largura_nome, medir(estereotipo, TAM_MEMBRO))
        calculada = max(largura_texto, largura_nome) + 2 * MARGEM_X
        largura_final = largura if largura else math.ceil(calculada / 2) * 2
        self.altura_cabecalho = 8 + ALTURA_LINHA + (ALTURA_LINHA + 1 if estereotipo else 0) + 4
        self.altura_atributos = self._altura(self.atributos)
        self.altura_operacoes = self._altura(self.operacoes)
        altura = self.altura_cabecalho + self.altura_atributos + (self.altura_operacoes if self.tres else 0)
        super().__init__(x, y, largura_final, altura)

    def _altura(self, grupo):
        linhas = sum(len(l) for l, _ in grupo)
        if linhas == 0:
            return COMPARTIMENTO_VAZIO if (self.tres or self.enumeracao) else 0
        return linhas * ALTURA_LINHA + 8

    def _desenhar_membros(self, svg, grupo, y):
        cursor = y + 4 + 12
        for linhas, estatico in grupo:
            for texto, recuo in linhas:
                svg.texto(self.x + MARGEM_X + recuo, cursor, texto, tam=TAM_MEMBRO, sublinhado=estatico)
                cursor += ALTURA_LINHA

    def desenhar(self, svg):
        svg.retangulo(self.x, self.y, self.w, self.h)
        svg.retangulo(self.x, self.y, self.w, self.altura_cabecalho, preenchimento=CORES[self.camada])
        centro = self.x + self.w / 2
        cursor = self.y + 8 + 12
        if self.estereotipo:
            svg.texto(centro, cursor, self.estereotipo, tam=TAM_MEMBRO, ancora="middle", cor="#444444")
            cursor += ALTURA_LINHA + 1
        svg.texto(centro, cursor + 1, self.nome, tam=TAM_NOME, negrito=True, italico=self.abstrata, ancora="middle")
        y = self.y + self.altura_cabecalho
        self._desenhar_membros(svg, self.atributos, y)
        if self.tres:
            y2 = y + self.altura_atributos
            svg.linha(self.x, y2, self.x + self.w, y2)
            self._desenhar_membros(svg, self.operacoes, y2)
        elif self.enumeracao:
            pass


class Pacote:
    def __init__(self, nome, x, y, w, h):
        self.nome = nome
        self.x = x
        self.y = y
        self.w = w
        self.h = h

    def desenhar(self, svg):
        aba_largura = medir(self.nome, 13, negrito=True) + 24
        aba_altura = 24
        svg.adicionar(
            "fundo",
            f'<rect x="{self.x:.1f}" y="{self.y:.1f}" width="{self.w:.1f}" height="{self.h:.1f}" '
            f'fill="#fafafa" stroke="#6b6b6b" stroke-width="1.3"/>',
        )
        svg.adicionar(
            "fundo",
            f'<rect x="{self.x:.1f}" y="{self.y - aba_altura:.1f}" width="{aba_largura:.1f}" height="{aba_altura}" '
            f'fill="#eeeeee" stroke="#6b6b6b" stroke-width="1.3"/>',
        )
        svg.texto(self.x + 12, self.y - 7, self.nome, tam=13, negrito=True, camada="fundo")


def rotulo(svg, x, y, texto, ancora="start", tam=12.5, italico=False):
    svg.texto(x, y, texto, tam=tam, ancora=ancora, halo=True, camada="topo", italico=italico)
