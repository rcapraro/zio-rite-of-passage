package com.rockthejvm.reviewboard.http.endpoints

import com.rockthejvm.reviewboard.domain.errors.HttpError
import sttp.tapir.*

trait BaseEndpoint {

  val baseEndpoint: Endpoint[Unit, Unit, Throwable, Unit, Any] = endpoint
    .errorOut(statusCode and plainBody[String])
    .mapErrorOut[Throwable](HttpError.decode)(HttpError.encode)

}
