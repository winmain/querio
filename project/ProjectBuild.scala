import sbt.Keys._
import sbt._

object ProjectBuild extends sbt.Build {
  val buildScalaVersion = "2.11.7"
  val module = "querio"

  ///////////////////////  Settings ///////////////////////////

  val commonSettings = Seq(
    organization := "com.github.winmain",
    version := "0.4.99-SNAPSHOT",
    publishTo := (if (isSnapshot.value) Some("snapshots" at "http://nexus/content/repositories/snapshots") else None),
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),

    incOptions := incOptions.value.withNameHashing(nameHashing = true),
    resolvers ++= Seq("Typesafe releases" at "http://repo.typesafe.com/typesafe/releases"),
    sources in doc in Compile := List(), // Выключить генерацию JavaDoc, ScalaDoc
    scalaVersion := buildScalaVersion,
    scalacOptions ++= Seq("-target:jvm-1.8", "-unchecked", "-deprecation", "-feature", "-language:existentials"),

    sourceDirectories in Compile := Seq(baseDirectory.value / "src"),
    scalaSource in Compile := baseDirectory.value / "src",
    javaSource in Compile := baseDirectory.value / "src",
    resourceDirectory in Compile := baseDirectory.value / "resources",

    scalaSource in Test := baseDirectory.value / "test",
    javaSource in Test := baseDirectory.value / "test",
    resourceDirectory in Test := baseDirectory.value / "test/resources",

    libraryDependencies += "com.google.code.findbugs" % "jsr305" % "2.0.1",
    libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.4",
    libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.12",
    libraryDependencies += "com.github.scala-incubator.io" %% "scala-io-file" % "0.4.3-1",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.0-M15" % "test",
    libraryDependencies += "org.scalamock" %% "scalamock-scalatest-support" % "3.2.2" % "test",
    libraryDependencies += "org.postgresql" % "postgresql" % "9.3-1101-jdbc4" % "optional",
    libraryDependencies += "org.json4s" % "json4s-jackson_2.10" % "3.3.0" % "optional"
  )

  val testH2Settings = Seq(
    name := "querio-test-h2",
    version := "0.1",
    scalaVersion := "2.11.7",
    //  libraryDependencies += "com.github.winmain" %% "querio" % "0.4.3-SNAPSHOT",

    //  libraryDependencies += "com.h2database" % "h2" % "1.4.191",
    libraryDependencies += "com.h2database" % "h2" % "1.3.175",
    libraryDependencies += "org.json4s" % "json4s-jackson_2.10" % "3.3.0",
    libraryDependencies += "org.specs2" % "specs2_2.11" % "3.7",


    sourceDirectories in Compile := Seq(baseDirectory.value / "src"),
    scalaSource in Compile := baseDirectory.value / "src",
    javaSource in Compile := baseDirectory.value / "src",

    scalaSource in Test := baseDirectory.value / "test",
    javaSource in Test := baseDirectory.value / "test"
  )

  val testPostgreSQLSettings = Seq(
    name := "querio-test-postgresql",
    version := "0.1",
    scalaVersion := "2.11.7",
    //  libraryDependencies += "com.github.winmain" %% "querio" % "0.4.3-SNAPSHOT",
    libraryDependencies += "org.postgresql" % "postgresql" % "9.3-1101-jdbc4",
    libraryDependencies += "org.json4s" % "json4s-jackson_2.10" % "3.3.0",
    libraryDependencies += "org.specs2" % "specs2_2.11" % "3.7",
    libraryDependencies += "com.opentable.components" % "otj-pg-embedded" % "0.5.0",

    sourceDirectories in Compile := Seq(baseDirectory.value / "src"),
    scalaSource in Compile := baseDirectory.value / "src",
    javaSource in Compile := baseDirectory.value / "src",

    scalaSource in Test := baseDirectory.value / "test",
    javaSource in Test := baseDirectory.value / "test"
  )

  ///////////////////////  projects ///////////////////////////

  lazy val main: Project = Project("querio", base = file("."), settings = commonSettings).settings(
    name := "querio",
    genQuerioLibSourcesTask
  ).dependsOn(querioCodegen).aggregate(querioCodegen)

  lazy val querioCodegen = Project("querio-codegen",
    base = file("codegen"),
    settings = commonSettings).settings(
    name := "querio-codegen"
  )


  lazy val test_h2 = Project(id = "test-h2",
    base = file("test-h2"),
    settings = testH2Settings).settings(
    name := "test-h2"
    )

  lazy val test_postgresql= Project(id = "test-postgresql",
    base = file("test-postgresql"),
    settings = testPostgreSQLSettings).settings(
    name := "test-postgresql"
    ).dependsOn(main)

  ///////////////////////  Tasks ///////////////////////////

  // Task: Generate some querio lib sources
  val genQuerioLibSources = TaskKey[Unit]("gen-querio-lib-sources")
  lazy val genQuerioLibSourcesTask = genQuerioLibSources <<=
    (scalaSource in Compile, dependencyClasspath in Compile,
      baseDirectory in Compile, classDirectory in Runtime) map {
      (scalaSource, classPath, baseDir, classesDir) => {
        runScala(classPath.files :+ baseDir :+ classesDir, "querio.codegen.SelfClassesGenerator",
          Seq(scalaSource.absolutePath))
      }
    }

  val genTestH2DbSources = TaskKey[Unit]("gen-test-h2-db-sources")
  lazy val genTestH2DbSourcesTask = genTestH2DbSources <<=
    (scalaSource in Compile, dependencyClasspath in Compile,
      baseDirectory in Compile, classDirectory in Runtime) map {
      (scalaSource, classPath, baseDir, classesDir) => {
        runScala(classPath.files :+ baseDir :+ classesDir, "test.SourcesGenerator",
          Seq(scalaSource.absolutePath))
      }
    }

  val genTestPostgreSqlDbSources = TaskKey[Unit]("gen-test-postgresql-db-sources")
  lazy val genTestPostgreSqlDbSourcesTask = genTestPostgreSqlDbSources <<=
    (scalaSource in Compile, dependencyClasspath in Compile,
      baseDirectory in Compile, classDirectory in Runtime) map {
      (scalaSource, classPath, baseDir, classesDir) => {
        runScala(classPath.files :+ baseDir :+ classesDir, "test.SourcesGenerator",
          Seq(scalaSource.absolutePath))
      }
    }

  /*
      // Task: Сгенерировать классы для таблиц БД
      val genDbSources = TaskKey[Unit]("gen-db-sources")
      lazy val genDbSourcesTask = genDbSources <<=
        (scalaSource in Compile in main, dependencyClasspath in Compile, baseDirectory in Compile, classDirectory in Runtime) map {
          (scalaSource, classPath, baseDir, classesDir) => {
            runScala(classPath.files :+ baseDir :+ classesDir, "orm.codegen.TodoGenerator", Seq(scalaSource.absolutePath))
          }
        }
    */


  /**
   * Запустить scala класс кодогенерации в отдельном процессе
   */
  def runScala(classPath: Seq[File], className: String, arguments: Seq[String]) {
    val ret: Int = new Fork("java", Some(className)).apply(ForkOptions(bootJars = classPath), arguments)
    if (ret != 0) sys.error("Trouble with code generator")
  }

}
