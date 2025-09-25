package teamflow.repositories

import cats.effect.Resource
import skunk._
import teamflow.domain.AssetId
import teamflow.domain.asset.Asset
import teamflow.repositories.sql.AssetsSql
import teamflow.support.skunk.syntax.all._

trait AssetsRepository[F[_]] {
  def create(asset: Asset): F[Unit]
  def findAsset(assetId: AssetId): F[Option[Asset]]
}

object AssetsRepository {
  def make[F[_]: fs2.Compiler.Target](
      implicit
      session: Resource[F, Session[F]]
    ): AssetsRepository[F] = new AssetsRepository[F] {
    override def create(asset: Asset): F[Unit] =
      AssetsSql.insert.execute(asset)

    override def findAsset(assetId: AssetId): F[Option[Asset]] =
      AssetsSql.findById.queryOption(assetId)
  }
}
