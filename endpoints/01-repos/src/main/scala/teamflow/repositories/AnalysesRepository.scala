package teamflow.repositories

import cats.effect.Resource
import cats.implicits.toFunctorOps
import skunk._
import skunk.codec.all.int8
import teamflow.domain.AnalysisId
import teamflow.domain.PaginatedResponse
import teamflow.domain.UserId
import teamflow.domain.analyses.Analysis
import teamflow.domain.analyses.AnalysisFilter
import teamflow.repositories.sql.AnalysesSql
import teamflow.support.skunk.syntax.all._

trait AnalysesRepository[F[_]] {
  def get(filter: AnalysisFilter): F[PaginatedResponse[Analysis]]
  def findById(id: AnalysisId): F[Option[Analysis]]
  def findByUserId(userId: UserId): F[List[Analysis]]
  def create(analysis: Analysis): F[Unit]
  def update(analysis: Analysis): F[Unit]
  def delete(id: AnalysisId): F[Unit]
  def linkUserToAnalysis(userId: UserId, analysisId: AnalysisId): F[Unit]
  def unlinkUserFromAnalysis(userId: UserId, analysisId: AnalysisId): F[Unit]
}

object AnalysesRepository {
  def make[F[_]: fs2.Compiler.Target](
      implicit
      session: Resource[F, Session[F]]
    ): AnalysesRepository[F] = new AnalysesRepository[F] {
    override def get(filter: AnalysisFilter): F[PaginatedResponse[Analysis]] = {
      val af = AnalysesSql.getByFilter(filter).paginateOpt(filter.limit, filter.page)
      for {
        analyses <- af.fragment.query(AnalysesSql.codec *: int8).queryList(af.argument)
        list = analyses.map(_.head)
        count = analyses.headOption.fold(0L)(_.tail.head)
      } yield PaginatedResponse(list, count)
    }

    override def findById(id: AnalysisId): F[Option[Analysis]] =
      AnalysesSql.findById.queryOption(id)

    override def findByUserId(userId: UserId): F[List[Analysis]] =
      AnalysesSql.findByUserId.queryList(userId)

    override def create(analysis: Analysis): F[Unit] =
      AnalysesSql.insert.execute(analysis)

    override def update(analysis: Analysis): F[Unit] =
      AnalysesSql.update.execute(analysis)

    override def delete(id: AnalysisId): F[Unit] =
      AnalysesSql.delete.execute(id)

    override def linkUserToAnalysis(userId: UserId, analysisId: AnalysisId): F[Unit] =
      AnalysesSql.insertUserAnalysis.execute(userId *: analysisId *: EmptyTuple)

    override def unlinkUserFromAnalysis(userId: UserId, analysisId: AnalysisId): F[Unit] =
      AnalysesSql.deleteUserAnalysis.execute(userId *: analysisId *: EmptyTuple)
  }
}
