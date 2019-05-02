package io.github.Sammers21.cm.picker

import io.github.Sammers21.cm.picker.impl.DotabuffClientImpl
import io.vertx.core.Vertx

fun main() {
    val vertx = Vertx.vertx()
    val dotabuffClientImpl = DotabuffClientImpl(vertx)
    val heroes = dotabuffClientImpl.heroes()
    heroes.heroes.forEach({ hero ->
        println("Counters for " + hero)
        dotabuffClientImpl.counters(hero).forEach { (key, value) ->
            println(String.format("Hero: '%s', Dis rate: '%s', Win rate: '%s', Matches: '%s'", key.originalHeroName, value.disadvantage.toString(), value.winRate.toString(), value.matchesPlayed.toString()))
        }
    })
    vertx.close()
}