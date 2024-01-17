package com.rockthejvm.reviewboard.http.controllers

import com.rockthejvm.reviewboard.domain.data.UserId
import com.rockthejvm.reviewboard.http.endpoints.ReviewEndpoints
import com.rockthejvm.reviewboard.services.{JWTService, ReviewService}
import sttp.tapir.server.ServerEndpoint
import zio.*

class ReviewController private (reviewService: ReviewService, jwtService: JWTService)
    extends BaseController
    with ReviewEndpoints {

  val create: ServerEndpoint[Any, Task] =
    createReviewEndpoint
      .serverSecurityLogic[UserId, Task](token => jwtService.verifyToken(token).either)
      .serverLogic(userId => req => reviewService.create(req, userId.id).either)

  val getById: ServerEndpoint[Any, Task] =
    getReviewByIdEndpoint.serverLogic(id => reviewService.getById(id).either)

  val getByCompanyId: ServerEndpoint[Any, Task] =
    getByCompanyIdEndpoint.serverLogic(id => reviewService.getByCompanyId(id).either)

  override val routes: List[ServerEndpoint[Any, Task]] = List(create, getById, getByCompanyId)
}

object ReviewController {
  val makeZIO: URIO[ReviewService & JWTService, ReviewController] =
    for {
      reviewService <- ZIO.service[ReviewService]
      jwtService    <- ZIO.service[JWTService]
    } yield ReviewController(reviewService, jwtService)
}
