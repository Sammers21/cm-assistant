package io.github.Sammers21.cm.picker

import io.github.Sammers21.cm.picker.impl.DotabuffClientImpl
import io.vertx.core.Vertx
import org.junit.Assert
import org.junit.Test

class AliasesAreCorrect {

    @Test
    fun heroListWorks() {
        val client = DotabuffClientImpl(Vertx.vertx())
        val heroes = client.heroes()
        Assert.assertTrue(heroes.heroExists("io"))
    }
}