CREATE DATABASE `example`;

CREATE TABLE `user` (
  `id`            INT(11)   NOT NULL AUTO_INCREMENT,
  `email`         CHAR(128) NOT NULL
  COMMENT 'user valid email',
  `password_hash` CHAR(32)  NOT NULL
  COMMENT 'hashed password (md5)',
  `active`        BOOL      NOT NULL
  COMMENT 'is user active and can login?',
  `rating`        INT(11)   NULL
  COMMENT 'just simple int field',
  PRIMARY KEY (`id`)
);
