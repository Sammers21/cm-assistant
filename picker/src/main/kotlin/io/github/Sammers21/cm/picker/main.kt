package io.github.Sammers21.cm.picker

import io.github.Sammers21.cm.picker.impl.DotabuffClientImpl
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.*

fun main() {
    runBlocking<Unit> {
        val vertx = Vertx.vertx()
        val dotabuffClientImpl = DotabuffClientImpl(vertx)
        launch(vertx.dispatcher()) {
            for (hero in dotabuffClientImpl.heroes()) {
                println(hero)
            }
        }
        dotabuffClientImpl.heroes();
    }
}