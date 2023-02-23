import sbt._
import Keys._

import scoverage._
import scala.sys.process._

object SciFeBuild {
  
  val preferredJVM = Some("jvm-1.8")
  
  lazy val root =
    Project("SciFe", file("."))
      .configs( BenchConfig )
      .settings( inConfig(BenchConfig)(Defaults.testTasks): _*)
      .settings(
        // add commands
        commands ++= Seq(benchCommand, benchBadgeCommand),
        // fork by default,
//        fork := false,

        // test options 
        Test / fork := true,
        Test / javaOptions ++= Seq("-Xms2048m", "-Xmx2048m",
          "-XX:MaxPermSize=512m", "-XX:+UseConcMarkSweepGC"),
        // verbose QuickCheck error ouput
        Test / testOptions += Tests.Argument(TestFrameworks.ScalaCheck, "-verbosity", "3"),
        // exclude slow tests
        Test / testOptions += noSlowTests,
        
        // benchmark options
        //unmanagedSourceDirectories in BenchConfig <+= sourceDirectory ( _ / "bench" ),
        Test / unmanagedSourceDirectories += sourceDirectory ( _ / "bench" ).value,
        // run only benchmark not dependent tests
        BenchConfig / compile / sourceDirectories += sourceDirectory ( _ / "bench" ).value,
        //sources in (BenchConfig, test) := Seq ( sourceDirectory.value / "bench" ),
        BenchConfig / fork := false,        
        BenchConfig / includeFilter := AllPassFilter,
        BenchConfig / testOptions := Seq( benchmarksFilter/*, noSuiteFilter */ ),
//        testOptions in BenchConfig += Tests.Filter({ (s: String) =>
//          val isFull = s endsWith "Full"
//          !isFull
//        }),
        BenchConfig / scalacOptions ++= generalScalacFlagList,
        BenchConfig / scalacOptions ++= optimizedCompileScalacFlagsList,
        
        // ScalaMeter
        BenchConfig / parallelExecution := false,
        BenchConfig / testFrameworks += new TestFramework("org.scalameter.ScalaMeterFramework")
        
        // Scoverage
        , scoverage.ScoverageKeys.coverageExcludedPackages :=
          "<empty>;scife\\.util\\.*;scife\\.enumeration\\.util.*"+
          ";scife\\.util\\.format\\.*;scife\\.util\\.logging\\.*"
      )

  val benchRegEx = //"""(.*\.suite\.[^\.]*Suite*)"""
    """(.*\.benchmarks\..*)"""
  val benchmarksFilter = Tests.Filter(
    _ matches """(.*\.benchmarks\..*)"""
  )
  val noSuiteFilter = Tests.Filter(
    (s: String) => !(s matches """.*Suite.*""")
  )
  val noSlowTests = Tests.Argument(TestFrameworks.ScalaTest, "-l", "tags.Slow")
  
  lazy val BenchConfig = config("benchmark") extend(Test)
    
  def benchCommand = Command.single("bench") { (state, arg) =>
    
    val extracted: Extracted = Project.extract(state)
    import extracted._
    
    arg match {
      case "full" =>
        val fullState =
          append(Seq(
            BenchConfig / testOptions := (BenchConfig / testOptions).value diff Seq(noSuiteFilter),
            BenchConfig / testOptions += Tests.Filter(_ endsWith "Full")), state)
        Project.runTask(BenchConfig / test, fullState)
        // return the same state (not the modified one with filters)
        state
      case "minimal" | "simple" =>
        val minState =          
          append(Seq(
            BenchConfig / testOptions := (BenchConfig / testOptions).value diff Seq(noSuiteFilter),
            BenchConfig / testOptions += Tests.Filter(_ endsWith "Minimal")
          ), state)
        Project.runTask(BenchConfig / test, minState)
        state
      case "measure" =>
        val measureState =          
          append(Seq(
            BenchConfig / testOptions := (BenchConfig / testOptions).value diff Seq(noSuiteFilter),
            BenchConfig / testOptions += Tests.Filter(_ endsWith "Measure")
          ), state)
        Project.runTask(BenchConfig / test, measureState)
        state
      case "profile" =>
        val profileState =          
          append(Seq(
            BenchConfig / testOptions := (Test / testOptions).value diff Seq(noSlowTests),
            BenchConfig / testOptions += Tests.Filter(_ contains "Profile"),
            // for one JVM
            BenchConfig / test / fork := true,
            BenchConfig / test / javaOptions := profileJVMFlagList ++ remoteConnectionJVMFlagList
          ), state)
        Project.runTask(BenchConfig / test, profileState)
        state
      case "slow" =>
        val slowState =          
          append(Seq(BenchConfig / testOptions += Tests.Filter(_ endsWith "Slow")), state)
        Project.runTask(BenchConfig / test, slowState)
        slowState
      case "debug" =>
        Project.runTask(BenchConfig / test, state)
        state
      case _ =>
        state.fail
    }
  }
  
  val profileJVMFlagList = List(
    // print important outputs
//    "-XX:+PrintCompilation",
    // verbose GC
    "-verbose:gc", "-XX:+PrintGCTimeStamps", "-XX:+PrintGCDetails",
    // compilation
    "-Xbatch",
//    "--XX:CICompilerCount=1",
    // optimizations
    "-XX:ReservedCodeCacheSize=512M",
    "-XX:CompileThreshold=100", "-XX:+TieredCompilation",
    "-XX:+AggressiveOpts", "-XX:MaxInlineSize=512"
    ,
    // memory
    "-Xms32G", "-Xmx32G"
    // new generation size
//    ,"-XX:NewSize=20G",
//    // disable adaptive policy
//    "-XX:-UseAdaptiveSizePolicy",
//    "-XX:MinHeapFreeRatio=100",
//    "-XX:MaxHeapFreeRatio=100"
  )
  
  val remoteConnectionJVMFlagList = List(
    "-Dcom.sun.management.jmxremote",
    "-Dcom.sun.management.jmxremote.port=4567",
    "-Dcom.sun.management.jmxremote.ssl=false",
    "-Dcom.sun.management.jmxremote.authenticate=false",
    "-Djava.rmi.server.hostname=128.52.186.35"
  )
    
  val generalScalacFlagList = List(
    "-deprecation", "-unchecked", "-feature",
    // no debugging info
    "-g", "none"
  ) ::: preferredJVM.toList.flatMap( "-target " :: _ :: Nil )
  
  val optimizedCompileScalacFlagsList = List(
    "-Xdisable-assertions",
    // elide logging facilities
    "-Xelide-below", "OFF",
    // group of flags for optimization
    "-optimise",
    "-Yinline"
  )
  
  import java.util._
  import java.text._
  
  val badgesUrl = "http://img.shields.io/badge/"
  val pattern = "benchmark-%s-green.svg"
  val downloadCommand = "wget -O ./tmp/status.svg %s%s"
  val suffixPattern = "benchmark-%s-green.svg"
    
  def benchBadgeCommand = Command.command("bench-badge") { state =>
    val currentTime = Calendar.getInstance().getTime()
    val dateFormat = new SimpleDateFormat("""dd%2'F'MM%2'F'yy""")
    
    val dateString = dateFormat format currentTime
    val suffix = suffixPattern format dateString
    
    val commandResult =
      Process(downloadCommand.format(badgesUrl, suffix)).lines
    
    println(commandResult)
    
    state
  }
  
}
