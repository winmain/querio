CREATE DATABASE example;
USE example;

-- -----------------------------------------------------
-- Table `example`.`user`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `example`.`user` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `email` VARCHAR(255) NOT NULL COMMENT 'user valid email',
  `password_hash` VARCHAR(255) NOT NULL COMMENT 'hashed password (md5)',
  `active` TINYINT(1) NOT NULL COMMENT 'is user active and can login?',
  `rating` INT NULL COMMENT 'just simple int field',
  PRIMARY KEY (`id`))
ENGINE = InnoDB
COMMENT = 'Common user';
