
-- Categorias do Oracle 04_carga.sql — IDs idênticos aos dados de exemplo.
-- Mantém a enumeração de categorias do contrato REST.
INSERT INTO t_category (category_id, name, description, base_weight, active) VALUES
  (1, 'Bueiro entupido', 'Bueiro ou boca de lobo obstruido por lixo, folhas ou terra', 4, TRUE),
  (2, 'Entulho em via publica', 'Restos de obra ou lixo volumoso que a chuva pode arrastar para a drenagem', 2, TRUE),
  (3, 'Ponto de alagamento', 'Local que acumula agua com frequencia quando chove', 5, TRUE),
  (4, 'Corrego assoreado', 'Corrego com terra, lixo ou mato que reduzem a vazao', 4, TRUE),
  (5, 'Arvore em risco', 'Arvore ou galho com risco de queda sobre via, fiacao ou drenagem', 3, TRUE);
