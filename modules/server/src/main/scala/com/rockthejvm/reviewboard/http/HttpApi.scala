package com.rockthejvm.reviewboard.http

import com.rockthejvm.reviewboard.http.controllers.{BaseController, CompanyController, HealthController}
import sttp.tapir.server.ServerEndpoint
import zio.*

object HttpApi {

  def gatherRoutes(controllers: List[BaseController]): List[ServerEndpoint[Any, Task]] =
    controllers.flatMap(_.routes)

  def makeControllers: UIO[List[BaseController]] = for {
    healthController <- HealthController.makeZIO
    companyController <- CompanyController.makeZIO
  } yield List(healthController, companyController)

  val endpointsZIO: UIO[List[ServerEndpoint[Any, Task]]] = makeControllers.map(gatherRoutes)

}
