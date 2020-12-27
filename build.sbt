name := "Music Gradebook"

version := "5.3.1"

organization := "com.dbschools"

scalaVersion := "2.10.6"

resolvers ++= Seq("snapshots"     at "https://oss.sonatype.org/content/repositories/snapshots",
                  "staging"       at "https://oss.sonatype.org/content/repositories/staging",
                  "releases"      at "https://oss.sonatype.org/content/repositories/releases",
                  "mvnrepository" at "https://mvnrepository.com/artifact/com.typesafe.akka/akka-actor"
                 )

seq(webSettings :_*)

unmanagedResourceDirectories in Test <+= (baseDirectory) { _ / "src/main/webapp" }

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-Ylog-classpath")

libraryDependencies ++= {
  val liftVersion = "2.6.2"
  Seq(
    "javax.servlet"     %  "servlet-api"        % "2.5" % "compile",
    "javax.transaction" % "javax.transaction-api" % "1.3",
    "javax.transaction" % "jta"                 % "1.1",
    "com.typesafe.akka" %% "akka-actor"         % "2.3.2" withSources(),
    "org.scalaz"        %  "scalaz-core_2.10"   % "7.0.3" withSources(),
    "net.liftweb"       %% "lift-webkit"        % liftVersion        % "compile",
    "net.liftweb"       %% "lift-mapper"        % liftVersion        % "compile",
    "net.liftweb"       %% "lift-record"        % liftVersion        % "compile",
    "net.liftweb"       %% "lift-squeryl-record" % liftVersion exclude("org.squeryl","squeryl"),
    "net.liftmodules"   %% "lift-jquery-module_2.6" % "2.8",
    "net.liftmodules"   %% "fobo_2.6"           % "1.5"                 % "compile",
    "org.eclipse.jetty" % "jetty-webapp"        % "8.1.17.v20150415"  % "container,test",
    "org.eclipse.jetty" % "jetty-plus"          % "8.1.17.v20150415"  % "container,test", // For Jetty Config
    "org.eclipse.jetty.orbit" % "javax.servlet" % "3.0.0.v201112011016" % "container,test" artifacts Artifact("javax.servlet", "jar", "jar"),
    "log4j"             %  "log4j"              % "1.2.17",
    "ch.qos.logback"    %  "logback-classic"    % "1.0.13",
    "org.specs2"        %  "specs2_2.10"        % "2.2" % "test",
    "com.h2database"    %  "h2"                 % "1.3.173",
    "org.squeryl"       %% "squeryl"            % "0.9.5-6",
    "org.postgresql"    %  "postgresql"         % "42.2.18.jre7",
    "org.scala-tools.time" % "time_2.9.1"       % "0.5",
    "com.jolbox"        %  "bonecp"             % "0.7.1.RELEASE", // connection pooling
    "com.github.scala-incubator.io" %% "scala-io-core" % "0.4.2",
    "com.github.scala-incubator.io" %% "scala-io-file" % "0.4.2",
    "com.norbitltd"     %  "spoiwo"             % "1.0.6",
    "org.scalatest"     %% "scalatest"          % "3.0.0"
  )
}

fullRunTask(TaskKey[Unit]("load-sample-data", "Loads sample data"), Compile, "com.dbschools.mgb.TestDataMaker")
