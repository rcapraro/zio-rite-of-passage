package com.rockthejvm.reviewboard.repositories

import org.postgresql.ds.PGSimpleDataSource
import org.testcontainers.containers.PostgreSQLContainer
import zio.*

import javax.sql.DataSource
trait RepositorySpec {

  // test containers
  // spawn a Postgres instance on Docker just for the test
  private def createContainer(): PostgreSQLContainer[Nothing] = {
    val container: PostgreSQLContainer[Nothing] =
      PostgreSQLContainer("postgres")
        .withInitScript("sql/companies.sql")
    container.start()
    container
  }

  // create a datasource fot connect to the Postgres
  private def createDatasource(container: PostgreSQLContainer[Nothing]): DataSource = {
    val dataSource = new PGSimpleDataSource
    dataSource.setURL(container.getJdbcUrl)
    dataSource.setUser(container.getUsername)
    dataSource.setPassword(container.getPassword)
    dataSource
  }

  // use the datasource(as a ZLayer) to build the Quill instance (as a ZLayer)
  def dataSourceLayer: RLayer[Any with Scope, DataSource] = ZLayer {
    for {
      container <- ZIO.acquireRelease(ZIO.attempt(createContainer()))(container =>
        ZIO.attempt(container.stop()).ignoreLogged
      )
      dataSource <- ZIO.attempt(createDatasource(container))
    } yield dataSource
  }

}
