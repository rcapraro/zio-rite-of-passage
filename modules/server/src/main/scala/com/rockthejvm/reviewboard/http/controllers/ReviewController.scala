package com.rockthejvm.reviewboard.http.controllers

import com.rockthejvm.reviewboard.http.endpoints.ReviewEndpoints
import com.rockthejvm.reviewboard.services.ReviewService
import sttp.tapir.server.ServerEndpoint
import zio.*

class ReviewController private (service: ReviewService) extends BaseController with ReviewEndpoints {

  val create: ServerEndpoint[Any, Task] =
    createReviewEndpoint.serverLogicSuccess(req => service.create(req, -1L /* TODO add user id */ ))

  val getById: ServerEndpoint[Any, Task] =
    getReviewByIdEndpoint.serverLogicSuccess(id => service.getById(id))

  val getByCompanyId: ServerEndpoint[Any, Task] =
    getByCompanyIdEndpoint.serverLogicSuccess(id => service.getByCompanyId(id))

  override val routes: List[ServerEndpoint[Any, Task]] = List(create, getById, getByCompanyId)
}

object ReviewController {
  val makeZIO: URIO[ReviewService, ReviewController] =
    ZIO.serviceWith[ReviewService](service => ReviewController(service))
}
