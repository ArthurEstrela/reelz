# Reelz Web

Front-end mobile-first do Reelz, construído com React, TypeScript, Vite, Tailwind CSS, React Router v8 e Framer Motion.

## Executar

Use Node.js 22.22.0 ou superior.

```bash
npm install
npm run dev
```

Por padrão, o Vite encaminha `/api` para `http://localhost:8080`. Em outros ambientes, copie `.env.example` para `.env.local` e defina:

```dotenv
VITE_API_URL=https://api.example.com
```

Não inclua `/api` no final de `VITE_API_URL`; os serviços já utilizam caminhos como `/api/v1/auth/login`.
Provedores e vibes são carregados dos endpoints autenticados de catálogo; não precisam de configuração no build do front-end.

Em produção, prefira servir front-end e API sob a mesma origem por meio de um reverse proxy. Se forem usados domínios diferentes, o backend deverá liberar por CORS somente a origem exata do front-end.

## Comandos

```bash
npm run lint
npm run test
npm run build
```

## Estrutura

```text
src/
├── components/
│   ├── auth/       # composição visual das telas de acesso
│   ├── brand/      # identidade visual reutilizável
│   ├── feedback/   # alertas e mensagens
│   ├── form/       # campos e botões
│   └── roulette/   # filtros, slot, resultado e modal de limite
├── config/         # gêneros do TMDB ainda sem catálogo no backend
├── context/        # estado e provider de autenticação
├── hooks/          # acesso tipado aos contextos
├── pages/          # Login, Cadastro e Home
├── routes/         # definição e guards de rota
├── services/       # Axios e contratos HTTP
├── storage/        # persistência da sessão
├── test/           # setup global do Vitest
├── types/          # contratos TypeScript
└── utils/          # erros da API e locale do browser
```

## Autenticação

- A sessão fica no `sessionStorage`, limitada à aba atual.
- O interceptor injeta `Authorization: Bearer <token>`.
- Uma resposta 401 limpa a sessão e notifica o `AuthProvider`.
- `PrivateRoute` redireciona usuários anônimos para `/login`.
- O e-mail é preservado junto à sessão porque o contrato atual de login retorna somente token e `userId`.

## Roleta

- `POST /api/v1/roulette/spin` recebe uma chave de idempotência nova e os filtros selecionados.
- Providers e vibes são carregados de `/api/v1/catalog/providers` e `/api/v1/catalog/vibes`, com skeleton durante a espera.
- A franquia é sincronizada na montagem e depois de cada sucesso por `/api/v1/roulette/usage/today`.
- Respostas 404 exibem uma falha animada e sugerem novos filtros; 403/429 abrem o modal de limite.
- Usuários Free escolhem um provedor por vez. Quando a resposta informa cota ilimitada, múltiplos provedores são liberados.
- O botão “Já vi / Girar de novo” inicia a animação e registra o filme em `/api/v1/history`.
- A nova busca aguarda o histórico terminar, garantindo consistência read-after-write sem congelar a interface.
- Falhas no histórico são isoladas em um toast e não impedem a continuação do giro.

## Biblioteca

- A rota privada `/library` lista somente filmes assistidos em uma grade responsiva de pôsteres.
- O contador usa `totalElements` do backend, representando toda a coleção e não apenas a página carregada.
- As próximas páginas são carregadas sob demanda pelo botão “Carregar mais”.
- A navegação inferior permite alternar entre Roleta e Biblioteca em uma experiência adequada para PWA.

## Dependência do React Router

O projeto usa `react-router` 8.3 ou superior. Essa linha contém as correções de segurança indisponíveis simultaneamente nas versões anteriores e exige Node.js 22.22.0+, React 19.2.7+ e Vite 7+.
