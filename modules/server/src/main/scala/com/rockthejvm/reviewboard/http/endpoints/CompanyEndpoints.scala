package com.rockthejvm.reviewboard.http.endpoints

import com.rockthejvm.reviewboard.domain.data.Company
import com.rockthejvm.reviewboard.domain.errors.HttpError
import com.rockthejvm.reviewboard.http.requests.*
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.*

trait CompanyEndpoints extends BaseEndpoint {

  val createEndpoint: Endpoint[Unit, CreateCompanyRequest, Throwable, Company, Any] = baseEndpoint
    .tag("companies")
    .name("create")
    .description("Create a listing for a company")
    .in("companies")
    .post
    .in(jsonBody[CreateCompanyRequest])
    .out(jsonBody[Company])

  val getAllEndpoint: Endpoint[Unit, Unit, Throwable, List[Company], Any] =
    baseEndpoint
      .tag("companies")
      .name("getAll")
      .description("Get all company listing")
      .in("companies")
      .get
      .out(jsonBody[List[Company]])

  val getByIdEndpoint: Endpoint[Unit, String, Throwable, Option[Company], Any] =
    baseEndpoint
      .tag("companies")
      .name("getById")
      .description("Get a company by its id")
      .in("companies" / path[String]("id"))
      .get
      .out(jsonBody[Option[Company]])

}
