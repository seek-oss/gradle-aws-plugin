package seek.aws

import java.io.File

import cats.data.Kleisli
import cats.effect.IO
import com.amazonaws.AmazonServiceException
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.{DeleteObjectsRequest, ListObjectsV2Request, S3ObjectSummary}
import org.gradle.api.GradleException
import fs2._

import scala.collection.JavaConverters._

package object s3 {

  class FileAlreadyExistsException(bucket: String, key: String)
    extends GradleException(s"File '${key}' already exists in S3 bucket '${bucket}'")

  val S3PageSize = 1000

  def ensureNoneExist(bucket: String, keys: List[String]): Kleisli[IO, AmazonS3, Unit] =
    Kleisli { c =>
      keys match {
        case h :: t =>
          IO(c.getObjectMetadata(bucket, h)).attempt.flatMap {
            case Left(ex: AmazonServiceException) if ex.getStatusCode == 404 => ensureNoneExist(bucket, t).run(c)
            case Left(ex) => IO.raiseError(ex)
            case Right(_) => IO.raiseError(new FileAlreadyExistsException(bucket, h))
          }
        case Nil => IO.unit
      }
    }

  def upload(bucket: String, key: String, file: File): Kleisli[IO, AmazonS3, Unit] =
    Kleisli { client => IO(client.putObject(bucket, key, file)) }

  def deleteAll(bucket: String, prefix: String): Kleisli[IO, AmazonS3, Unit] =
    Kleisli { c =>
      val keys = listObjects(bucket, prefix)(c).map(_.getKey)
      keys.sliding(S3PageSize).evalMap { q =>
        val req = new DeleteObjectsRequest(bucket)
          .withQuiet(true)
          .withKeys(q: _*)
        IO(c.deleteObjects(req))
      }.run
    }

  def listObjects(bucket: String, prefix: String)(client: AmazonS3): Stream[IO, S3ObjectSummary] = {
    val pages = Stream.unfoldEval[IO, Option[String], Seq[S3ObjectSummary]](None) { startAfter =>
      val req = new ListObjectsV2Request()
        .withBucketName(bucket)
        .withPrefix(prefix)
        .withStartAfter(startAfter.orNull)
      IO(client.listObjectsV2(req)).map { res =>
        val os = res.getObjectSummaries.asScala
        os.lastOption.map(last => (os, Some(last.getKey)))
      }
    }
    pages.flatMap(Stream.emits(_))
  }
}
