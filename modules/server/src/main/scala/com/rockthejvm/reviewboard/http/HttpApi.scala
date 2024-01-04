package com.rockthejvm.reviewboard.http

import com.rockthejvm.reviewboard.http.controllers.{BaseController, CompanyController, HealthController}
import com.rockthejvm.reviewboard.services.CompanyService
import sttp.tapir.server.ServerEndpoint
import zio.*

object HttpApi {

  private def gatherRoutes(controllers: List[BaseController]): List[ServerEndpoint[Any, Task]] =
    controllers.flatMap(_.routes)

  private def makeControllers: URIO[CompanyService, List[BaseController]] = for {
    healthController  <- HealthController.makeZIO
    companyController <- CompanyController.makeZIO
  } yield List(healthController, companyController)

  val endpointsZIO: URIO[CompanyService, List[ServerEndpoint[Any, Task]]] = makeControllers.map(gatherRoutes)

}
