lazy val example = (project in file(".")).settings(
  name := "example",
  version := "0.1",
  scalaVersion := "2.11.6",
  libraryDependencies += "com.github.winmain" %% "querio" % "0.1-SNAPSHOT",

  sourceDirectories in Compile := Seq(baseDirectory.value / "src"),
  scalaSource in Compile := baseDirectory.value / "src"
)
