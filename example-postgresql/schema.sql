CREATE DATABASE "example";

CREATE TABLE "user"
(
  id SERIAL PRIMARY KEY,
  "email" character varying(128) NOT NULL,
  "password_hash" character varying(32) NOT NULL,
  "active" boolean NOT NULL,
  "rating" integer NULL
)
WITH (
  OIDS=FALSE
);

