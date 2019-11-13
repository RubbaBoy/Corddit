CREATE TABLE IF NOT EXISTS `users` (
    reddit VARCHAR(128),
    discord BIGINT UNIQUE
);