package teamflow.repositories.sql

import skunk._
import skunk.implicits._
import teamflow.domain.AgentId
import teamflow.domain.agents.Agent
import teamflow.domain.agents.AgentFilter
import teamflow.support.skunk.Sql
import teamflow.support.skunk.codecs.nes
import teamflow.support.skunk.codecs.zonedDateTime
import teamflow.support.skunk.syntax.all.skunkSyntaxFragmentOps

private[repositories] object AgentsSql extends Sql[AgentId] {
  private[repositories] val codec: Codec[Agent] =
    (id *: zonedDateTime *: nes *: nes *: nes.opt).to[Agent]

  val insert: Command[Agent] =
    sql"""
      INSERT INTO agents (id, created_at, name, prompt, description)
      VALUES ($codec)
    """.command

  def getByFilter(filter: AgentFilter): AppliedFragment = {
    val searchFilter: List[Option[AppliedFragment]] = List(
      filter.name.map(sql"name ILIKE '%' || $nes || '%'")
    )

    val query: AppliedFragment =
      void"""
        SELECT
          id, created_at, name, prompt, description, COUNT(*) OVER()
        FROM agents
      """

    query.whereAndOpt(searchFilter) |+| void""" ORDER BY created_at DESC"""
  }

  val findById: Query[AgentId, Agent] =
    sql"""
      SELECT
        id, created_at, name, prompt, description
      FROM agents
      WHERE id = $id
      LIMIT 1
    """.query(codec)

  def findByIds(ids: List[AgentId]): Query[ids.type, Agent] =
    sql"""
      SELECT id, created_at, name, prompt, description
      FROM agents
      WHERE id IN (${id.values.list(ids)})
    """.query(codec)

  val update: Command[Agent] =
    sql"""
      UPDATE agents SET
        name = $nes,
        prompt = $nes,
        description = ${nes.opt}
      WHERE id = $id
    """
      .command
      .contramap { (a: Agent) =>
        a.name *: a.prompt *: a.description *: a.id *: EmptyTuple
      }

  val delete: Command[AgentId] =
    sql"""DELETE FROM agents WHERE id = $id""".command
}
