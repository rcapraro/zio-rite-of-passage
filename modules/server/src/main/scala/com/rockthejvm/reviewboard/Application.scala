package com.rockthejvm.reviewboard

import com.rockthejvm.reviewboard.config.{Configs, JWTConfig}
import com.rockthejvm.reviewboard.http.HttpApi
import com.rockthejvm.reviewboard.repositories.{
  CompanyRepositoryLive,
  Repository,
  ReviewRepositoryLive,
  UserRepositoryLive
}
import com.rockthejvm.reviewboard.services.{CompanyServiceLive, JWTServiceLive, ReviewServiceLive, UserServiceLive}
import io.getquill.SnakeCase
import sttp.tapir.*
import sttp.tapir.server.ziohttp.{ZioHttpInterpreter, ZioHttpServerOptions}
import zio.*
import zio.http.Server

object Application extends ZIOAppDefault {

  private val serverProgram = for {
    endpoints <- HttpApi.endpointsZIO
    server <- Server.serve(
      ZioHttpInterpreter(ZioHttpServerOptions.default).toHttp(endpoints)
    )
    _ <- Console.printLine("Server is started at :8080")
  } yield ()

  override def run: Task[Unit] = serverProgram.provide(
    Server.default,
    // services
    CompanyServiceLive.layer,
    ReviewServiceLive.layer,
    UserServiceLive.layer,
    JWTServiceLive.configuredLayer,
    // repos
    CompanyRepositoryLive.layer,
    ReviewRepositoryLive.layer,
    UserRepositoryLive.layer,
    // database
    Repository.dataLayer
  )
}
