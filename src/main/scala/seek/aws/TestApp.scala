package seek.aws

import com.typesafe.scalalogging.LazyLogging
import io.circe.Decoder

object TestApp extends App with LazyLogging {

  val json =
    """
      |{
      |  "a": "A",
      |  "b": null,
      |  "c": 8,
      |  "d": {
      |    "e": "E"
      |  },
      |  "e": [ 4, 5, 6 ]
      |}
    """.stripMargin

  import io.circe.parser._

  implicit val decoder: Decoder[Map[String, String]] =
    Decoder.instance { c =>
      c.as[String] match {
        case Right(s) =>
          Right(Map.empty[String, String])
        case l @ Left(_) => l.asInstanceOf[Decoder.Result[Map[String, String]]]
      }
    }
  val parsed = parse(json).right.get



  //parsed.as[Map[String, String]]
}
