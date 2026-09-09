# Marca CineGiro

Símbolo: letra **G** sólida com abertura de 65° à direita, play triangular
interno com respiro. Cor: vermelho `#E93645`, o mesmo já usado no beta.

## Arquivos

| Arquivo | Onde usar |
|---|---|
| `cinegiro-simbolo.svg` | símbolo isolado, na cor da marca |
| `cinegiro-simbolo-preto.svg` | sobre fundo claro |
| `cinegiro-simbolo-branco.svg` | sobre fundo escuro ou foto |
| `cinegiro-simbolo-currentcolor.svg` | React/JSX — herda a cor do CSS |
| `cinegiro-lockup-horizontal.svg` | cabeçalho, e-mail, assinatura |
| `cinegiro-lockup-horizontal-preto.svg` | versão sobre fundo claro |
| `cinegiro-lockup-horizontal-branco.svg` | versão sobre fundo escuro |
| `cinegiro-lockup-empilhado.svg` | espaços quadrados, splash, redes |
| `cinegiro-icone-app.svg` | ícone de PWA (512×512) |
| `cinegiro-favicon.svg` | favicon (anel mais grosso, aguenta 16px) |
| `cores.css` | tokens de cor da interface |

## Cores

### Paleta oficial

| Token | Valor | Uso |
|---|---|---|
| `--cg-red` | `#E93645` | vermelho principal — logo, botões, kicker |
| `--cg-red-bright` | `#FF4D59` | vermelho destaque — hover, realces |
| `--cg-bg` | `#0B0B0D` | fundo da página |
| `--cg-surface` | `#151518` | superfície — cards |
| `--cg-surface-raised` | `#1C1C20` | superfície elevada — modais, chips |
| `--cg-text` | `#F4F0E8` | texto principal |
| `--cg-text-muted` | `#AAA6A0` | texto secundário |
| `--cg-reward` | `#E7B862` | recompensas — conquistas, troféus, Premium |

### Derivados

| Token | Valor | Uso |
|---|---|---|
| `--cg-red-press` | `#C22C39` | botão pressionado |
| `--cg-border` | `#2A2A30` | divisores |
| `--cg-border-strong` | `#3A3A42` | borda de botão secundário |
| `--cg-reward-hover` | `#F2CA84` | recompensa em hover |
| `--cg-reward-dim` | `#B8914C` | conquista bloqueada |
| `--cg-text-subtle` | `#7A7772` | metadados, contadores |

### Contraste verificado

| Combinação | Razão | Situação |
|---|---|---|
| Texto principal sobre fundo | 17,3:1 | passa |
| Texto secundário sobre fundo | 8,1:1 | passa |
| Texto secundário sobre superfície | 7,5:1 | passa |
| Recompensas sobre fundo | 10,7:1 | passa |
| Vermelho principal sobre fundo | 4,8:1 | passa |
| Branco sobre vermelho principal | 4,1:1 | **só texto grande** |
| Branco sobre vermelho destaque | 3,3:1 | **só texto grande** |
| Fundo sobre vermelho destaque | 6,1:1 | passa |

**O único ponto de atenção é o texto branco em cima do vermelho.** A 4,1:1
ele fica abaixo do mínimo de 4,5:1 que as diretrizes pedem para texto
normal. Duas saídas, ambas dentro da paleta:

1. **Deixar o rótulo do botão em 17px com peso 600 ou mais.** Aí ele se
   qualifica como texto grande, cujo mínimo é 3:1, e passa. É a solução mais
   simples e não muda cor nenhuma.
2. **Usar `--cg-bg` (#0B0B0D) como cor do texto sobre o vermelho destaque.**
   Dá 6,1:1 e passa com folga, mas muda bastante o visual do botão.

A opção 1 é a que eu seguiria.

## Regras de uso do logo

- **Área de proteção:** deixe pelo menos uma altura da letra "C" de espaço
  livre em volta do logo. Nada encosta.
- **Tamanho mínimo:** o símbolo isolado não deve ser usado abaixo de 20px.
  Abaixo disso, use o favicon, que tem o anel mais grosso.
- **Não distorça:** escale sempre proporcionalmente.
- **Não recolora** fora das variações fornecidas.
- **Não adicione** sombra, contorno ou gradiente.
- Sobre foto ou pôster, use a versão branca com um fundo escuro atrás.

## Tipografia

Duas famílias, ambas gratuitas no Google Fonts, sob licença SIL Open Font —
uso comercial liberado, o que importa porque o app vai cobrar.

- **Manrope** — wordmark, títulos e headlines. Pesos 600 e 700.
- **Inter** — interface, corpo, botões e metadados. Pesos 400, 500 e 600.

### Escala

| Uso | Fonte | Tamanho | Peso |
|---|---|---|---|
| Headline principal | Manrope | 32–40px | 700 |
| Título de card | Manrope | 20–24px | 600 |
| Kicker (maiúsculas) | Inter | 11px, tracking +0.12em | 600 |
| Corpo / sinopse | Inter | 14–15px | 400 |
| Botão | Inter | 15px | 500 |
| Metadado | Inter | 12–13px | 400 |

```css
--cg-font-display: 'Manrope', system-ui, -apple-system, 'Segoe UI', sans-serif;
--cg-font-ui: 'Inter', system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif;
```

Aplique tracking de -0.02em no wordmark em tamanhos grandes — fonte
geométrica em corpo grande fica solta sem esse ajuste.

**Antes de publicar:** converta o texto "CineGiro" dos SVGs de lockup em
curvas (outline). Hoje eles usam pilha de fontes de sistema como provisório;
sem a conversão, o logo muda de aparência em máquina que não tenha a fonte.

## Favicon

O favicon tem o anel propositalmente mais grosso que o símbolo principal.
A 16px o vão entre o play e o anel fecha na versão normal; a versão
engrossada mantém a leitura. Os dois desenhos são o mesmo logo — ninguém
percebe a diferença nesse tamanho.
