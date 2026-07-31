# Preferências de streaming

As plataformas que o usuário assina são persistidas separadamente dos filtros usados em cada giro.
Isso permite que uma conta Free cadastre vários serviços, mas continue selecionando somente um deles
por giro. Contas Premium podem selecionar vários serviços simultaneamente.

## API

As duas rotas exigem JWT e usam o usuário autenticado; nenhum `userId` é aceito no payload.

### Consultar preferências

```http
GET /api/v1/users/me/streaming-preferences
```

```json
{
  "providerIds": [
    "6ad73953-bcf3-41dd-988c-cf997ed45c2b"
  ]
}
```

### Substituir preferências

```http
PUT /api/v1/users/me/streaming-preferences
Content-Type: application/json
```

```json
{
  "providerIds": [
    "6ad73953-bcf3-41dd-988c-cf997ed45c2b",
    "9e43cb1e-0a86-40e7-b65e-2c508da7b3f2"
  ]
}
```

O `PUT` possui semântica de substituição completa. Uma lista vazia limpa as preferências. O limite é
de 20 provedores e somente serviços ativos, com ofertas elegíveis no país do usuário, são aceitos.

## Concorrência e consistência

O serviço bloqueia a linha do usuário durante a substituição. Assim, duas atualizações simultâneas não
intercalam exclusões e inserções na tabela pivô. A constraint única `(user_id, provider_id)` permanece
como última barreira de integridade.

## Frontend

- A Home carrega catálogo, vibes e preferências na inicialização.
- Quando existem preferências, os pills exibem somente os serviços que o usuário marcou.
- O modal **Meus streamings** permite editar todas as assinaturas sem aplicar o limite do plano Free.
- Se as preferências estiverem vazias, todos os provedores curados ficam visíveis.
- Se a consulta de preferências falhar, o catálogo continua utilizável e um toast informa o problema.
