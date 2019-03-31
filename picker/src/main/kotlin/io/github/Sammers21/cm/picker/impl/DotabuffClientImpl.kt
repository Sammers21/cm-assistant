package io.github.Sammers21.cm.picker.impl

import io.github.Sammers21.cm.picker.CounterInfo
import io.github.Sammers21.cm.picker.DotabuffClient
import io.vertx.core.Vertx
import io.vertx.ext.web.client.WebClient
import io.vertx.kotlin.ext.web.client.sendAwait
import org.jsoup.Jsoup
import javax.swing.text.html.parser.Element

class DotabuffClientImpl : DotabuffClient {

    private val vertx: Vertx
    private val webClient: WebClient

    constructor(vertx: Vertx) {
        this.vertx = vertx
        this.webClient = WebClient.create(vertx)
    }

    override suspend fun heroes(): List<String> {
        val response = webClient.getAbs("https://www.dotabuff.com/heroes").sendAwait()
        val bodyAsString = response.bodyAsString()
        val parsedHtml = Jsoup.parse(bodyAsString)
        val heroes = parsedHtml.select("body > div.container-outer.seemsgood > div.container-inner.container-inner-content > div.content-inner > section:nth-child(3) > footer > div > a > div > div.name")
        return heroes.map { element -> element.text() }
    }

    override suspend fun counters(hero: String): Map<String, CounterInfo> {
        val url = String.format("https://www.dotabuff.com/heroes/%s/counters", hero.toLowerCase())
        val response = webClient.getAbs(url).sendAwait()
        val bodyAsString = response.bodyAsString()
        val parsed = Jsoup.parse(bodyAsString)
        val heroes = parsed.select("body > div.container-outer.seemsgood > div.container-inner.container-inner-content > div.content-inner > section:nth-child(4) > article > table > tbody > tr")

        return heroes.map { element ->
            element.child(1).children().text() to
                    CounterInfo(
                            element.child(2).attr("data-value").toDouble(),
                            element.child(3).attr("data-value").toDouble(),
                            element.child(4).attr("data-value").toLong())
        }.toMap()
    }
}