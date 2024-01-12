package com.rockthejvm.reviewboard.services

import com.auth0.jwt.*
import com.auth0.jwt.JWTVerifier.BaseVerification
import com.auth0.jwt.algorithms.Algorithm
import com.rockthejvm.reviewboard.domain.data.{User, UserId, UserToken}
import zio.*

import java.time.Instant

trait JWTService {
  def createToken(user: User): Task[UserToken]
  def verifyToken(token: String): Task[UserId]
}

class JWTServiceLive private (clock: java.time.Clock) extends JWTService {

  private val SECRET         = "secret"        // TODO pass this from config
  private val ISSUER         = "rockthejvm.com"
  private val TTL            = 30 * 24 * 36000 // TODO pass this from config
  private val CLAIM_USERNAME = "username"

  private val algorithm = Algorithm.HMAC512(SECRET)
  private val verifier: JWTVerifier =
    JWT
      .require(algorithm)
      .withIssuer(ISSUER)
      .asInstanceOf[BaseVerification]
      .build(clock)

  override def createToken(user: User): Task[UserToken] =
    for {
      now        <- ZIO.attempt(clock.instant())
      expiration <- ZIO.succeed(now.plusSeconds(TTL))
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
  val layer: ULayer[JWTServiceLive] = ZLayer {
    Clock.javaClock.map(clock => new JWTServiceLive(clock))
  }
}

object JWTServiceDemo extends ZIOAppDefault {

  private val program = for {
    service   <- ZIO.service[JWTService]
    userToken <- service.createToken(User(1L, "daniel@rockthejvm.com", "unimportant"))
    _         <- Console.printLine(userToken)
    userId    <- service.verifyToken(userToken.token)
    _         <- Console.printLine(userId)
  } yield ()

  override def run: ZIO[Any & ZIOAppArgs with Scope, Any, Any] =
    program.provide(JWTServiceLive.layer)

}
