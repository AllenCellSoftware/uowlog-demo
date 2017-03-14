organization := "org.uowlog"
name := "uowlog-demo"

version := "1.1-SNAPSHOT"
scalaVersion := "2.12.0"

// logLevel in Global := Level.Debug

libraryDependencies += "org.scalactic"  %% "scalactic"       % "3.0.0"
libraryDependencies += "org.scalatest"  %% "scalatest"       % "3.0.0" % "test"
libraryDependencies += "org.slf4j"      %  "slf4j-api"       % "1.7.21"
libraryDependencies += "ch.qos.logback" %  "logback-classic" % "1.1.7"
libraryDependencies += "io.spray"       %% "spray-json"      % "1.3.2"

libraryDependencies += "org.aspectj" % "aspectjweaver" % "1.8.9"
libraryDependencies += "org.aspectj" % "aspectjtools"  % "1.8.9"

libraryDependencies += "com.typesafe.akka" %% "akka-actor"   % "2.4.14"
libraryDependencies += "com.typesafe.akka" %% "akka-remote"  % "2.4.14"
libraryDependencies += "com.typesafe.akka" %% "akka-slf4j"   % "2.4.14"
libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.4.14"

libraryDependencies += "org.uowlog" %% "uowlog"         % "[1.0,1.1["
libraryDependencies += "org.uowlog" %% "uowlog-http"    % "[1.0,1.1["
libraryDependencies += "org.uowlog" %% "uowlog-testkit" % "[1.0,1.1[" % "test"

libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.6"

fork := true
fork in (Test,run) := true
aspectjSettings
AspectjKeys.compileOnly in Aspectj := true
publishArtifact in Test := true

javaOptions in run <++= AspectjKeys.weaverOptions in Aspectj
javaOptions in Test <++= AspectjKeys.weaverOptions in Aspectj
products in Compile <++= products in Aspectj
products in run <<= products in Compile

resolvers += Resolver.jcenterRepo
