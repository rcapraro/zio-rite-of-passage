package com.rockthejvm.reviewboard.http.endpoints

import sttp.tapir.*

trait HealthEndpoint {

  val healthEndpoint: Endpoint[Unit, Unit, Unit, String, Any] = endpoint
    .tag("health")
    .name("health")
    .description("Health check")
    .get
    .in("health")
    .out(plainBody[String])

}
