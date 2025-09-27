package teamflow.repositories.sql

import skunk._
import skunk.implicits._
import teamflow.domain.ProjectId
import teamflow.domain.projects.Project
import teamflow.domain.projects.ProjectFilter
import teamflow.support.skunk.Sql
import teamflow.support.skunk.codecs.nes
import teamflow.support.skunk.syntax.all.skunkSyntaxFragmentOps

private[repositories] object ProjectsSql extends Sql[ProjectId] {
  private[repositories] val codec: Codec[Project] =
    (id *: nes *: nes).to[Project]

  val insert: Command[Project] =
    sql"""
      INSERT INTO projects (id, name, url)
      VALUES ($codec)
    """.command

  def getByFilter(filter: ProjectFilter): AppliedFragment = {
    val searchFilter: List[Option[AppliedFragment]] = List(
      filter.name.map(sql"name ILIKE '%' || $nes || '%'")
    )

    val query: AppliedFragment =
      void"""
        SELECT
          id, name, url, COUNT(*) OVER()
        FROM projects
      """

    query.whereAndOpt(searchFilter) |+| void""" ORDER BY name ASC"""
  }

  val findById: Query[ProjectId, Project] =
    sql"""
      SELECT
        id, name, url
      FROM projects
      WHERE id = $id
      LIMIT 1
    """.query(codec)

  def findByIds(ids: List[ProjectId]): Query[ids.type, Project] =
    sql"""
      SELECT id, name, url
      FROM projects
      WHERE id IN (${id.values.list(ids)})
    """.query(codec)

  val update: Command[Project] =
    sql"""
      UPDATE projects SET
        name = $nes,
        url = $nes
      WHERE id = $id
    """
      .command
      .contramap { (p: Project) =>
        p.name *: p.url *: p.id *: EmptyTuple
      }

  val delete: Command[ProjectId] =
    sql"""DELETE FROM projects WHERE id = $id""".command
}
