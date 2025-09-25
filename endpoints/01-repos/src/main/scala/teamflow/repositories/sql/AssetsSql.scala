package teamflow.repositories.sql

import skunk._
import skunk.implicits._
import teamflow.domain.AssetId
import teamflow.domain.asset.Asset
import teamflow.support.skunk.Sql
import teamflow.support.skunk.codecs.nes
import teamflow.support.skunk.codecs.zonedDateTime

private[repositories] object AssetsSql extends Sql[AssetId] {
  private val codec: Codec[Asset] = (id *: zonedDateTime *: nes *: nes.opt *: nes.opt).to[Asset]

  val insert: Command[Asset] =
    sql"""INSERT INTO assets VALUES ($codec)""".command

  val findById: Query[AssetId, Asset] =
    sql"""SELECT * FROM assets WHERE id = $id LIMIT 1""".query(codec)
}
