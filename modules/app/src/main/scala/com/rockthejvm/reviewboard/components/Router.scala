package com.rockthejvm.reviewboard.components

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom
import frontroute.*
import org.scalajs.dom.HTMLElement
import com.rockthejvm.reviewboard.pages.*

object Router {

  def apply(): ReactiveHtmlElement[HTMLElement] =
    mainTag(
      routes(
        div(
          cls := "container-fluid",
          // potential children
          (pathEnd | path("companies")) { // http://localhost:1234/ or http://localhost:1234
            CompaniesPage()
          },
          path("login") {
            LoginPage()
          },
          path("signup") {
            SignupPage()
          },
          noneMatched {
            NotFoundPage()
          }
        )
      )
    )
}
