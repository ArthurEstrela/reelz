# Etapa 4 — Spring Security e JWT

## Configuração obrigatória

O backend não possui segredo JWT padrão. Antes de iniciar a aplicação, defina `JWT_SECRET` como uma chave aleatória de pelo menos 256 bits codificada em Base64.

Exemplo para gerar uma chave no PowerShell:

```powershell
$jwtBytes = New-Object byte[] 32
[Security.Cryptography.RandomNumberGenerator]::Fill($jwtBytes)
[Convert]::ToBase64String($jwtBytes)
```

Variáveis suportadas:

| Variável | Obrigatória | Padrão |
|---|---:|---|
| `JWT_SECRET` | sim | nenhum |
| `JWT_ISSUER` | não | `reelz-api` |
| `JWT_EXPIRATION` | não | `PT2H` |
| `TERMS_VERSION` | não | `1.0` |
| `PRIVACY_VERSION` | não | `1.0` |

`JWT_EXPIRATION` usa o formato ISO-8601 de duração. Exemplos: `PT30M`, `PT2H` e `P1D`.

## Rotas públicas

- `POST /api/v1/users`
- `POST /api/v1/auth/login`
- `GET /actuator/health`
- `GET /actuator/health/**`

Todas as demais rotas exigem `Authorization: Bearer <token>`.

## Cadastro

```http
POST /api/v1/users
Content-Type: application/json
```

```json
{
  "displayName": "Arthur",
  "email": "arthur@example.com",
  "password": "senha-segura",
  "timezone": "America/Sao_Paulo",
  "countryCode": "BR",
  "termsAccepted": true
}
```

A senha é persistida exclusivamente como hash BCrypt. O cadastro também grava as versões aceitas dos Termos de Uso e da Política de Privacidade.

## Login

```http
POST /api/v1/auth/login
Content-Type: application/json
```

```json
{
  "email": "arthur@example.com",
  "password": "senha-segura"
}
```

```json
{
  "accessToken": "eyJ...",
  "tokenType": "Bearer",
  "expiresInSeconds": 7200,
  "userId": "2c09cc0a-0de5-41a9-96ae-7035fe8962c3"
}
```

Erros de login sempre usam a mensagem genérica `E-mail ou senha inválidos.`, sem revelar se uma conta existe.

## Fluxo autenticado da roleta

```http
POST /api/v1/roulette/spin
Authorization: Bearer eyJ...
Content-Type: application/json
```

O filtro valida assinatura, emissor e expiração. O `sub` do JWT contém o UUID do usuário e é convertido em `AuthenticatedUser`, resolvido no controller com `@AuthenticationPrincipal`.

## Testes autenticados

Para testes MVC que não precisam exercitar a assinatura JWT, use a anotação tipada:

```java
@Test
@WithMockCineGiroUser(userId = "11111111-1111-1111-1111-111111111111")
void shouldSpinAsAnAuthenticatedUser() {
    // ...
}
```

Para testar o filtro, envie um Bearer e configure `JwtService.extractUserId` no teste. Ambos os exemplos estão em `SecurityIntegrationTest`.

## Decisões de segurança

- API sem sessão (`STATELESS`).
- CSRF desabilitado porque a credencial é enviada no header Authorization, não em cookie automático.
- Form login, HTTP Basic e logout de sessão desabilitados.
- Respostas 401 e 403 seguem o mesmo contrato JSON dos erros de negócio.
- Segredo JWT exclusivamente externo ao repositório.
- Tokens de usuários removidos continuam válidos até expirar; revogação imediata e refresh tokens ficam para uma etapa posterior.
