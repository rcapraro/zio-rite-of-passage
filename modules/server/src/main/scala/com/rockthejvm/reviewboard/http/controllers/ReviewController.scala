package com.rockthejvm.reviewboard.http.controllers

import com.rockthejvm.reviewboard.http.endpoints.ReviewEndpoints
import com.rockthejvm.reviewboard.services.ReviewService
import sttp.tapir.server.ServerEndpoint
import zio.*

class ReviewController private (service: ReviewService) extends BaseController with ReviewEndpoints {

  val create: ServerEndpoint[Any, Task] =
    createReviewEndpoint.serverLogic(req => service.create(req, -1L /* TODO add user id */ ).either)

  val getById: ServerEndpoint[Any, Task] =
    getReviewByIdEndpoint.serverLogic(id => service.getById(id).either)

  val getByCompanyId: ServerEndpoint[Any, Task] =
    getByCompanyIdEndpoint.serverLogic(id => service.getByCompanyId(id).either)

  override val routes: List[ServerEndpoint[Any, Task]] = List(create, getById, getByCompanyId)
}

object ReviewController {
  val makeZIO: URIO[ReviewService, ReviewController] =
    ZIO.serviceWith[ReviewService](service => ReviewController(service))
}
