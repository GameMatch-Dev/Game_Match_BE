CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id)
) ENGINE = InnoDB;

CREATE TABLE game (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    sort VARCHAR(50) NOT NULL,
    url VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id)
) ENGINE = InnoDB;

CREATE TABLE game_user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    game_id BIGINT NOT NULL,
    nickname VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_game_user_user_id_game_id UNIQUE (user_id, game_id)
) ENGINE = InnoDB;

CREATE TABLE login_identity (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    provider VARCHAR(30) NOT NULL,
    provider_subject VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_login_identity_provider_subject UNIQUE (provider, provider_subject),
    CONSTRAINT fk_login_identity_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB;
