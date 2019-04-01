package io.github.Sammers21.cm.picker.discord;

import io.github.Sammers21.cm.picker.DotabuffClient;
import io.github.Sammers21.cm.picker.impl.DotabuffClientImpl;
import io.vertx.core.Vertx;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.message.Message;

import java.util.*;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) {
        String token = args[0];
        DiscordApi api = new DiscordApiBuilder().setToken(token).login().join();
        DotabuffClient dotabuffClient = new DotabuffClientImpl(Vertx.vertx());
        final Set<String> heroes = dotabuffClient.heroes();

        api.addMessageCreateListener(event -> {
            try {
                final Message message = event.getMessage();
                final String content = message.getContent();
                if (content.equalsIgnoreCase("!ping")) {
                    event.getChannel().sendMessage("Pong!");
                }
                if (content.startsWith("!vs")) {
                    final String[] argsList = content.substring(3).strip().split(" ");
                    final Set<String> inputHeroes = Arrays.stream(argsList).map(String::toLowerCase).collect(Collectors.toSet());
                    final Set<String> nonValidHeroes = inputHeroes.stream().filter(inputHero -> !heroes.contains(inputHero)).collect(Collectors.toSet());
                    if (!nonValidHeroes.isEmpty()) {
                        nonValidHeroes.forEach(nonValidHero -> event.getChannel().sendMessage(String.format("Героя '%s' в доте не существует", nonValidHero)));
                        return;
                    }
                    final Set<String> heroesToCalculateFor = new HashSet<>(heroes);
                    heroesToCalculateFor.removeAll(inputHeroes);
                    Map<String, Double> scores = heroesToCalculateFor.stream().collect(Collectors.toMap(o -> o, o -> 0d));
                    inputHeroes.stream().map(hero ->
                    {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        return dotabuffClient.counters(hero);
                    }).forEach(stringCounterInfoMap -> {
                        heroesToCalculateFor.forEach(hero -> {
                            final double disadvantage = stringCounterInfoMap.get(hero).getDisadvantage();
                            scores.computeIfPresent(hero, (s, aDouble) -> disadvantage + aDouble);
                        });
                    });

                    event.getChannel().sendMessage("Полная контра:");
                    scores.entrySet()
                            .stream()
                            .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                            .limit(5)
                            .forEach(entry -> {
                                event.getChannel().sendMessage(String.format("Герой: '%s', Очков контры: '%.2f'\n", entry.getKey(), entry.getValue()));
                            });
                }
            } catch (Throwable t) {
                System.out.println("Error occured: ");
                t.printStackTrace();
            }
        });
        System.out.println("You can invite the bot by using the following url: " + api.createBotInvite());
    }
}
