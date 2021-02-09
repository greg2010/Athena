package org.kys.athena.components

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.{Event, html}

object DropdownMenu {
  def apply[T](titleSignal: Signal[String],
               dropdownElements: List[T],
               dropdownObserver: Observer[T],
               dropdownButtonCls: Option[String],
               dropdownContainerCls: Option[String],
               dropdownElemCls: Option[String],
               mods: Modifier[HtmlElement]*): ReactiveHtmlElement[html.Div] = {

    val isHidden: Var[Boolean] = Var(true)
    val hiddenObs              = isHidden.writer.contramap[Event](_ => !isHidden.now())

    div(
      button(
        cls := s"${dropdownButtonCls.fold("")(identity)}",
        `type` := "button",
        onClick.preventDefault --> hiddenObs,
        child.text <-- titleSignal),
      div(
        cls := s"py-1 absolute z-1 ${dropdownContainerCls.fold("")(identity)}",
        display <-- isHidden.signal.map {
          case true => "none"
          case false => "block"
        },
        dropdownElements.map { elem =>
          button(elem.toString,
                 `type` := "button",
                 onClick.preventDefault.mapTo(elem) --> dropdownObserver,
                 onClick.preventDefault --> hiddenObs,
                 cls := s"block ${dropdownElemCls.fold("")(identity)}")
        }), mods)
  }

}
