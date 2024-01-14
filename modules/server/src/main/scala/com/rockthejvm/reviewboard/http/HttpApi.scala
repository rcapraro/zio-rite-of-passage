package com.rockthejvm.reviewboard.http

import com.rockthejvm.reviewboard.http.controllers.*
import com.rockthejvm.reviewboard.services.{CompanyService, JWTService, ReviewService, UserService}
import sttp.tapir.server.ServerEndpoint
import zio.*

object HttpApi {

  private def gatherRoutes(controllers: List[BaseController]): List[ServerEndpoint[Any, Task]] =
    controllers.flatMap(_.routes)

  private def makeControllers = for {
    healthController  <- HealthController.makeZIO
    companyController <- CompanyController.makeZIO
    reviewController  <- ReviewController.makeZIO
    userController    <- UserController.makeZIO
  } yield List(healthController, companyController, reviewController, userController)

  val endpointsZIO
      : ZIO[JWTService & UserService & ReviewService & CompanyService, Nothing, List[ServerEndpoint[Any, Task]]] =
    makeControllers.map(gatherRoutes)

}
