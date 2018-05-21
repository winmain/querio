import sbt.Keys.{libraryDependencies, scalaSource, _}

// ------------------------------- Main projects -------------------------------

val DefaultScalaVersion = "2.12.6"

crossScalaVersions := Seq("2.11.12", "2.12.6")
scalaVersion := DefaultScalaVersion

val scalaSettings = Seq(
  scalaVersion := DefaultScalaVersion,
  scalacOptions ++= Seq(/*"-target:jvm-1.8", */"-unchecked", "-deprecation", "-feature", "-language:existentials")
)

val defaultProjectStructure = Seq(
  sourceDirectories in Compile := Seq(baseDirectory.value / "src"),
  scalaSource in Compile := baseDirectory.value / "src",
  javaSource in Compile := baseDirectory.value / "src",
  resourceDirectory in Compile := baseDirectory.value / "resources",

  scalaSource in Test := baseDirectory.value / "testSrc",
  javaSource in Test := baseDirectory.value / "testSrc",
  resourceDirectory in Test := baseDirectory.value / "testData"
)

val commonSettings = _root_.bintray.BintrayPlugin.bintrayPublishSettings ++ scalaSettings ++ defaultProjectStructure ++ Seq(
  organization := "com.github.citrum.querio",
  version := "0.7.0",

  incOptions := incOptions.value.withNameHashing(nameHashing = true),
  sources in doc in Compile := List(), // Выключить генерацию JavaDoc, ScalaDoc
  mainClass in Compile := None,

  // Dependencies
  libraryDependencies += "com.google.code.findbugs" % "jsr305" % "3.0.1", // @Nonnull, @Nullable annotation support
  libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.6",
  libraryDependencies += "org.apache.commons" % "commons-text" % "1.1",
  libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.25",

  // Optional dependencies
  libraryDependencies += "org.postgresql" % "postgresql" % "42.2.2" % "optional",
  libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.5.0" % "optional",
  libraryDependencies += "com.beachape" %% "enumeratum" % "1.5.13" % "optional",

  // Test dependencies
  libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  libraryDependencies += "org.scalamock" %% "scalamock-scalatest-support" % "3.5.0" % "test",
  libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.1" % "test",

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

// ------------------------------- Main project -------------------------------

lazy val main: Project = project.in(file(".")).settings(
  publishArtifact := false,
  genQuerioLibSourcesTask,
  testAllTask,
  // Наводим красоту в командной строке sbt
  shellPrompt := {state: State => "[" + scala.Console.GREEN + "querio" + scala.Console.RESET + "] "}
).dependsOn(querio).aggregate(querio)

// ------------------------------- Querio project -------------------------------

lazy val querio = Project("querio",
  base = file("querio"),
  settings = commonSettings
).settings(
  name := "querio",
  description := "Scala ORM, DSL, and code generator for database queries"
)

// ------------------------------- Codegen project -------------------------------

lazy val querioSelfCodegen = Project("querio-selfcodegen",
  base = file("selfcodegen"),
  settings = commonSettings
).settings(
  name := "querio-selfcodegen",
  // Disable packaging & publishing artifact
  Keys.`package` := file(""),
  publishArtifact := false,
  publishLocal := {},
  publish := {},
  bintrayUnpublish := {}
)

// ------------------------------- Test projects -------------------------------

lazy val testH2 = Project(id = "test-h2",
  base = file("test-h2"),
  settings = scalaSettings ++ defaultProjectStructure
).settings(
  name := "test-h2",

  //  libraryDependencies += "com.h2database" % "h2" % "1.4.191",
  libraryDependencies += "com.h2database" % "h2" % "1.3.175",
  libraryDependencies += "org.json4s" % "json4s-jackson_2.10" % "3.3.0",
  libraryDependencies += "org.specs2" %% "specs2-core" % "3.8.8",

  parallelExecution in Test := false,
  genTestH2DbSourcesTask
).dependsOn(querio)

lazy val testPostgresql = Project(id = "test-postgresql",
  base = file("test-postgresql"),
  settings = scalaSettings ++ defaultProjectStructure
).settings(
  name := "test-postgresql",

  libraryDependencies += "org.postgresql" % "postgresql" % "42.2.2",
  libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.5.4",
  libraryDependencies += "com.opentable.components" % "otj-pg-embedded" % "0.12.0",
  libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.1",
  libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test",

  genTestPostgreSqlDbSourcesTask
).dependsOn(querio)


///////////////////////  Tasks ///////////////////////////

/**
  * Запустить scala класс кодогенерации в отдельном процессе
  */
def runScala(classPath: Seq[File], className: String, arguments: Seq[String]) {
  val ret: Int = new Fork("java", Some(className)).apply(ForkOptions(bootJars = classPath), arguments)
  if (ret != 0) sys.error("Trouble with code generator")
}

// Task: Generate some querio lib sources
val genQuerioLibSources = taskKey[Unit]("gen-querio-lib-sources")
lazy val genQuerioLibSourcesTask = genQuerioLibSources := {
  (compile in Compile in querioSelfCodegen).value // Run codegen compile task
  val classPath: Seq[File] =
    (dependencyClasspath in Compile in querioSelfCodegen).value.files :+
      (classDirectory in Runtime in querioSelfCodegen).value
  runScala(classPath, "querio.selfcodegen.SelfClassesGenerator",
    Seq((scalaSource in Compile in querio).value.absolutePath))
}

val genTestH2DbSources = TaskKey[Unit]("gen-test-h2-db-sources")
lazy val genTestH2DbSourcesTask = genTestH2DbSources := {
  runScala((dependencyClasspath in Compile).value.files :+
    (baseDirectory in Compile).value :+
    (classDirectory in Runtime).value,
    "test.SourcesGenerator",
    Seq((scalaSource in Compile).value.absolutePath))
}

val genTestPostgreSqlDbSources = TaskKey[Unit]("gen-test-postgresql-db-sources")
lazy val genTestPostgreSqlDbSourcesTask = genTestPostgreSqlDbSources := {
  runScala((dependencyClasspath in Compile).value.files :+
    (baseDirectory in Compile).value :+
    (classDirectory in Runtime).value,
    "test.SourcesGenerator",
    Seq((scalaSource in Compile).value.absolutePath))
}

// Run all tests task

val testAll = TaskKey[Unit]("test-all")
lazy val testAllTask = testAll := {
  val a = (test in Test in querio).value
  val b = (test in Test in testPostgresql).value
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
