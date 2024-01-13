package com.rockthejvm.reviewboard.services

import com.auth0.jwt.*
import com.auth0.jwt.JWTVerifier.BaseVerification
import com.auth0.jwt.algorithms.Algorithm
import com.rockthejvm.reviewboard.config.{Configs, JWTConfig}
import com.rockthejvm.reviewboard.domain.data.{User, UserId, UserToken}
import zio.*
import zio.config.*
trait JWTService {
  def createToken(user: User): Task[UserToken]
  def verifyToken(token: String): Task[UserId]
}

class JWTServiceLive private (jwtConfig: JWTConfig, clock: java.time.Clock) extends JWTService {

  private val ISSUER         = "rockthejvm.com"
  private val CLAIM_USERNAME = "username"

  private val algorithm = Algorithm.HMAC512(jwtConfig.secret)
  private val verifier: JWTVerifier =
    JWT
      .require(algorithm)
      .withIssuer(ISSUER)
      .asInstanceOf[BaseVerification]
      .build(clock)

  override def createToken(user: User): Task[UserToken] =
    for {
      now        <- ZIO.attempt(clock.instant())
      expiration <- ZIO.succeed(now.plusSeconds(jwtConfig.ttl))
      token <- ZIO
        .attempt(
          JWT
            .create()
            .withIssuer(ISSUER)
            .withIssuedAt(now)
            .withExpiresAt(expiration)
            .withSubject(user.id.toString)
            .withClaim(CLAIM_USERNAME, user.email)
            .sign(algorithm)
        )
    } yield UserToken(user.email, token, expiration.getEpochSecond)

  override def verifyToken(token: String): Task[UserId] =
    for {
      decoded <- ZIO.attempt(verifier.verify(token))
      userId <- ZIO.attempt(
        UserId(
          id = decoded.getSubject.toLong,
          email = decoded.getClaim(CLAIM_USERNAME).asString()
        )
      )
    } yield userId
}

object JWTServiceLive {
  val layer: URLayer[JWTConfig, JWTServiceLive] = ZLayer {
    for {
      jwtConfig <- ZIO.service[JWTConfig]
      clock     <- Clock.javaClock
    } yield new JWTServiceLive(jwtConfig, clock)
  }

  val configuredLayer: Layer[Config.Error, JWTServiceLive] =
    ZLayer.fromZIO(Configs.makeConfig[JWTConfig]("rockthejvm.jwt")) >>> layer
}

object JWTServiceDemo extends ZIOAppDefault {

  private val program = for {
    service   <- ZIO.service[JWTService]
    userToken <- service.createToken(User(1L, "daniel@rockthejvm.com", "unimportant"))
    _         <- Console.printLine(userToken)
    userId    <- service.verifyToken(userToken.token)
    _         <- Console.printLine(userId)
  } yield ()

  override def run: ZIO[Any & ZIOAppArgs with Scope, Any, Any] = {
    program.provide(
      ZLayer.fromZIO(Configs.makeConfig[JWTConfig]("rockthejvm.jwt")),
      JWTServiceLive.layer
    )
  }

}
