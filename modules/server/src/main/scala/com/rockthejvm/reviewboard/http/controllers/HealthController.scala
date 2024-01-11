package com.rockthejvm.reviewboard.http.controllers

import com.rockthejvm.reviewboard.domain.errors.HttpError
import com.rockthejvm.reviewboard.http.endpoints.HealthEndpoint
import sttp.tapir.*
import sttp.tapir.server.ServerEndpoint
import zio.*

class HealthController private extends BaseController with HealthEndpoint {

  val health: ServerEndpoint[Any, Task] =
    healthEndpoint
      .serverLogicSuccess[Task](_ => ZIO.succeed("All good!"))

  private val errorRoute =
    errorEndpoint
      .serverLogic[Task](_ => ZIO.fail(new RuntimeException("Boom !")).either)

  override val routes: List[ServerEndpoint[Any, Task]] = List(health, errorRoute)

}

object HealthController {
  val makeZIO: UIO[HealthController] = ZIO.succeed(new HealthController)
}
