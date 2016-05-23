package test

object BaseScheme {
  def crateSql: String = """
    CREATE TABLE "user"
    (
      id INTEGER AUTO_INCREMENT PRIMARY KEY,
      email VARCHAR(128) NOT NULL,
      "password_hash" VARCHAR(32) NOT NULL,
      "active" BOOLEAN NOT NULL,
      "rating" INTEGER NULL,
      "verbose" BOOLEAN NULL,
      "js" VARCHAR(512) NOT NULL DEFAULT '{}',
      "lastLogin" TIMESTAMP NOT NULL
    );

                         """

  def truncateSql: String = """
    TRUNCATE TABLE "user";
                            """
}
