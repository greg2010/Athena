package org.kys.athena.http

import org.kys.athena.riot.api.dto.ddragon.champions.{ChampionEntry, Champions}
import org.kys.athena.riot.api.dto.ddragon.runes.{Rune, RuneTree, RunesEntry}
import org.kys.athena.riot.api.dto.ddragon.summonerspells.{SummonerSpells, SummonerSpellsEntry}
import org.kys.athena.util.Config


case class DData(c: Champions, r: List[RuneTree], s: SummonerSpells) {
  private lazy val flatRunes: List[RunesEntry] = r.flatMap(t => t.slots.flatMap(_.runes))

  def championById(id: Long): Option[ChampionEntry] = c.data.values.find(_.key == id)

  def summonerById(id: Int): Option[SummonerSpellsEntry] = s.data.values.find(_.key == id)

  def treeById(id: Long): Option[RuneTree] = r.find(_.id == id)

  def keystoneById(id: Long): Option[RunesEntry] = flatRunes.find(_.id == id)

  def championUrl(champion: Option[ChampionEntry]): String = {
    champion.map { ch =>
      s"${Config.DDRAGON_BASE_URL}${Config.DDRAGON_VERSION}/img/champion/${ch.id}.png"
    }.getOrElse("/placeholder_champion.png")
  }

  def summonerUrl(summonerSpellsEntry: SummonerSpellsEntry): String = {
    s"${Config.DDRAGON_BASE_URL}${Config.DDRAGON_VERSION}/img/spell/${summonerSpellsEntry.id}.png"
  }

  def summonerUrlById(id: Int): Option[String] = summonerById(id).map(summonerUrl)

  def runeUrl(rune: Rune) = s"${Config.DDRAGON_BASE_URL}img/${rune.icon}"

}