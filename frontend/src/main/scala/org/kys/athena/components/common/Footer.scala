package org.kys.athena.components.common


import com.raquo.laminar.api.L._


object Footer {

  private val disclaimer: String =
    "Athena isn't endorsed by Riot Games and doesn't reflect the views or opinions of Riot Games or " +
    "anyone officially involved in producing or managing Riot Games properties. Riot Games, and all associated " +
    "properties are trademarks or registered trademarks of Riot Games, Inc."

  def apply(): HtmlElement = {
    footer(cls := "max-w-md mb-2", p(cls := "text-center text-xs text-gray-800 ", disclaimer))
  }
}
