package teamflow.repositories.sql

import skunk._
import skunk.implicits._
import teamflow.domain.AgentId
import teamflow.domain.AnalysisId
import teamflow.domain.ProjectId
import teamflow.domain.UserId
import teamflow.domain.analyses.Analysis
import teamflow.domain.analyses.AnalysisFilter
import teamflow.support.skunk.Sql
import teamflow.support.skunk.codecs.nes
import teamflow.support.skunk.codecs.zonedDateTime
import teamflow.support.skunk.syntax.all.skunkSyntaxFragmentOps

private[repositories] object AnalysesSql extends Sql[AnalysisId] {
  private[repositories] val codec: Codec[Analysis] =
    (id *: zonedDateTime *: ProjectsSql.id *: AgentsSql.id *: nes).to[Analysis]

  val insert: Command[Analysis] =
    sql"""
      INSERT INTO analyses (id, created_at, project_id, agent_id, response)
      VALUES ($codec)
    """.command

  def getByFilter(filter: AnalysisFilter): AppliedFragment = {
    val searchFilter: List[Option[AppliedFragment]] = List(
      filter.projectId.map(sql"a.project_id = ${ProjectsSql.id}"),
      filter.agentId.map(sql"a.agent_id = ${AgentsSql.id}"),
      filter.userId.map(sql"ua.user_id = ${UsersSql.id}"),
    )

    val query: AppliedFragment =
      void"""
        SELECT
          id, created_at, project_id, agent_id, response, COUNT(*) OVER()
        FROM analyses a
        INNER JOIN user_analyses ua ON ua.analysis_id = a.id
      """

    query.whereAndOpt(searchFilter) |+| void""" ORDER BY a.created_at DESC"""
  }

  val findById: Query[AnalysisId, Analysis] =
    sql"""
      SELECT
        id, created_at, project_id, agent_id, response
      FROM analyses
      WHERE id = $id
      LIMIT 1
    """.query(codec)

  val findByUserId: Query[UserId, Analysis] =
    sql"""
      SELECT
        a.id, a.created_at, a.project_id, a.agent_id, a.response
      FROM analyses a
      INNER JOIN user_analyses ua ON a.id = ua.analysis_id
      WHERE ua.user_id = ${UsersSql.id}
      ORDER BY a.created_at DESC
    """.query(codec)

  val update: Command[Analysis] =
    sql"""
      UPDATE analyses SET
        project_id = ${ProjectsSql.id},
        agent_id = ${AgentsSql.id},
        response = $nes
      WHERE id = $id
    """
      .command
      .contramap { (a: Analysis) =>
        a.projectId *: a.agentId *: a.response *: a.id *: EmptyTuple
      }

  val delete: Command[AnalysisId] =
    sql"""DELETE FROM analyses WHERE id = $id""".command

  // UserAnalysis operations
  val insertUserAnalysis: Command[UserId *: AnalysisId *: EmptyTuple] =
    sql"""
      INSERT INTO user_analyses (user_id, analysis_id)
      VALUES (${UsersSql.id}, $id)
    """.command

  val deleteUserAnalysis: Command[UserId *: AnalysisId *: EmptyTuple] =
    sql"""
      DELETE FROM user_analyses 
      WHERE user_id = ${UsersSql.id} AND analysis_id = $id
    """.command
}
