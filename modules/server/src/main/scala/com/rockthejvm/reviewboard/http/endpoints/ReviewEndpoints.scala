package com.rockthejvm.reviewboard.http.endpoints

import com.rockthejvm.reviewboard.domain.data.Review
import com.rockthejvm.reviewboard.http.requests.*
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.*

trait ReviewEndpoints extends BaseEndpoint {

  val createReviewEndpoint: Endpoint[String, CreateReviewRequest, Throwable, Review, Any] = secureBaseEndpoints
    .tag("reviews")
    .name("create")
    .description("Create a review for a company")
    .in("reviews")
    .post
    .in(jsonBody[CreateReviewRequest])
    .out(jsonBody[Review])

  val getReviewByIdEndpoint: Endpoint[Unit, Long, Throwable, Option[Review], Any] = baseEndpoint
    .tag("reviews")
    .name("getById")
    .description("Get a review by its id")
    .in("reviews" / path[Long]("id"))
    .get
    .out(jsonBody[Option[Review]])

  val getByCompanyIdEndpoint: Endpoint[Unit, Long, Throwable, List[Review], Any] = baseEndpoint
    .tag("reviews")
    .name("getByCompanyId")
    .description("Get a review for a company")
    .in("reviews" / "companies" / path[Long]("id"))
    .get
    .out(jsonBody[List[Review]])

}
