package io.github.Sammers21.cm.picker

interface DotabuffClient {
    suspend fun heroes(): List<String>

    suspend fun counters(hero: String): Map<String, CounterInfo>
}