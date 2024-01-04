package com.rockthejvm.reviewboard.http.controllers

import com.rockthejvm.reviewboard.http.endpoints.HealthEndpoint
import sttp.tapir.server.ServerEndpoint
import zio.*

class HealthController private extends BaseController with HealthEndpoint {

  val health = healthEndpoint.serverLogicSuccess[Task](_ => ZIO.succeed("All good!"))

  override val routes: List[ServerEndpoint[Any, Task]] = List(health)
  
}


object HealthController {
  val makeZIO: UIO[HealthController] = ZIO.succeed(new HealthController)
}