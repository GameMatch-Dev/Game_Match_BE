ALTER TABLE friendly_match
    ADD COLUMN host_roster_confirmed BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE friendly_match_host_member (
    match_id BIGINT NOT NULL,
    game_user_id BIGINT NOT NULL,
    member_order INT NOT NULL,
    PRIMARY KEY (match_id, member_order),
    CONSTRAINT fk_friendly_match_host_member_match FOREIGN KEY (match_id) REFERENCES friendly_match (id)
);
