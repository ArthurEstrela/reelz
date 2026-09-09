# Identidade refinada do CineGiro

Esta pasta preserva uma segunda geração da marca sem alterar os arquivos de
`logomarca/`. O sistema visual foi alinhado à interface real do produto e está
pronto para receber os últimos testes de marca e aplicação.

## Conceito

O símbolo deve ser lido como **C + giro + play**:

- o `C` identifica CineGiro;
- o arco circular representa o giro e a redução da indecisão;
- o play representa o resultado acionável: sair da escolha e assistir.

Os terminais e o play foram arredondados para conversar com os cards, pills e
botões do produto. O desenho continua simples o bastante para funcionar como
favicon e pode ganhar movimento: na roleta, o arco gira e o play permanece
fixo.

## Versão principal

Em fundo escuro, use `cinegiro-lockup-horizontal-escuro.svg`: símbolo vermelho
e wordmark creme. Em fundo claro, use `cinegiro-lockup-horizontal-claro.svg`:
símbolo vermelho e wordmark quase preto.

As versões totalmente branca ou preta são reservadas para aplicações
monocromáticas. Não use a versão toda vermelha como lockup principal.

## Estrutura

```text
logomarca-refinada/
├── svg/                    vetores finais e independentes
├── png/                    exportações para PWA e plataformas
├── cores.css               tokens compartilhados com a interface
├── gerar_assets.py         fonte reproduzível dos vetores
└── exportar_png.ps1        exportação raster via Chrome ou Edge
```

### Principais arquivos SVG

- `cinegiro-simbolo.svg`: símbolo oficial vermelho.
- `cinegiro-simbolo-currentcolor.svg`: uso inline em React/CSS.
- `cinegiro-lockup-horizontal-escuro.svg`: cabeçalho em fundo escuro.
- `cinegiro-lockup-horizontal-claro.svg`: documentos e fundo claro.
- `cinegiro-lockup-empilhado-*.svg`: avatar, splash e peças quadradas.
- `cinegiro-icone-app.svg`: ícone comum com cantos transparentes.
- `cinegiro-icone-maskable.svg`: fundo integral para recortes do sistema.
- `cinegiro-favicon.svg`: desenho óptico próprio para 16–32 px.

O wordmark usa **Archivo 700 convertido em curvas**. Nenhum lockup depende de
uma fonte instalada no dispositivo.

## Uso e proteção

- Área de proteção: ao menos metade da altura do símbolo em todos os lados.
- Símbolo comum: mínimo recomendado de 24 px.
- Abaixo de 24 px: use o favicon.
- Lockup horizontal: mínimo recomendado de 120 px de largura.
- Não distorça, incline, contorne ou aplique sombra no símbolo.
- Não altere a relação de tamanho entre símbolo e wordmark.
- Sobre fotos, use a versão branca com uma superfície escura de apoio.

## PWA

Use os PNGs com os propósitos correspondentes:

- `cinegiro-icon-192.png` e `cinegiro-icon-512.png`: `purpose: any`;
- `cinegiro-maskable-512.png`: `purpose: maskable`;
- `apple-touch-icon-180.png`: Apple Touch Icon;
- `cinegiro-favicon-32.png`: fallback raster do favicon.

O ícone maskable não possui cantos arredondados próprios. O sistema operacional
é responsável pelo recorte; o símbolo fica dentro da área segura central.

## Regeneração

Pré-requisitos: Python, `fonttools`, `brotli` e Google Chrome ou Microsoft Edge.

```powershell
python -m pip install fonttools brotli
python logomarca-refinada/gerar_assets.py
powershell -ExecutionPolicy Bypass -File logomarca-refinada/exportar_png.ps1
```

Antes do registro comercial definitivo, faça uma pesquisa de anterioridade de
marca nominativa e figurativa. A pasta original deve ser mantida como histórico
e proveniência; os arquivos desta pasta são as cópias otimizadas para uso.
