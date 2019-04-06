package io.github.Sammers21.cm.picker

/**
 * A client which parses dotabuff site.
 */
interface DotabuffClient {

    /**
     * Return a list of Dota 2 heroes.
     */
    fun heroes(): Set<Hero>

    /**
     * A map of counter heroes for a given one.
     */
    fun counters(hero: Hero): Map<Hero, CounterInfo>
}