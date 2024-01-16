package com.rockthejvm.reviewboard.repositories

import com.rockthejvm.reviewboard.config.RecoveryTokensConfig
import com.rockthejvm.reviewboard.domain.data.PasswordRecoveryToken
import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.*
import com.rockthejvm.reviewboard.config.Configs

trait RecoveryTokensRepository {

  def getToken(email: String): Task[Option[String]]
  def checkToken(email: String, token: String): Task[Boolean]

}

class RecoveryTokensRepositoryLive private (
    tokensConfig: RecoveryTokensConfig,
    quill: Quill.Postgres[SnakeCase],
    userRepo: UserRepository
) extends RecoveryTokensRepository {

  import quill.*

  inline given schema: SchemaMeta[PasswordRecoveryToken]  = schemaMeta[PasswordRecoveryToken]("recovery_tokens")
  inline given insMeta: InsertMeta[PasswordRecoveryToken] = insertMeta[PasswordRecoveryToken]()
  inline given updMeta: UpdateMeta[PasswordRecoveryToken] = updateMeta[PasswordRecoveryToken](_.email)

  private val tokenDuration = tokensConfig.duration

  override def getToken(email: String): Task[Option[String]] =
    userRepo
      .getByEmail(email)
      .flatMap {
        case Some(_) => makeFreshToken(email).map(Some(_))
        case None    => ZIO.none
      }

  override def checkToken(email: String, token: String): Task[Boolean] =
    run(
      query[PasswordRecoveryToken]
        .filter(r => r.email == lift(email) && r.token == lift(token))
    ).map(_.nonEmpty)

  private def randomUppercaseString(len: Int): Task[String] = {
    ZIO.succeed(scala.util.Random.alphanumeric.take(len).mkString.toUpperCase)
  }

  private def makeFreshToken(email: String): Task[String] =
    findToken(email).flatMap {
      case Some(_) => replaceToken(email)
      case None    => generateToken(email)
    }

  private def findToken(email: String): Task[Option[String]] =
    run(
      query[PasswordRecoveryToken]
        .filter(_.email == lift(email))
    ).map(_.headOption.map(_.token))

  private def replaceToken(email: String): Task[String] =
    for {
      token <- randomUppercaseString(8)
      _ <- run(
        query[PasswordRecoveryToken]
          .updateValue(
            lift(PasswordRecoveryToken(email, token, java.lang.System.currentTimeMillis() + tokenDuration))
          )
          .returning(r => r)
      )
    } yield token

  private def generateToken(email: String): Task[String] =
    for {
      token <- randomUppercaseString(8)
      _ <- run(
        query[PasswordRecoveryToken]
          .insertValue(
            lift(PasswordRecoveryToken(email, token, java.lang.System.currentTimeMillis() + tokenDuration))
          )
          .returning(r => r)
      )
    } yield token

}

object RecoveryTokensRepositoryLive {
  val layer: URLayer[RecoveryTokensConfig & Quill.Postgres[SnakeCase] & UserRepository, RecoveryTokensRepositoryLive] =
    ZLayer {
      for {
        config   <- ZIO.service[RecoveryTokensConfig]
        quill    <- ZIO.service[Quill.Postgres[SnakeCase]]
        userRepo <- ZIO.service[UserRepository]
      } yield RecoveryTokensRepositoryLive(config, quill, userRepo)
    }

  val configuredLayer: ZLayer[UserRepository & Quill.Postgres[SnakeCase], Config.Error, RecoveryTokensRepositoryLive] =
    ZLayer.fromZIO(Configs.makeConfig[RecoveryTokensConfig]("rockthejvm.recoveryTokens")) >>> layer
}
