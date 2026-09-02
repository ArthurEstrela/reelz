UPDATE achievement_definition
SET description = 'Abra um streaming a partir de uma escolha do CineGiro.',
    updated_at = CURRENT_TIMESTAMP
WHERE code = 'OPEN_PROVIDER'
  AND description = 'Abra um streaming a partir de uma escolha do Reelz.';
