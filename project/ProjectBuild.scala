import sbt.Keys._
import sbt._

object ProjectBuild extends sbt.Build {
  val buildScalaVersion = "2.11.7"
  val module = "querio"

  val commonSettings = Seq(
    organization := "com.github.winmain",
    version := "0.3-SNAPSHOT",
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
    libraryDependencies += "com.github.scala-incubator.io" %% "scala-io-file" % "0.4.3-1"
  )

  val codegen = Project("codegen", base = file("codegen"), settings = commonSettings).settings(
    name := "querio-codegen"
  )


  // Task: Generate some querio lib sources
  val genQuerioLibSources = TaskKey[Unit]("gen-querio-lib-sources")
  lazy val genQuerioLibSourcesTask = genQuerioLibSources <<=
    (scalaSource in Compile, dependencyClasspath in Compile, baseDirectory in Compile, classDirectory in Runtime) map {
      (scalaSource, classPath, baseDir, classesDir) => {
        runScala(classPath.files :+ baseDir :+ classesDir, "querio.codegen.SelfClassesGenerator", Seq(scalaSource.absolutePath))
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


  lazy val main: Project = Project("querio", base = file("."), settings = commonSettings).settings(
    name := "querio",
    genQuerioLibSourcesTask
  ).dependsOn(codegen).aggregate(codegen)
}
