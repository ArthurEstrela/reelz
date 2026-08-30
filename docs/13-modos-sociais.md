# Modos Casal e Grupo

## Escopo do primeiro beta

O modo social usa salas privadas autenticadas. `COUPLE` aceita duas pessoas e `GROUP` aceita até oito. Todos precisam de uma conta e onboarding concluído.

O anfitrião cria a sala e compartilha um código de oito caracteres ou o link `/social/join/{inviteCode}`. Somente ele pode girar. Encerrar a participação do anfitrião fecha a sala; os demais membros podem sair sem encerrá-la.

## Regra do sorteio

O conjunto elegível é formado por filmes que:

- estão disponíveis no país do anfitrião;
- pertencem aos provedores selecionados entre os streamings em comum dos membros;
- atendem a pelo menos um palpite de cada participante;
- não aparecem como `WATCHED` no histórico de nenhum participante ativo da sala.

Cada pessoa escolhe até três gêneros e uma vibe opcional. Dentro do palpite de uma pessoa vale `OR`; entre participantes vale `AND`. Por exemplo, quem escolhe Ação ou Comédia aceita qualquer uma dessas opções, mas o filme também precisa combinar com pelo menos uma escolha de cada outra pessoa.

Todos confirmam “Estou pronto” antes do giro. O anfitrião apenas seleciona, conforme seu plano, quais streamings em comum serão usados e dispara a roleta. Ele não adiciona filtros próprios por cima dos palpites coletivos. Depois de um giro novo, a prontidão é reiniciada para que ninguém reutilize consentimento antigo no próximo sorteio.

O sorteio continua usando `ORDER BY RANDOM() LIMIT 1`. A criação de sala `GROUP` é exclusiva do anfitrião Premium, mas qualquer conta autenticada pode entrar pelo convite. O modo `COUPLE` continua disponível no Free. A quota e o plano do anfitrião são usados no giro: conta Free seleciona um provedor e consome um giro; Premium pode cruzar vários provedores e possui giros ilimitados.

## Concorrência e idempotência

O backend adquire lock pessimista na sala antes de girar. Isso serializa cliques concorrentes e protege a sequência compartilhada. O motor original mantém o optimistic locking da quota diária e a chave de idempotência. Repetir a mesma requisição devolve o resultado anterior sem consumir outro giro ou incrementar a sequência da sala.

Cada resultado é armazenado como snapshot em `social_room_spin`. Assim, todos os membros veem o mesmo filme mesmo se o catálogo externo mudar depois.

## Sincronização do cliente

O cliente consulta `GET /api/v1/social/rooms/{roomId}` a cada três segundos. Polling reduz a complexidade operacional do beta; WebSockets podem ser avaliados depois que o uso simultâneo justificar conexões persistentes.

## Endpoints

- `POST /api/v1/social/rooms`: cria uma sala.
- `GET /api/v1/social/rooms`: lista salas do usuário.
- `POST /api/v1/social/rooms/join`: entra por código.
- `GET /api/v1/social/rooms/{roomId}`: consulta lobby e último resultado.
- `PUT /api/v1/social/rooms/{roomId}/members/me/preferences`: salva o palpite e a prontidão do participante.
- `POST /api/v1/social/rooms/{roomId}/spin`: gira como anfitrião.
- `DELETE /api/v1/social/rooms/{roomId}/members/me`: sai ou encerra a sala.

## Fora deste corte

Votação ou veto depois do resultado, chat, presença em tempo real, convidados anônimos e transferência de anfitrião ficaram fora do primeiro beta. Esses itens devem ser priorizados com dados de uso das salas, não por antecipação.

O cockpit administrativo já expõe salas criadas, salas que chegaram ao primeiro giro, quantidade de giros sociais e participantes únicos. Esses quatro números formam o funil inicial de validação do recurso.
