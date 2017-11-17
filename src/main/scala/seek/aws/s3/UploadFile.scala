package seek.aws.s3

import java.io.File

import cats.effect.IO
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import seek.aws.AwsTask

class UploadFile extends AwsTask {

  setDescription("Uploads a single file to S3")

  private val bucket = lazyProp[String]("bucket")
  def bucket(v: Any): Unit = bucket.set(v)

  private val file = lazyProp[File]("file")
  def file(v: Any): Unit = file.set(v)

  private val key = lazyProp[String]("key", "")
  def prefix(v: Any): Unit = key.set(v)

  private val failIfObjectExists = lazyProp[Boolean]("failIfObjectExists", false)
  def checkExistsAndFailFast(v: Any): Unit = failIfObjectExists.set(v)

  private val client = AmazonS3ClientBuilder.standard().withRegion(region).build()

  override def run: IO[Unit] =
    (if (failIfObjectExists.get)
      ensureNoneExist(bucket.get, List(key.get)).run(client)
    else IO.unit).flatMap(_ => upload(bucket.get, key.get, file.get).run(client))
}
