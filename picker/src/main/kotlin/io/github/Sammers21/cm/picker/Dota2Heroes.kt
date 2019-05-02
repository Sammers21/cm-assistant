package io.github.Sammers21.cm.picker

import java.util.concurrent.ConcurrentHashMap

typealias HeroAlias = Pair<List<String>, String>

data class Dota2Heroes(val heroes: Set<Hero>, val aliases: List<HeroAlias> = Aliases.DefaultAliases) {

    private var internalLookUpMap: MutableMap<String, Hero> = ConcurrentHashMap()

    init {
        heroes.forEach { hero: Hero ->
            internalLookUpMap.put(hero.originalHeroName, hero)
        }
        aliases.forEach { alias: HeroAlias ->
            val hero = internalLookUpMap.get(alias.second)
            if (hero == null) {
                throw IllegalStateException(
                        String.format("'%s' is not a valid alias for '%s', because '%s' is an invalid hero name", alias.first, alias.second, alias.second)
                )
            } else {
                alias.first.forEach { aliasToAdd: String ->
                    val aHero = internalLookUpMap.get(aliasToAdd)
                    if (aHero == null) {
                        internalLookUpMap.put(aliasToAdd, hero)
                    } else {
                        throw IllegalStateException(
                                String.format(
                                        "Cant create duplicate alias '%s' -> '%s', because alias '%s' -> '%s' already exists'",
                                        aliasToAdd, hero.originalHeroName, aliasToAdd, aHero.originalHeroName
                                )
                        )
                    }
                }
            }
        }
    }

    fun lookUpHero(name: String): Hero {
        return internalLookUpMap.getValue(name)
    }

    fun heroExists(name: String): Boolean {
        return internalLookUpMap.containsKey(name)
    }

    fun heroExists(inputHero: Hero): Boolean {
        return heroExists(inputHero.originalHeroName)
    }
}