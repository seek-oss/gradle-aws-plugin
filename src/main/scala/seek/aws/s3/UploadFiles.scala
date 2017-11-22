package seek.aws.s3

import java.io.File

import cats.effect.IO
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import org.gradle.api.GradleException
import org.gradle.api.file.{FileTree, FileTreeElement}
import seek.aws.AwsTask

import scala.collection.mutable

class UploadFiles extends AwsTask {

  setDescription("Uploads multiple files to S3")

  private val bucket = lazyProp[String]("bucket")
  def bucket(v: Any): Unit = bucket.set(v)

  private val files = lazyProp[FileTree]("files")
  def files(v: Any): Unit = files.set(v)

  private val prefix = lazyProp[String]("prefix", "")
  def prefix(v: Any): Unit = prefix.set(v)

  private val failIfPrefixExists = lazyProp[Boolean]("failIfPrefixExists", false)
  def failIfPrefixExists(v: Any): Unit = failIfPrefixExists.set(v)

  private val failIfObjectExists = lazyProp[Boolean]("failIfObjectExists", false)
  def failIfObjectExists(v: Any): Unit = failIfObjectExists.set(v)

  private val cleanPrefixBeforeUpload = lazyProp[Boolean]("cleanPrefixBeforeUpload", false)
  def cleanPrefixBeforeUpload(v: Any): Unit = cleanPrefixBeforeUpload.set(v)

  private val client = AmazonS3ClientBuilder.standard().withRegion(region).build()

  override def run: IO[Unit] = {
    val prefix = if (this.prefix.get.nonEmpty) s"${this.prefix.get.stripSuffix("/")}/" else ""
    val keyFileMap = fileElements(files.get).foldLeft(Map.empty[String, File]) { (z, e) =>
      z + (s"${prefix}${e.getRelativePath}" -> e.getFile)
    }

    for {
      _ <- checkFailIfPrefixExists
      _ <- checkFailIfObjectExists(keyFileMap.keys.toList)
      _ <- checkCleanPrefixBeforeUpload
      _ <- uploadAll(keyFileMap)
    } yield ()
  }

  private def checkFailIfPrefixExists: IO[Unit] =
    if (failIfPrefixExists.get)
      exists(bucket.get, prefix.get).run(client).flatMap {
        case true  => IO.raiseError(new GradleException(s"Prefix '${prefix.get}' already exists in bucket '${bucket.get}'"))
        case false => IO.unit
      }
    else IO.unit

  private def checkFailIfObjectExists(keys: List[String]): IO[Unit] =
    if (failIfObjectExists.get)
      existsAny(bucket.get, keys).run(client).flatMap {
        case true  => IO.raiseError(new GradleException(s"One or more objects already exist in bucket '${bucket.get}'"))
        case false => IO.unit
      }
    else IO.unit

  private def checkCleanPrefixBeforeUpload: IO[Unit] =
    if (cleanPrefixBeforeUpload.get) cleanPrefix else IO.unit

  private def cleanPrefix: IO[Unit] =
    if (prefix.get.isEmpty)
      IO.raiseError(new GradleException("No prefix specified to clean (and refusing to delete entire bucket)"))
    else
      deleteAll(bucket.get, prefix.get).run(client)

  private def uploadAll(keyFileMap: Map[String, File]): IO[Unit] =
    keyFileMap.foldLeft(IO.unit) {
      case (z, (k, f)) => z.flatMap(_ => upload(bucket.get, k, f).run(client))
    }

  private def fileElements(ft: FileTree): List[FileTreeElement] = {
    val buf = new mutable.ArrayBuffer[FileTreeElement]()
    ft.visit(d => buf += d)
    buf.filter(e => e.getFile.isFile && e.getFile.exists).toList
  }
}
