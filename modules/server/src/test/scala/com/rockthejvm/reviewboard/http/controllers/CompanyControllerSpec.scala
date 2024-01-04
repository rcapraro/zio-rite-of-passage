package com.rockthejvm.reviewboard.http.controllers

import com.rockthejvm.reviewboard.domain.data.Company
import com.rockthejvm.reviewboard.http.requests.CreateCompanyRequest
import com.rockthejvm.reviewboard.services.CompanyService
import com.rockthejvm.reviewboard.syntax.*
import sttp.client3.*
import sttp.client3.testing.SttpBackendStub
import sttp.monad.MonadError
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.RIOMonadError
import zio.*
import zio.json.*
import zio.test.*

object CompanyControllerSpec extends ZIOSpecDefault {

  private given zioME: MonadError[Task] = new RIOMonadError[Any]

  private val rockTheJVM = Company(1, "rock-the-jvm", "Rock the JVM", "rockthejvm.com")

  private val serviceStub = new CompanyService {
    override def create(req: CreateCompanyRequest): Task[Company] =
      ZIO.succeed(rockTheJVM)

    override def getAll: Task[List[Company]] = ZIO.succeed(List(rockTheJVM))

    override def getById(id: Long): Task[Option[Company]] = ZIO.succeed {
      if (id == 1) Some(rockTheJVM) else None
    }

    override def getBySlug(slug: String): Task[Option[Company]] = ZIO.succeed {
      if (slug == "rock-the-jvm") Some(rockTheJVM) else None
    }
  }

  private def backendStubZIO(endpointFn: CompanyController => ServerEndpoint[Any, Task]) = for {
    // create the controller
    controller <- CompanyController.makeZIO
    // build tapir backend
    backendStub <- ZIO.succeed(
      TapirStubInterpreter(SttpBackendStub(MonadError[Task]))
        .whenServerEndpointRunLogic(endpointFn(controller))
        .backend()
    )
  } yield backendStub

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("CompanyControllerSpec")(
      test("post company") {
        val program = for {
          backendStub <- backendStubZIO(_.create)
          // run http request
          response <- basicRequest
            .post(uri"/companies")
            .body(CreateCompanyRequest("Rock the JVM", "rockthejvm.com").toJson)
            .send(backendStub)
        } yield response.body

        // inspect http response
        program.assert { responseBody =>
          responseBody.toOption
            .flatMap(_.fromJson[Company].toOption)
            .contains(Company(1, "rock-the-jvm", "Rock the JVM", "rockthejvm.com"))

        }
      },
      test("get all") {
        val program = for {
          backendStub <- backendStubZIO(_.getAll)
          // run http request
          response <- basicRequest
            .get(uri"/companies")
            .send(backendStub)
        } yield response.body

        // inspect http response
        program.assert { responseBody =>
          responseBody.toOption
            .flatMap(_.fromJson[List[Company]].toOption)
            .contains(List(rockTheJVM))
        }
      },
      test("get by id") {
        val program = for {
          backendStub <- backendStubZIO(_.getById)
          // run http request
          response <- basicRequest
            .get(uri"/companies/1")
            .send(backendStub)
        } yield response.body

        // inspect http response
        program.assert { responseBody =>
          responseBody.toOption
            .flatMap(_.fromJson[Company].toOption)
            .contains(rockTheJVM)
        }
      }
    ).provide(ZLayer.succeed(serviceStub))
}
