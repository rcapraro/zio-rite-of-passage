package com.rockthejvm.reviewboard.pages

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.*

object SignupPage {
  def apply(): ReactiveHtmlElement[HTMLDivElement] =
    div("Sign up")
}
