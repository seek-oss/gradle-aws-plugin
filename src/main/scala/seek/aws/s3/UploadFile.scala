package seek.aws.s3

import java.io.File

import cats.effect.IO
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import org.gradle.api.GradleException
import seek.aws.AwsTask

class UploadFile extends AwsTask {

  setDescription("Uploads a single file to S3")

  private val bucket = lazyProp[String]("bucket")
  def bucket(v: Any): Unit = bucket.set(v)

  private val file = lazyProp[File]("file")
  def file(v: Any): Unit = file.set(v)

  private val key = lazyProp[String]("key", "")
  def key(v: Any): Unit = key.set(v)

  private val failIfObjectExists = lazyProp[Boolean]("failIfObjectExists", false)
  def failIfObjectExists(v: Any): Unit = failIfObjectExists.set(v)

  private val client = AmazonS3ClientBuilder.standard().withRegion(region).build()

  override def run: IO[Unit] =
    for {
      _ <- checkFailIfObjectExists
      _ <- upload(bucket.get, key.get, file.get).run(client)
    } yield ()

  private def checkFailIfObjectExists: IO[Unit] =
    if (failIfObjectExists.get)
      exists(bucket.get, key.get).run(client).flatMap {
        case true  => IO.raiseError(new GradleException(s"Object '${key.get}' already exists in bucket '${bucket.get}'"))
        case false => IO.unit
      }
    else IO.unit
}
