-- Usuários de teste para demonstração e correção (documentados no README).
-- O cadastro público (/auth/register) só cria CIDADAO; este é o único GESTOR inicial.
-- Senhas armazenadas em BCrypt (custo 10), o mesmo PasswordEncoder da aplicação:
--   gestor@antesdachuva.com   / Gestor@2026
--   maria@antesdachuva.com    / Cidadao@2026
--   joao@antesdachuva.com     / Cidadao@2026
INSERT INTO t_user (user_id, name, email, password_hash, role, neighborhood, active) VALUES
  (1, 'Gestor Zeladoria', 'gestor@antesdachuva.com', '$2a$10$3UZmtfSFOqd2.DlFFjMyUOPOpVhPHB.vZ4iW7C8RLiDwgqwv/4Rqa', 'GESTOR', NULL, TRUE),
  (2, 'Maria Moradora', 'maria@antesdachuva.com', '$2a$10$9UK9ZgzvT4EpCjTQoMpG0eODrGPedi5Htt5B9vL3oi66f0tI/XVoC', 'CIDADAO', 'Jardim Pantanal', TRUE),
  (3, 'Joao Morador', 'joao@antesdachuva.com', '$2a$10$9UK9ZgzvT4EpCjTQoMpG0eODrGPedi5Htt5B9vL3oi66f0tI/XVoC', 'CIDADAO', 'Jardim Pantanal', TRUE);
