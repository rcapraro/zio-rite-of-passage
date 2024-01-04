package com.rockthejvm.reviewboard.domain.data

import zio.json.{DeriveJsonCodec, JsonCodec}

case class Company(
    id: Long,
    slug: String,
    name: String,
    url: String,
    location: Option[String]=None,
    country: Option[String]=None,
    industry: Option[String]=None,
    image: Option[String]=None,
    tags: List[String]=List.empty
)

object Company {
  given codec: JsonCodec[Company] = DeriveJsonCodec.gen[Company]
  
  def makeSlug(name: String): String =
    name
      .replaceAll(" +", " ")
      .split(" ")
      .map(_.toLowerCase())
      .mkString("-") // My Company -> my-company
}
