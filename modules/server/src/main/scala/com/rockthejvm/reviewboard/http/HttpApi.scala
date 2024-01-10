package com.rockthejvm.reviewboard.http

import com.rockthejvm.reviewboard.http.controllers.{
  BaseController,
  CompanyController,
  HealthController,
  ReviewController
}
import com.rockthejvm.reviewboard.services.{CompanyService, ReviewService}
import sttp.tapir.server.ServerEndpoint
import zio.*

object HttpApi {

  private def gatherRoutes(controllers: List[BaseController]): List[ServerEndpoint[Any, Task]] =
    controllers.flatMap(_.routes)

  private def makeControllers: URIO[CompanyService & ReviewService, List[BaseController]] = for {
    healthController  <- HealthController.makeZIO
    companyController <- CompanyController.makeZIO
    reviewController  <- ReviewController.makeZIO
  } yield List(healthController, companyController, reviewController)

  val endpointsZIO: URIO[CompanyService & ReviewService, List[ServerEndpoint[Any, Task]]] =
    makeControllers.map(gatherRoutes)

}
