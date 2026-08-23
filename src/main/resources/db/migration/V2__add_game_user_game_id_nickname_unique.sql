ALTER TABLE game_user
    ADD CONSTRAINT uk_game_user_game_id_nickname
    UNIQUE (game_id, nickname);
