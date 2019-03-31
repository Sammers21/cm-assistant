package io.github.Sammers21.cm.picker

import io.github.Sammers21.cm.picker.impl.DotabuffClientImpl
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.*

fun main() {
    val vertx = Vertx.vertx()
    val dotabuffClientImpl = DotabuffClientImpl(vertx)
    runBlocking<Unit> {
        launch(vertx.dispatcher()) {
            val heroes = dotabuffClientImpl.heroes()
            heroes.forEach({ hero: String ->
                println("Counters for " + hero)
                dotabuffClientImpl.counters(hero).forEach { (key, value) ->
                    println(String.format("Hero: '%s', Dis rate: '%s', Win rate: '%s', Matches: '%s'", key, value.disadvantage.toString(), value.winRate.toString(), value.matchesPlayed.toString()))
                }
            })
        }
    }
    vertx.close()
}