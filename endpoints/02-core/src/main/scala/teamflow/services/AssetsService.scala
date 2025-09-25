package teamflow.services

import scala.reflect.runtime.{ universe => ru }

import cats.effect.Sync
import cats.implicits._
import teamflow.Language
import teamflow.domain.AssetId
import teamflow.domain.asset.Asset
import teamflow.domain.asset.FileMeta
import teamflow.effects.Calendar
import teamflow.effects.FileLoader
import teamflow.effects.GenUUID
import teamflow.integration.aws.s3.S3Client
import teamflow.repositories.AssetsRepository
import teamflow.syntax.refined._
import teamflow.utils.ID

trait AssetsService[F[_]] {
  def create(meta: FileMeta[F]): F[AssetId]
  def getFile(path: String)(implicit lang: Language): F[String]
}

object AssetsService {
  def make[F[_]: Sync: FileLoader: GenUUID: Calendar: Lambda[M[_] => fs2.Compiler[M, M]]](
      assetsRepository: AssetsRepository[F],
      s3Client: S3Client[F],
    ): AssetsService[F] =
    new AssetsService[F] {
      override def create(meta: FileMeta[F]): F[AssetId] =
        for {
          id <- ID.make[F, AssetId]
          now <- Calendar[F].currentZonedDateTime
          key <- genFileKey(meta.fileName)
          asset = Asset(
            id = id,
            createdAt = now,
            s3Key = key,
            fileName = meta.fileName.some,
            contentType = meta.contentType,
          )
          _ <- meta.bytes.through(s3Client.putObject(key)).compile.drain
          _ <- assetsRepository.create(asset)
        } yield id

      private def getFileType(filename: String): String = {
        val extension = filename.substring(filename.lastIndexOf('.') + 1)
        extension.toLowerCase
      }

      private def genFileKey(orgFilename: String): F[String] =
        GenUUID[F].make.map { uuid =>
          uuid.toString + "." + getFileType(orgFilename)
        }

      def getFile(path: String)(implicit lang: Language): F[String] = for {
        file <- FileLoader[F].resourceAsString(path)
        placeholderPattern = "%%(.*?)%%".r
        replacements = placeholderPattern.findAllMatchIn(file).map(_.group(1)).toSet
        translations <- replacements.toList.traverse { key =>
          invokeTranslationF(key).map(valueOpt => key -> valueOpt)
        }
        translationMap = translations.collect { case (k, Some(v)) => k -> v }.toMap
        content = translationMap.foldLeft(file) {
          case (acc, (key, value)) =>
            acc.replaceAll(s"%%$key%%", value)
        }
      } yield content

      def invokeTranslationF(name: String)(implicit lang: Language): F[Option[String]] =
        Sync[F].delay {
          val mirror = ru.runtimeMirror(getClass.getClassLoader)
          val module = mirror.staticModule("bot.ResponseMessages")
          val obj = mirror.reflectModule(module).instance
          val im = mirror.reflect(obj)
          val tpe = module.typeSignature
          val symbol = tpe.decl(ru.TermName(name))

          if (symbol == ru.NoSymbol) None
          else if (symbol.isTerm) {
            val term = symbol.asTerm
            if (term.isVal) {
              val fieldMirror = im.reflectField(term)
              val map = fieldMirror.get.asInstanceOf[Map[Language, String]]
              map.get(lang)
            }
            else if (term.isMethod && term.asMethod.paramLists.flatten.isEmpty) {
              val methodMirror = im.reflectMethod(term.asMethod)
              val map = methodMirror().asInstanceOf[Map[Language, String]]
              map.get(lang)
            }
            else None
          }
          else None
        }
    }
}
