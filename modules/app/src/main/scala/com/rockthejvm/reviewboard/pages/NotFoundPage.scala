package com.rockthejvm.reviewboard.pages

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.*

object NotFoundPage {
  def apply(): ReactiveHtmlElement[HTMLDivElement] =
    div("404 (invalid page) - you lost friend? ")
}
