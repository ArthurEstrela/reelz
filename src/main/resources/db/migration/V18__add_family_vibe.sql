INSERT INTO vibe (slug, label, description, genre_ids, query_rules, rules_version, active)
VALUES (
    'em-familia',
    'Em família',
    'Filmes para reunir todo mundo na sala.',
    ARRAY[10751],
    '{}'::jsonb,
    1,
    TRUE
)
ON CONFLICT (slug) DO UPDATE SET
    label = EXCLUDED.label,
    description = EXCLUDED.description,
    genre_ids = EXCLUDED.genre_ids,
    query_rules = EXCLUDED.query_rules,
    rules_version = EXCLUDED.rules_version,
    active = TRUE,
    updated_at = CURRENT_TIMESTAMP;
