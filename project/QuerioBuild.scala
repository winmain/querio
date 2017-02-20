import bintray.BintrayKeys._
import sbt.Keys._
import sbt._

object QuerioBuild extends sbt.Build {
  val buildScalaVersion = "2.11.8"
  val module = "querio"

  // ------------------------------- Main projects -------------------------------

  val commonSettings = _root_.bintray.BintrayPlugin.bintrayPublishSettings ++ Seq(
    organization := "com.github.citrum.querio",
    version := "0.6.7",

    incOptions := incOptions.value.withNameHashing(nameHashing = true),
    sources in doc in Compile := List(), // Выключить генерацию JavaDoc, ScalaDoc
    scalaVersion := buildScalaVersion,
    scalacOptions ++= Seq("-target:jvm-1.8", "-unchecked", "-deprecation", "-feature", "-language:existentials"),
    mainClass in Compile := None,

    sourceDirectories in Compile := Seq(baseDirectory.value / "src"),
    scalaSource in Compile := baseDirectory.value / "src",
    javaSource in Compile := baseDirectory.value / "src",
    resourceDirectory in Compile := baseDirectory.value / "resources",

    scalaSource in Test := baseDirectory.value / "test",
    javaSource in Test := baseDirectory.value / "test",
    resourceDirectory in Test := baseDirectory.value / "test/resources",

    // Dependencies
    libraryDependencies += "com.google.code.findbugs" % "jsr305" % "3.0.1", // @Nonnull, @Nullable annotation support
    libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.4",
    libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.21",

    // Optional dependencies
    libraryDependencies += "org.postgresql" % "postgresql" % "9.3-1101-jdbc4" % "optional",
    libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.4.0" % "optional",

    // Test dependencies
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.0-M15" % "test",
    libraryDependencies += "org.scalamock" %% "scalamock-scalatest-support" % "3.2.2" % "test",

    // Deploy settings
    startYear := Some(2015),
    homepage := Some(url("https://github.com/citrum/querio")),
    licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html")),
    bintrayVcsUrl := Some("https://github.com/citrum/querio"),
    bintrayOrganization := Some("citrum"),
    // No Javadoc
    publishArtifact in(Compile, packageDoc) := false,
    publishArtifact in packageDoc := false,
    sources in(Compile, doc) := Seq.empty
  )

  lazy val querioSelfCodegen = Project("querio-selfcodegen", base = file("selfcodegen"), settings = commonSettings).settings(
    name := "querio-selfcodegen",
    // Disable packaging & publishing artifact
    Keys.`package` := file(""),
    publishArtifact := false,
    publishLocal := {},
    publish := {},
    bintrayUnpublish := {}
  )

  lazy val main: Project = Project("querio", base = file("."), settings = commonSettings).settings(
    name := "querio",
    description := "Scala ORM, DSL, and code generator for database queries",
    genQuerioLibSourcesTask,
    // Наводим красоту в командной строке sbt
    shellPrompt := {state: State => "[" + scala.Console.GREEN + "querio" + scala.Console.RESET + "] "}
  )

  // ------------------------------- Test projects -------------------------------

  val testH2Settings = Seq(
    name := "querio-test-h2",
    version := "0.1",
    scalaVersion := "2.11.7",
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

  lazy val test_h2 = Project(id = "test-h2",
    base = file("test-h2"),
    settings = testH2Settings).settings(
    name := "test-h2"
    ).dependsOn(main)

  lazy val test_postgresql= Project(id = "test-postgresql",
    base = file("test-postgresql"),
    settings = testPostgreSQLSettings).settings(
    name := "test-postgresql"
    ).dependsOn(main)

  ///////////////////////  Tasks ///////////////////////////

  // Task: Generate some querio lib sources
  val genQuerioLibSources = taskKey[Unit]("gen-querio-lib-sources")
  lazy val genQuerioLibSourcesTask = genQuerioLibSources := {
    (compile in Compile in querioSelfCodegen).value // Run codegen compile task
    val classPath: Seq[File] =
      (dependencyClasspath in Compile in querioSelfCodegen).value.files :+
      (classDirectory in Runtime in querioSelfCodegen).value
    runScala(classPath, "querio.selfcodegen.SelfClassesGenerator",
      Seq((scalaSource in Compile).value.absolutePath))
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
            runScala(classPath.files :+ baseDir :+ classesDir, "orm.querio.codegen.TodoGenerator", Seq(scalaSource.absolutePath))
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
