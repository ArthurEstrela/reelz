INSERT INTO vibe (slug, label, description, genre_ids, query_rules, rules_version, active)
VALUES
    ('para-rir', 'Para rir', 'Comédias para desligar e se divertir.', ARRAY[35], '{}'::jsonb, 1, TRUE),
    ('tensao', 'Tensão', 'Suspense, mistério, crime e terror.', ARRAY[53, 27, 80, 9648], '{}'::jsonb, 1, TRUE),
    ('leve', 'Leve', 'Histórias leves para uma sessão tranquila.', ARRAY[35, 10751, 10749, 16], '{}'::jsonb, 1, TRUE),
    ('adrenalina', 'Adrenalina', 'Ação, aventura e ficção científica.', ARRAY[28, 12, 878], '{}'::jsonb, 1, TRUE),
    ('para-emocionar', 'Para emocionar', 'Dramas e romances para sentir de verdade.', ARRAY[18, 10749], '{}'::jsonb, 1, TRUE),
    ('outro-mundo', 'Outro mundo', 'Fantasia, aventura e ficção científica.', ARRAY[14, 12, 878], '{}'::jsonb, 1, TRUE)
ON CONFLICT (slug) DO UPDATE SET
    label = EXCLUDED.label,
    description = EXCLUDED.description,
    genre_ids = EXCLUDED.genre_ids,
    query_rules = EXCLUDED.query_rules,
    rules_version = EXCLUDED.rules_version,
    active = TRUE,
    updated_at = CURRENT_TIMESTAMP;
