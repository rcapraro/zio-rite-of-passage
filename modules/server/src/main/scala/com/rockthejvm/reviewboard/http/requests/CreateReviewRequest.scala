package com.rockthejvm.reviewboard.http.requests

import zio.json.{DeriveJsonCodec, JsonCodec}

import java.time.Instant

case class CreateReviewRequest(
    companyId: Long,
    management: Int,
    culture: Int,
    salary: Int,
    benefits: Int,
    wouldRecommend: Int,
    review: String
)

object Review {
  given codec: JsonCodec[CreateReviewRequest] = DeriveJsonCodec.gen[CreateReviewRequest]
}
