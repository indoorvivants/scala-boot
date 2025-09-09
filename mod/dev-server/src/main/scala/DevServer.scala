import java.io.File

@main def hello =
  val unitdCommand = sys.env.getOrElse(
    "SCALABOOT_UNITD_COMMAND",
    sys.error("SCALABOOT_UNITD_COMMAND is missing from env")
  )
  val cwd = sys.env.getOrElse(
    "SCALABOOT_SERVER_CWD",
    sys.error("SCALABOOT_SERVER_CWD is missing from env")
  )

  import sys.process.*

  val p = ProcessLogger(System.out.println(_), System.err.println(_))

  val bgProc = Process(unitdCommand, cwd = new File(cwd)).run(p)

  sys.addShutdownHook {
    println("Caught shutdown, killing process")
    bgProc.destroy
  }
  val ev = bgProc.exitValue
  println(s"Process finished naturally with code $ev")
end hello
