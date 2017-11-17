package seek.aws

import java.io.File

import cats.data.Kleisli
import cats.effect.IO
import com.amazonaws.AmazonServiceException
import com.amazonaws.services.s3.AmazonS3
import org.gradle.api.GradleException

package object s3 {

  class FileAlreadyExistsException(bucket: String, key: String)
    extends GradleException(s"File '${key}' already exists in S3 bucket '${bucket}'")

  def ensureNoneExist(bucket: String, keys: List[String]): Kleisli[IO, AmazonS3, Unit] =
    Kleisli { client =>
      keys match {
        case h :: t =>
          IO(client.getObjectMetadata(bucket, h)).attempt.flatMap {
            case Left(ex: AmazonServiceException) if ex.getStatusCode == 404 => ensureNoneExist(bucket, t).run(client)
            case Left(ex) => IO.raiseError(ex)
            case Right(_) => IO.raiseError(new FileAlreadyExistsException(bucket, h))
          }
        case Nil => IO.unit
      }
    }

  def upload(bucket: String, key: String, file: File): Kleisli[IO, AmazonS3, Unit] =
    Kleisli { client => IO(client.putObject(bucket, key, file)) }

}
