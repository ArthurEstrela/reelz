CREATE TABLE achievement_definition (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(60) NOT NULL,
    name VARCHAR(80) NOT NULL,
    description VARCHAR(220) NOT NULL,
    icon_key VARCHAR(40) NOT NULL,
    category VARCHAR(30) NOT NULL,
    target_value BIGINT NOT NULL,
    display_order INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_achievement_definition_code UNIQUE (code),
    CONSTRAINT uk_achievement_definition_order UNIQUE (display_order),
    CONSTRAINT ck_achievement_definition_target CHECK (target_value > 0),
    CONSTRAINT ck_achievement_definition_category CHECK (
        category IN ('DISCOVERY', 'COLLECTION', 'EXPLORATION', 'SOCIAL', 'HABIT')
    )
);

CREATE TABLE user_achievement (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    achievement_id UUID NOT NULL REFERENCES achievement_definition(id) ON DELETE CASCADE,
    progress_value BIGINT NOT NULL DEFAULT 0,
    unlocked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_achievement_user_definition UNIQUE (user_id, achievement_id),
    CONSTRAINT ck_user_achievement_progress CHECK (progress_value >= 0)
);

CREATE INDEX idx_user_achievement_user_unlocked
    ON user_achievement (user_id, unlocked_at DESC NULLS LAST);

-- A sala pode perder membros depois do giro. O snapshot preserva quem realmente
-- participou e impede que uma conquista social desapareça retroativamente.
CREATE TABLE social_room_spin_participant (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    spin_id UUID NOT NULL REFERENCES social_room_spin(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    room_type VARCHAR(20) NOT NULL,
    participant_count INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_social_room_spin_participant UNIQUE (spin_id, user_id),
    CONSTRAINT ck_social_spin_participant_room_type CHECK (room_type IN ('COUPLE', 'GROUP')),
    -- Backfills legados podem conhecer apenas o membro que ainda permanece na sala.
    -- Novos giros sempre gravam o tamanho real (mínimo 2) no serviço.
    CONSTRAINT ck_social_spin_participant_count CHECK (participant_count >= 1)
);

CREATE INDEX idx_social_spin_participant_user
    ON social_room_spin_participant (user_id, room_type, created_at DESC);

INSERT INTO social_room_spin_participant (
    spin_id, user_id, room_type, participant_count, created_at
)
SELECT spin.id,
       member.user_id,
       room.room_type,
       COUNT(*) OVER (PARTITION BY spin.id),
       spin.created_at
  FROM social_room_spin spin
  JOIN social_room room ON room.id = spin.room_id
  JOIN social_room_member member ON member.room_id = room.id;

INSERT INTO achievement_definition (
    code, name, description, icon_key, category, target_value, display_order
) VALUES
    ('FIRST_SPIN', 'Primeira Sessão', 'Encontre seu primeiro filme na roleta.', 'ticket', 'DISCOVERY', 1, 10),
    ('OPEN_PROVIDER', 'Sem Enrolação', 'Abra um streaming a partir de uma escolha do Reelz.', 'play', 'DISCOVERY', 1, 20),
    ('WATCHED_10', 'Arquivo Pessoal', 'Colecione 10 filmes que você já assistiu.', 'film-stack', 'COLLECTION', 10, 30),
    ('WATCHED_50', 'Cinéfilo de Carteirinha', 'Colecione 50 filmes que você já assistiu.', 'membership', 'COLLECTION', 50, 40),
    ('WATCHED_100', 'Acervo Vivo', 'Colecione 100 filmes que você já assistiu.', 'archive', 'COLLECTION', 100, 50),
    ('WATCHLIST_5', 'Na Reserva', 'Guarde 5 filmes para assistir depois.', 'bookmark', 'COLLECTION', 5, 60),
    ('GENRES_5', 'Saindo da Bolha', 'Passe por 5 gêneros diferentes na sua coleção.', 'compass', 'EXPLORATION', 5, 70),
    ('COUPLE_SPIN', 'Date Night', 'Conclua uma escolha no modo casal.', 'hearts', 'SOCIAL', 1, 80),
    ('GROUP_SPIN_3', 'Cineclube', 'Conclua uma escolha em grupo com pelo menos 3 pessoas.', 'people', 'SOCIAL', 1, 90),
    ('ACTIVE_WEEKS_4', 'Sessão Marcada', 'Use a roleta em 4 semanas diferentes.', 'calendar', 'HABIT', 4, 100);
