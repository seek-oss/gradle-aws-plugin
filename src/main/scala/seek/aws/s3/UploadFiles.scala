package seek.aws.s3

import java.io.File

import cats.effect.IO
import com.amazonaws.services.s3.AmazonS3ClientBuilder
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

  // TODO: Do we want to check if all exist? This could mean we write files amoungst existing files.
  // We probably want multiple properties:
  // failIfPrefixExists
  // failIfObjectExists
  // cleanPrefixBeforeUpload
  private val checkExistsAndFailFast = lazyProp[Boolean]("checkExistsAndFailFast", false)
  def checkExistsAndFailFast(v: Any): Unit = checkExistsAndFailFast.set(v)

  private val client = AmazonS3ClientBuilder.standard().withRegion(region).build()

  override def run: IO[Unit] = {
    val prefix = if (this.prefix.get.nonEmpty) s"${this.prefix.get.stripSuffix("/")}/" else ""
    val keyFileMap = fileElements(files.get).foldLeft(Map.empty[String, File]) { (z, e) =>
      z + (s"${prefix}${e.getRelativePath}" -> e.getFile)
    }
    (if (checkExistsAndFailFast.get)
      ensureNoneExist(bucket.get, keyFileMap.keys.toList).run(client)
    else IO.unit).flatMap(_ => uploadAll(keyFileMap))
  }

  private def fileElements(ft: FileTree): List[FileTreeElement] = {
    val buf = new mutable.ArrayBuffer[FileTreeElement]()
    ft.visit(d => buf += d)
    buf.filter(e => e.getFile.isFile && e.getFile.exists).toList
  }

  private def uploadAll(keyFileMap: Map[String, File]): IO[Unit] =
    keyFileMap.foldLeft(IO.unit) {
      case (z, (k, f)) => z.flatMap(_ => upload(bucket.get, k, f).run(client))
    }
}
