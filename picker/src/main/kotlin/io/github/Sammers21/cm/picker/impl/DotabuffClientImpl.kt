package io.github.Sammers21.cm.picker.impl

import io.github.Sammers21.cm.picker.CounterInfo
import io.github.Sammers21.cm.picker.Dota2Heroes
import io.github.Sammers21.cm.picker.DotabuffClient
import io.github.Sammers21.cm.picker.Hero
import io.vertx.core.Vertx
import io.vertx.ext.web.client.WebClient
import io.vertx.kotlin.ext.web.client.sendAwait
import kotlinx.coroutines.runBlocking
import org.jsoup.Jsoup

class DotabuffClientImpl : DotabuffClient {

    private val vertx: Vertx
    private val webClient: WebClient

    constructor(vertx: Vertx) {
        this.vertx = vertx
        this.webClient = WebClient.create(vertx)
    }

    override fun heroes(): Dota2Heroes {
        return runBlocking {
            val response = webClient.getAbs("https://www.dotabuff.com/heroes").sendAwait()
            val bodyAsString = response.bodyAsString()
            val parsedHtml = Jsoup.parse(bodyAsString)
            val heroes = parsedHtml.select("body > div.container-outer.seemsgood > div.container-inner.container-inner-content > div.content-inner > section:nth-child(3) > footer > div > a")
                    .map { it.attr("href").substring(8) }
                    .map { Hero(it) }
                    .toSet()
            Dota2Heroes(heroes)
        }
    }

    override fun counters(hero: Hero): Map<Hero, CounterInfo> {
        return runBlocking {
            val url = String.format("https://www.dotabuff.com/heroes/%s/counters?date=week", hero.originalHeroName)
            val response = webClient.getAbs(url).sendAwait()
            val bodyAsString = response.bodyAsString()
            val parsed = Jsoup.parse(bodyAsString)
            val heroes = parsed.select("body > div.container-outer.seemsgood > div.container-inner.container-inner-content > div.content-inner > section:nth-child(4) > article > table > tbody > tr")
            heroes.map { element ->
                Hero(element.attr("data-link-to").substring(8)) to
                        CounterInfo(
                                element.child(2).attr("data-value").toDouble(),
                                element.child(3).attr("data-value").toDouble(),
                                element.child(4).attr("data-value").toLong())
            }.toMap()
        }
    }
}