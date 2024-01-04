package com.rockthejvm

import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.ziohttp.{ZioHttpInterpreter, ZioHttpServerOptions}
import zio.*
import zio.http.Server

import scala.collection.mutable

object TapirDemo extends ZIOAppDefault {

  val simplestEndpoint = endpoint
    .tag("simple")
    .name("simple")
    .description("simplest endpoint possible")
    .get
    .in("simple")
    .out(plainBody[String])
    .serverLogicSuccess[Task](_ => ZIO.succeed("All good!"))

  val simpleServerProgram = Server.serve(
    ZioHttpInterpreter(
      ZioHttpServerOptions.default // can add configs e.g CORS
    ).toHttp(simplestEndpoint)
  )

  // simulate a job board
  val db: mutable.Map[Long, Job] = mutable.Map(
    1L -> Job(1L, "Instructor", "rockthejvm.com", "Rock the JVM")
  )

  // create
  // get by id
  // get all
  val getByIdEndpoint: ServerEndpoint[Any, Task] = endpoint
    .tag("jobs")
    .name("getById")
    .description("get a job by id")
    .in("jobs" / path[Long]("id"))
    .get
    .out(jsonBody[Option[Job]])
    .serverLogicSuccess(id => ZIO.succeed(db.get(id)))

  val createEndpoint: ServerEndpoint[Any, Task] = endpoint
    .tag("jobs")
    .name("create")
    .description("create a job")
    .in("jobs")
    .post
    .in(jsonBody[CreateJobRequest])
    .out(jsonBody[Job])
    .serverLogicSuccess(req =>
      ZIO.succeed {
        val newId  = db.keys.max + 1
        val newJob = Job(newId, req.title, req.url, req.company)
        db += (newId -> newJob)
        newJob
      }
    )

  val getAllEndpoint: ServerEndpoint[Any, Task] = endpoint
    .tag("jobs")
    .name("getAll")
    .description("get all jobs")
    .in("jobs")
    .get
    .out(jsonBody[List[Job]])
    .serverLogicSuccess(_ => ZIO.succeed(db.values.toList))

  val serverProgram = Server.serve(
    ZioHttpInterpreter(
      ZioHttpServerOptions.default // can add configs e.g CORS
    ).toHttp(List(getByIdEndpoint, createEndpoint, getAllEndpoint))
  )

  override def run = serverProgram.provide(
    Server.default // port 8080
  )

}
