package com.rockthejvm.reviewboard.http.controllers

import com.rockthejvm.reviewboard.domain.data.Company
import com.rockthejvm.reviewboard.http.endpoints.CompanyEndpoints
import sttp.tapir.server.ServerEndpoint
import zio.*

import scala.collection.mutable

class CompanyController private extends BaseController with CompanyEndpoints {

  val db: mutable.Map[Long, Company] = mutable.Map[Long, Company]()

  // create
  val create: ServerEndpoint[Any, Task] = createEndpoint.serverLogicSuccess { req =>
    ZIO.succeed {
      val newId      = db.keys.maxOption.getOrElse(0L) + 1
      val newCompany = req.toCompany(newId)
      db += (newId -> newCompany)
      newCompany
    }
  }

  // getAll
  val getAll: ServerEndpoint[Any, Task] = getAllEndpoint.serverLogicSuccess(_ => ZIO.succeed(db.values.toList))

  // getById
  val getById: ServerEndpoint[Any, Task] = getByIdEndpoint.serverLogicSuccess { id =>
    ZIO
      .attempt(id.toLong)
      .map(db.get)
  }

  override val routes: List[ServerEndpoint[Any, Task]] = List(create, getAll, getById)
}

object CompanyController {
  val makeZIO: UIO[CompanyController] = ZIO.succeed(CompanyController())
}
