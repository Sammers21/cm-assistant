package io.github.Sammers21.cm.picker.impl

import io.github.Sammers21.cm.picker.CounterInfo
import io.github.Sammers21.cm.picker.DotabuffClient
import io.vertx.core.Vertx
import io.vertx.ext.web.client.WebClient
import io.vertx.kotlin.ext.web.client.sendAwait

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
        println(bodyAsString)
        return listOf<String>("oracle")
    }

    override suspend fun counters(hero: String): Map<String, CounterInfo> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}