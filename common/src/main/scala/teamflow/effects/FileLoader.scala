package teamflow.effects

import cats.Applicative
import cats.effect.Sync
import cats.implicits._
import fs2._
import fs2.io.file.Files

import java.io.{FileNotFoundException, InputStream}
import scala.io.{Codec, Source}

trait FileLoader[F[_]] {
  def inputStreamAsString(is: InputStream): F[String]
  def resourceAsInputStream(path: String): F[InputStream]
  def resourceAsF2Stream(path: String): Stream[F, Byte]
  def resourceAsString(path: String): F[String]
  def getResourceString(path: String): Stream[F, String]
  def decodeToString: Pipe[F, Byte, String]
}

object FileLoader {
  def apply[F[_]](implicit ev: FileLoader[F]): FileLoader[F] = ev

  implicit def fileLoader[F[_]: Files](implicit F: Sync[F]): FileLoader[F] =
    new FileLoader[F] {
      implicit val codec: Codec = Codec.UTF8

      override def inputStreamAsString(is: InputStream): F[String] =
        for {
          source <- F.delay(Source.fromInputStream(is))
          lines = source.mkString
          _ <- F.delay(source.close())
        } yield lines

      override def resourceAsInputStream(path: String): F[InputStream] =
        F.delay(getClass.getResourceAsStream(path))

      override def resourceAsString(path: String): F[String] =
        for {
          source <- F.delay(Source.fromResource(path))
          lines = source.mkString
          _ <- F.delay(source.close())
        } yield lines

      override def resourceAsF2Stream(path: String): Stream[F, Byte] = {
        val classLoader = getClass.getClassLoader
        Option(classLoader.getResourceAsStream(path)) match {
          case Some(is) =>
            fs2.io.readInputStream(Sync[F].pure(is), 4096, closeAfterUse = true)
          case None =>
            Stream.raiseError[F](new FileNotFoundException(s"Resource not found: $path"))
        }
      }

      override def decodeToString: Pipe[F, Byte, String] =
        _.through(text.utf8.decode)
          .through(text.lines)

      override def getResourceString(path: String): Stream[F, String] =
        resourceAsF2Stream(path).through(decodeToString)
    }

  def noOp[F[_]: Applicative]: FileLoader[F] = new FileLoader[F] {
    override def inputStreamAsString(is: InputStream): F[String] =
      Applicative[F].pure("TEST")
    override def resourceAsInputStream(path: String): F[InputStream] =
      Applicative[F].pure(InputStream.nullInputStream())
    override def resourceAsF2Stream(path: String): Stream[F, Byte] =
      Stream.empty
    override def resourceAsString(path: String): F[String] =
      Applicative[F].pure("TEST")
    override def getResourceString(
        path: String
      ): Stream[F, String] =
      Stream.empty
    override def decodeToString: Pipe[F, Byte, String] =
      _ => Stream.empty
  }
}
