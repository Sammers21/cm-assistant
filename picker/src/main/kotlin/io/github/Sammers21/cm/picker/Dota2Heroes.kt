package io.github.Sammers21.cm.picker

import java.util.concurrent.ConcurrentHashMap

typealias HeroAlias = Pair<String, String>

class Dota2Heroes(heroes: Set<Hero>, aliases: List<HeroAlias> = Aliases.DefaultAliases) {

    var internalLookUpMap: Map<String, Hero> = ConcurrentHashMap()

    init {
        heroes.forEach { hero: Hero ->
            internalLookUpMap.plus(hero.originalHeroName to hero)
        }
        aliases.forEach { alias: HeroAlias ->
            val hero = internalLookUpMap.get(alias.second)
            if (hero == null) {
                throw IllegalStateException(
                        String.format("%s is not a valid alias for %s, because %s is an invalid hero name", alias.first, alias.second, alias.first)
                )
            } else {
                internalLookUpMap.plus(alias.first to hero)
            }
        }
    }

    fun lookUpHero(name: String): Hero? {
        return internalLookUpMap.get(name);
    }
}