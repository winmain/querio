import sbt.Keys._
// Task: Generate database classes
val genDbSources = TaskKey[Unit]("gen-db-sources")
lazy val genDbSourcesTask = genDbSources <<=
  (compile in Compile in codegen,
    scalaSource in Compile,
    dependencyClasspath in Compile in codegen,
    baseDirectory in Compile in codegen,
    classDirectory in Runtime in codegen) map {
    (_, scalaSource, classPath, baseDir, classesDir) => {
      runScala(classPath.files :+ baseDir :+ classesDir, "example.ExampleGenerator", Seq(scalaSource.absolutePath))
    }
  }

val querioVersion = "0.4.99-SNAPSHOT"
val querio = "com.github.winmain" %% "querio" % querioVersion
val querioCodegen = "com.github.winmain" %% "querio-codegen" % querioVersion
val mysql = "mysql" % "mysql-connector-java" % "5.1.36"

/**
 * Run scala class in separate thread
 */
def runScala(classPath: Seq[File], className: String, arguments: Seq[String]) {
  val ret: Int = new Fork("java", Some(className)).apply(ForkOptions(bootJars = classPath), arguments)
  if (ret != 0) sys.error("Trouble with code generator")
}


lazy val codegen: Project = Project("codegen", base = file("modules/codegen"),
  settings = Seq(
    scalaVersion := "2.11.7",
    sourceDirectories in Compile := Seq(baseDirectory.value / "src"),
    scalaSource in Compile := baseDirectory.value / "src",
    libraryDependencies += querioCodegen,
    libraryDependencies += mysql
  ))


lazy val querio_example_mysql = (project in file(".")).settings(
  name := "querio-example-mysql",
  version := "0.1",
  scalaVersion := "2.11.7",
  libraryDependencies += querio,
  libraryDependencies += mysql,

  sourceDirectories in Compile := Seq(baseDirectory.value / "src"),
  scalaSource in Compile := baseDirectory.value / "src",
  genDbSourcesTask
)
