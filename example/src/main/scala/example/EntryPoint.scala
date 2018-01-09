package example

import cats.effect.IO
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.S3Event
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.CopyObjectRequest
import com.typesafe.scalalogging.LazyLogging
import org.slf4j.MDC

import scala.collection.JavaConverters._

class EntryPoint extends LazyLogging {

  val config = Config.load
  val s3 = AmazonS3ClientBuilder.standard().withRegion(config.awsRegion).build()

  def handler(e: S3Event, ctx: Context): Unit =
    (for {
      _ <- updateMdc(ctx)
      _ <- copyAll(e)
    } yield ()).unsafeRunSync()

  private def copyAll(e: S3Event): IO[Unit] = {
    val copies = e.getRecords.asScala.map(_.getS3).map { o =>
      copy(o.getBucket.getName, o.getObject.getKey, o.getObject.getVersionId)
    }
    copies.foldLeft(IO.unit)((z, c) => z.flatMap(_ => c))
  }

  private def copy(bucket: String, key: String, vid: String): IO[Unit] =
    for {
      _ <- IO(s3.copyObject(new CopyObjectRequest(bucket, key, vid, config.destinationBucket, key)))
      _ <- IO(logger.info(s"Copied $bucket/$key version $vid to ${config.destinationBucket}"))
    } yield ()

  private def updateMdc(ctx: Context): IO[Unit] =
    IO {
      MDC.put("ServiceName", config.serviceName)
      MDC.put("ServiceVersion", config.serviceVersion)
      MDC.put("RequestId", ctx.getAwsRequestId)
      MDC.put("FunctionName", ctx.getFunctionName)
      MDC.put("FunctionVersion", ctx.getFunctionVersion)
    }
}
