package teamflow.repositories

import cats.effect.Resource
import cats.implicits._
import org.typelevel.twiddles.EmptyTuple
import skunk._
import skunk.codec.all.int8
import teamflow.domain.AnalysisId
import teamflow.domain.PaginatedResponse
import teamflow.domain.UserId
import teamflow.domain.analyses.Analysis
import teamflow.domain.analyses.AnalysisFilter
import teamflow.domain.auth.AuthedUser
import teamflow.repositories.sql.AnalysisSql
import teamflow.support.skunk.syntax.all._

trait AnalysisRepository[F[_]] {
  def get(filter: AnalysisFilter): F[PaginatedResponse[Analysis]]
  def findById(id: AnalysisId): F[Option[Analysis]]
  def findByUserId(userId: UserId): F[List[Analysis]]
  def findUsersByAnalysisId(analysisId: AnalysisId): F[List[AuthedUser.User]]
  def create(analysis: Analysis): F[Unit]
  def update(analysis: Analysis): F[Unit]
  def delete(id: AnalysisId): F[Unit]
  def linkUserToAnalysis(userId: UserId, analysisId: AnalysisId): F[Unit]
  def linkUsersToAnalysis(userIds: List[UserId], analysisId: AnalysisId): F[Unit]
  def unlinkUserFromAnalysis(userId: UserId, analysisId: AnalysisId): F[Unit]
}

object AnalysisRepository {
  def make[F[_]: fs2.Compiler.Target](
      usersRepo: UsersRepository[F]
    )(implicit
      session: Resource[F, Session[F]]
    ): AnalysisRepository[F] = new AnalysisRepository[F] {
    override def get(filter: AnalysisFilter): F[PaginatedResponse[Analysis]] = {
      val af = AnalysisSql.getByFilter(filter).paginateOpt(filter.limit, filter.page)
      for {
        analyses <- af.fragment.query(AnalysisSql.codec *: int8).queryList(af.argument)
        list = analyses.map(_.head)
        count = analyses.headOption.fold(0L)(_.tail.head)
      } yield PaginatedResponse(list, count)
    }

    override def findById(id: AnalysisId): F[Option[Analysis]] =
      AnalysisSql.findById.queryOption(id)

    override def findByUserId(userId: UserId): F[List[Analysis]] =
      AnalysisSql.findByUserId.queryList(userId)

    override def findUsersByAnalysisId(analysisId: AnalysisId): F[List[AuthedUser.User]] =
      for {
        userIds <- AnalysisSql.findUsersByAnalysisId.queryList(analysisId)
        usersMap <- usersRepo.findByIds(userIds)
        users = userIds.flatMap(usersMap.get)
      } yield users

    override def create(analysis: Analysis): F[Unit] =
      AnalysisSql.insert.execute(analysis)

    override def update(analysis: Analysis): F[Unit] =
      AnalysisSql.update.execute(analysis)

    override def delete(id: AnalysisId): F[Unit] =
      AnalysisSql.delete.execute(id)

    override def linkUserToAnalysis(userId: UserId, analysisId: AnalysisId): F[Unit] =
      AnalysisSql.insertUserAnalysis.execute(userId *: analysisId *: EmptyTuple)

    override def linkUsersToAnalysis(userIds: List[UserId], analysisId: AnalysisId): F[Unit] =
      if (userIds.isEmpty) ().pure[F]
      else {
        val userAnalyses = userIds.map(userId => userId *: analysisId *: EmptyTuple)
        AnalysisSql.insertBatchUserAnalysis(userAnalyses).execute(userAnalyses)
      }

    override def unlinkUserFromAnalysis(userId: UserId, analysisId: AnalysisId): F[Unit] =
      AnalysisSql.deleteUserAnalysis.execute(userId *: analysisId *: EmptyTuple)
  }
}
