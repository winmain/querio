// TODO: нет компиляции перед выполнением этого таска
// Task: Generate database classes
val genDbSources = TaskKey[Unit]("gen-db-sources")
lazy val genDbSourcesTask = genDbSources <<=
  (scalaSource in Compile, dependencyClasspath in Compile, baseDirectory in Compile, classDirectory in Runtime) map {
    (scalaSource, classPath, baseDir, classesDir) => {
      runScala(classPath.files :+ baseDir :+ classesDir, "example.ExampleGenerator", Seq(scalaSource.absolutePath))
    }
  }

val querio = "com.github.winmain" %% "querio" % "0.4.3-SNAPSHOT" // querio orm

lazy val QuerioProject: RootProject = RootProject(file("../"))
/**
 * Run scala class in separate thread
 */
def runScala(classPath: Seq[File], className: String, arguments: Seq[String]) {
  val ret: Int = new Fork("java", Some(className)).apply(ForkOptions(bootJars = classPath), arguments)
  if (ret != 0) sys.error("Trouble with code generator")
}

lazy val querio_test_postgresql = (project in file(".")).settings(
  name := "querio-test-postgresql",
  version := "0.1",
  scalaVersion := "2.11.7",
//  libraryDependencies += "com.github.winmain" %% "querio" % "0.4.3-SNAPSHOT",
  libraryDependencies += querio,
  libraryDependencies += "org.postgresql" % "postgresql" % "9.3-1101-jdbc4",
  libraryDependencies += "org.json4s" % "json4s-jackson_2.10" % "3.3.0",
  libraryDependencies += "org.specs2" % "specs2_2.11" % "3.7",

    //  libraryDependencies += "ru.yandex.qatools.embed" % "postgresql-embedded" % "1.12",
  libraryDependencies += "com.opentable.components" % "otj-pg-embedded" % "0.5.0",


  sourceDirectories in Compile := Seq(baseDirectory.value / "src"),
  scalaSource in Compile := baseDirectory.value / "src",
  genDbSourcesTask
).dependsOn(QuerioProject)
