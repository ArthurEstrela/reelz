# Reelz Web

Front-end mobile-first do Reelz, construído com React, TypeScript, Vite, Tailwind CSS e React Router DOM v6.

## Executar

```bash
npm install
npm run dev
```

Por padrão, o Vite encaminha `/api` para `http://localhost:8080`. Em outros ambientes, copie `.env.example` para `.env.local` e defina:

```dotenv
VITE_API_URL=https://api.example.com
```

Não inclua `/api` no final da variável; os serviços já utilizam caminhos como `/api/v1/auth/login`.

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
│   └── form/       # campos e botões
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

## Dependência do React Router

O projeto permanece na linha v6 por requisito desta etapa. O `npm audit` informa advisories moderados cuja correção oficial exige migração para v7. A navegação de retorno usada no login é sanitizada e o projeto é uma SPA sem hidratação SSR, reduzindo a exposição aos vetores reportados, mas a migração para v7 deve entrar no backlog técnico.
