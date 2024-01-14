package com.rockthejvm

import zio.json.{DeriveJsonCodec, JsonCodec}

final case class Job(
    id: Long,
    title: String,
    url: String,
    company: String
)

object Job {
  given codec: JsonCodec[Job] = DeriveJsonCodec.gen[Job]
}

final case class CreateJobRequest(
    title: String,
    url: String,
    company: String
)

object CreateJobRequest {
  given codec: JsonCodec[CreateJobRequest] = DeriveJsonCodec.gen[CreateJobRequest]
}
