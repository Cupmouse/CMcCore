
# 実績リスト

CREATE TABLE `achievements` (
	`achievement_type` SMALLINT(5) UNSIGNED NOT NULL,
	`name` TINYTEXT NOT NULL COLLATE 'utf8mb4_unicode_ci',
	PRIMARY KEY (`achievement_type`)
)
COLLATE='utf8mb4_unicode_ci'
ENGINE=InnoDB
;

INSERT INTO achievements VALUES (0, 'first_login');

# サーバーセッション

CREATE TABLE `server_session` (
	`session_id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
	`start_datetime` DATETIME NOT NULL,
	`last_datetime` DATETIME NULL DEFAULT NULL,
	`status` ENUM('starting','running','stopped') NOT NULL DEFAULT 'starting' COLLATE 'utf8mb4_unicode_ci',
	PRIMARY KEY (`session_id`)
)
COLLATE='utf8mb4_unicode_ci'
ENGINE=InnoDB
;

# ユーザー

CREATE TABLE `users` (
	`user_id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
	`uuid` BINARY(16) NOT NULL,
	`name` VARCHAR(16) NULL DEFAULT NULL COLLATE 'utf8mb4_unicode_ci',
	`playing_sec` BIGINT(20) UNSIGNED NOT NULL DEFAULT '0',
	`address` VARBINARY(16) NULL DEFAULT NULL,
	PRIMARY KEY (`user_id`)
)
COLLATE='utf8mb4_unicode_ci'
ENGINE=InnoDB
;

# ユーザー実績

CREATE TABLE `user_achievement` (
	`user_id` INT(10) UNSIGNED NULL DEFAULT NULL,
	`achievement_type` SMALLINT(5) UNSIGNED NOT NULL,
	`datetime` DATETIME(3) NOT NULL,
	UNIQUE INDEX `user_id_achivement_type` (`user_id`, `achievement_type`),
	INDEX `FK_user_achievement_achivements` (`achievement_type`),
	CONSTRAINT `FK_user_achievement_achivements` FOREIGN KEY (`achievement_type`) REFERENCES `achievements` (`achievement_type`) ON UPDATE CASCADE ON DELETE CASCADE,
	CONSTRAINT `FK_user_achievement_users` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON UPDATE CASCADE ON DELETE SET NULL
)
COLLATE='utf8mb4_unicode_ci'
ENGINE=InnoDB
;

# ユーザーセッション

CREATE TABLE `user_session` (
	`session_id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
	`user_id` INT(10) UNSIGNED NULL DEFAULT NULL,
	`login_datetime` DATETIME(3) NOT NULL,
	`disconnect_datetime` DATETIME(3) NULL DEFAULT NULL,
	PRIMARY KEY (`session_id`),
	INDEX `FK_user_session_users` (`user_id`),
	CONSTRAINT `FK_user_session_users` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON UPDATE CASCADE ON DELETE SET NULL
)
COLLATE='utf8mb4_unicode_ci'
ENGINE=InnoDB
;
