package com.rockthejvm.reviewboard

import com.raquo.airstream.ownership.OneTimeOwner
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.rockthejvm.reviewboard.components.*
import frontroute.LinkHandler
import org.scalajs.dom
import org.scalajs.dom.HTMLDivElement

import scala.util.Try

object App {

  val app: ReactiveHtmlElement[HTMLDivElement] = div(
    Header(),
    Router()
  ).amend(LinkHandler.bind) // for internal links

  def main(args: Array[String]): Unit = {
    val containerNode = dom.document.querySelector("#app")
    render(containerNode, app)
  }
}

// reactive variables
object Tutorial {
  val staticContent: ReactiveHtmlElement[HTMLDivElement] = div(
    // modifiers
    // CSS class, styles, onClick, etc
    styleAttr := "color:red",
    p("This is an app"),
    p("rock the JVM and also JS")
  )

  // EventStream - produce values of the same type
  private val ticks = EventStream.periodic(1000) // EventStream[Int]
  // Subscription - Airstream
  // Owner
  private val subscription = ticks.addObserver(new Observer[Int] {
    override def onError(err: Throwable): Unit    = ()
    override def onTry(nextValue: Try[Int]): Unit = ()
    override def onNext(nextValue: Int): Unit     = dom.console.log(s"ticks: $nextValue")
  })(new OneTimeOwner(() => ()))

  scala.scalajs.js.timers.setTimeout(10_000)(subscription.kill())

  val timeUpdated: ReactiveHtmlElement[HTMLDivElement] =
    div(
      span("Time since loaded: "),
      child <-- ticks.map(number => s"$number seconds")
    )

  // Eventbus - like EventStream, but you can push new elements to the stream
  private val clickEvents: EventBus[Int] = EventBus[Int]()
  val clickUpdated: ReactiveHtmlElement[HTMLDivElement] = div(
    span("Clicks since loaded: "),
    child <-- clickEvents.events.scanLeft(0)(_ + _).map(number => s"$number clicks"),
    button(
      `type`    := "button",
      styleAttr := "display: block",
      onClick.map(_ => 1) --> clickEvents,
      "Add a click"
    )
  )

  // Signal - similar to EventStream, but they have a "current value" (state)
  // can be inspected for the current state (if Laminar/Airstream knows that he has an owner)
  private val countSignal: OwnedSignal[Int] = clickEvents.events.scanLeft(0)(_ + _).observe(new OneTimeOwner(() => ()))
  private val queryEvents: EventBus[Unit]   = EventBus[Unit]()

  val clicksQueried: ReactiveHtmlElement[HTMLDivElement] = div(
    span("Clicks since loaded: "),
    child <-- queryEvents.events.map(_ => countSignal.now()), // countSignal.now() : current state
    button(
      `type`    := "button",
      styleAttr := "display: block",
      onClick.map(_ => 1) --> clickEvents,
      "Add a click"
    ),
    button(
      `type`    := "button",
      styleAttr := "display: block",
      onClick.mapTo(()) --> queryEvents,
      "Refresh count"
    )
  )

  // Var - reactive variable
  private val countVar = Var[Int](0)
  val clicksVar: ReactiveHtmlElement[HTMLDivElement] = div(
    span("Clicks so far: "),
    child <-- countVar.signal.map(_.toString),
    button(
      `type`    := "button",
      styleAttr := "display: block",
      // onClick --> countVar.updater((current, event) => current + 1),
      // onClick --> countVar.writer.contramap(event => countVar.now() + 1),
      onClick --> (_ => countVar.set(countVar.now() + 1)),
      "Add a click"
    )
  )

  /** no state | with state ------------------------------------ read EventStream | Signal
    * ------------------------------------ write EventBus | Var
    */

}
