package seek.aws
package cloudformation

import cats.effect.IO

class VerifyStack extends AwsTask {

  setDescription("Verifies that all required stack properties have been provided and prints their details")

  override def run: IO[Unit] =
    for {
      sp <- StackProperties(project)
      _  <- printStack(sp)
    } yield ()

  private def printStack(sp: StackProperties): IO[Unit] =
    for {
      _ <- IO(logger.lifecycle(s"Name: ${sp.name}"))
      _ <- IO(logger.lifecycle("Parameters:"))
      _ <- printSortedMap(sp.parameters)
      _ <- IO(logger.lifecycle("Tags:"))
      _ <- printSortedMap(sp.tags)
    } yield ()

  private def printSortedMap(m: Map[String, String]): IO[Unit] =
    IO(m.keys.toList.sortWith(_ < _).foreach { k => logger.lifecycle(s"  ${k}: ${m(k)}") })
}

