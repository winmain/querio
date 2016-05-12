package test

object BaseScheme {
  def sql: String = """
    CREATE TABLE "user"
    (
      id BIGSERIAL PRIMARY KEY,
      email character varying(128) NOT NULL,
      "password_hash" character varying(32) NOT NULL,
      "active" boolean NOT NULL,
      "rating" integer NULL,
      "verbose" boolean NULL,
      "js_b" jsonb NOT NULL DEFAULT '{}',
      "js" json NOT NULL DEFAULT '{}',
      "lastLogin" timestamp without time zone NOT NULL
    )
    WITH (
      OIDS=FALSE
    );

    CREATE TABLE "level"
    (
      id BIGSERIAL PRIMARY KEY,
      "userId" bigint NOT NULL,
      "level" int NOT NULL,
      "score" int NOT NULL DEFAULT 0,
      "complete" boolean NOT NULL DEFAULT false,
      "createdAt" timestamp without time zone NOT NULL
    )
    WITH (
      OIDS=FALSE
    );

    CREATE TABLE "purchase"
    (
      id BIGSERIAL PRIMARY KEY,
      "userId" bigint NOT NULL,
      "purchaseCode" int NOT NULL,
      "price" int NOT NULL,
      "level" int
    )
    WITH (
      OIDS=FALSE
    );

                    """
}
