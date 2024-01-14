package com.rockthejvm.reviewboard.services

import com.rockthejvm.reviewboard.config.JWTConfig
import com.rockthejvm.reviewboard.domain.data.User
import zio.*
import zio.test.*
object JWTServiceSpec extends ZIOSpecDefault {

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("JWTServiceSpec")(
      test("create and validate token") {
        for {
          service   <- ZIO.service[JWTService]
          userToken <- service.createToken(User(1L, "daniel@rockthejvm.com", "foobar"))
          userId    <- service.verifyToken(userToken.token)
        } yield assertTrue(
          userId.id == 1L && userId.email == "daniel@rockthejvm.com"
        )
      }
    ).provide(
      JWTServiceLive.layer,
      ZLayer.succeed(JWTConfig("secret", 3600))
    )
}
