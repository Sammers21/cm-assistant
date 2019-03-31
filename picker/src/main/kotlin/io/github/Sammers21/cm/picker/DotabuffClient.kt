package io.github.Sammers21.cm.picker

interface DotabuffClient {
    fun heroes(): Set<String>

    fun counters(hero: String): Map<String, CounterInfo>
}