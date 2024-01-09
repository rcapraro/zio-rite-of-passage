package com.rockthejvm.reviewboard.repositories

import com.rockthejvm.reviewboard.domain.data.Company
import com.rockthejvm.reviewboard.syntax.*
import zio.*
import zio.test.*

import java.sql.SQLException
import javax.sql.DataSource
object CompanyRepositorySpec extends ZIOSpecDefault with RepositorySpec {

  override val initScript: String = "sql/companies.sql"

  private val rtjvm = Company(1L, "rock-the-jvm", "Rock the JVM", "rockthejvm.com")

  private def genCompany(): Company =
    Company(-1L, slug = genString(), name = genString(), url = genString())

  private def genString(): String = scala.util.Random.alphanumeric.take(8).mkString

  override def spec: Spec[TestEnvironment & Scope, Any] = {
    suite("CompanyRepositorySpec")(
      test("create a company") {
        val program = for {
          repo    <- ZIO.service[CompanyRepository]
          company <- repo.create(rtjvm)
        } yield company

        program.assert {
          case Company(_, "rock-the-jvm", "Rock the JVM", "rockthejvm.com", _, _, _, _, _) => true
          case Company(_, _, _, _, _, _, _, _, _)                                          => false
        }
      },
      test("create a duplicate should err") {
        val program = for {
          repo <- ZIO.service[CompanyRepository]
          _    <- repo.create(rtjvm)
          err  <- repo.create(rtjvm).flip
        } yield err

        program.assert(_.isInstanceOf[SQLException])
      },
      test("get by id and slug a company") {
        val program = for {
          repo          <- ZIO.service[CompanyRepository]
          company       <- repo.create(rtjvm)
          fetchedById   <- repo.getById(company.id)
          fetchedBySlug <- repo.getBySlug(company.slug)
        } yield (company, fetchedById, fetchedBySlug)

        program.assert { case (company, fetchedById, fetchedBySlug) =>
          fetchedById.contains(company) && fetchedBySlug.contains(company)
        }
      },
      test("update a company") {
        val program = for {
          repo        <- ZIO.service[CompanyRepository]
          company     <- repo.create(rtjvm)
          updated     <- repo.update(company.id, _.copy(url = "blog.rockthejvm.com"))
          fetchedById <- repo.getById(company.id)
        } yield (updated, fetchedById)

        program.assert { case (updated, fetchedById) =>
          fetchedById.contains(updated)
        }
      },
      test("delete a company") {
        val program = for {
          repo        <- ZIO.service[CompanyRepository]
          company     <- repo.create(rtjvm)
          _           <- repo.delete(company.id)
          fetchedById <- repo.getById(company.id)
        } yield fetchedById

        program.assert(_.isEmpty)
      },
      test("getAll companies") {
        val program = for {
          repo             <- ZIO.service[CompanyRepository]
          companies        <- ZIO.foreach(1 to 10)(_ => repo.create(genCompany()))
          companiesFetched <- repo.getAll
        } yield (companies, companiesFetched)

        program.assert { case (companies, companiesFetched) =>
          companies.toSet == companiesFetched.toSet
        }
      }
    ).provide(
      CompanyRepositoryLive.layer,
      Repository.quillLayer,
      postgresDataSourceLayer,
      Scope.default
    )

  }

}
