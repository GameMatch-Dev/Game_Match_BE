CREATE TABLE friendly_match (
    id BIGINT NOT NULL AUTO_INCREMENT,
    host_game_user_id BIGINT NOT NULL,
    game_id BIGINT NOT NULL,
    match_date DATE NOT NULL,
    match_time TIME NOT NULL,
    sort VARCHAR(20) NOT NULL,
    state VARCHAR(20) NOT NULL,
    recruit INT NOT NULL,
    comment VARCHAR(500) NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB;
