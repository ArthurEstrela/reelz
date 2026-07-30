# Reelz Web

Front-end mobile-first do Reelz, construído com React, TypeScript, Vite, Tailwind CSS, React Router DOM v6 e Framer Motion.

## Executar

```bash
npm install
npm run dev
```

Por padrão, o Vite encaminha `/api` para `http://localhost:8080`. Em outros ambientes, copie `.env.example` para `.env.local` e defina:

```dotenv
VITE_API_URL=https://api.example.com
VITE_STREAMING_PROVIDERS=[{"id":"uuid-real-do-provider","label":"Netflix","emoji":"N"}]
VITE_VIBES=[{"id":"uuid-real-da-vibe","label":"Para rir","emoji":"😂"}]
```

Não inclua `/api` no final de `VITE_API_URL`; os serviços já utilizam caminhos como `/api/v1/auth/login`.
Os IDs de provedores e vibes precisam ser os UUIDs reais cadastrados no PostgreSQL. Enquanto o backend não expuser endpoints de catálogo, essas opções são lidas do ambiente; os valores do `.env.example` são apenas exemplos.

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
├── config/         # catálogo configurável de filtros da roleta
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
- O resultado atualiza o contador de giros usando a cota devolvida pelo backend.
- Respostas 404 exibem uma falha animada e sugerem novos filtros; 403/429 abrem o modal de limite.
- Usuários Free escolhem um provedor por vez. Quando a resposta informa cota ilimitada, múltiplos provedores são liberados.
- O botão “Já vi / Girar de novo” executa um novo giro. Persistir “Já assisti” depende de um endpoint de histórico, ainda não exposto pelo backend.

## Dependência do React Router

O projeto permanece na linha v6 por requisito desta etapa. O `npm audit` informa advisories moderados cuja correção oficial exige migração para v7. A navegação de retorno usada no login é sanitizada e o projeto é uma SPA sem hidratação SSR, reduzindo a exposição aos vetores reportados, mas a migração para v7 deve entrar no backlog técnico.
