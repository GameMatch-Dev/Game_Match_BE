CREATE TABLE match_request (
    id BIGINT NOT NULL AUTO_INCREMENT,
    match_id BIGINT NOT NULL,
    game_user_id BIGINT NOT NULL,
    state VARCHAR(20) NOT NULL,
    comment TEXT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_match_request_match_id_game_user_id UNIQUE (match_id, game_user_id)
) ENGINE = InnoDB;

CREATE TABLE match_request_member (
    request_id BIGINT NOT NULL,
    game_user_id BIGINT NOT NULL,
    member_order INT NOT NULL,
    PRIMARY KEY (request_id, member_order),
    CONSTRAINT fk_match_request_member_request FOREIGN KEY (request_id) REFERENCES match_request (id)
) ENGINE = InnoDB;
