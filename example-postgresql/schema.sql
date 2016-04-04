CREATE DATABASE "example";

CREATE TABLE "user"
(
  id BIGSERIAL PRIMARY KEY,
  email character varying(128) NOT NULL,
  "password_hash" character varying(32) NOT NULL,
  "active" boolean NOT NULL,
  "rating" integer NULL,
  "verbose" boolean NULL,
--   "js" jsonb NOT NULL DEFAULT '{}'
)
WITH (
  OIDS=FALSE
);

