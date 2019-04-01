package io.github.Sammers21.cm.picker.discord;

import io.github.Sammers21.cm.picker.CounterInfo;
import io.github.Sammers21.cm.picker.DotabuffClient;
import io.github.Sammers21.cm.picker.impl.DotabuffClientImpl;
import io.vertx.core.Vertx;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class Main {

    private final static Logger log = LoggerFactory.getLogger(Main.class);


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
                } else if (content.startsWith("!vs")) {
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
                    inputHeroes.stream()
                            .map(s -> s.replace("_", "-"))
                            .map(hero ->
                            {
                                log.info("Requesting counters for '{}'", hero);
                                final Map<String, CounterInfo> counters = dotabuffClient.counters(hero);
                                if (counters.size() < 100) {
                                    log.warn("Returned {} counter heroes for '{}'", counters.size(), hero);
                                }
                                return counters;
                            }).forEach(stringCounterInfoMap -> {
                        heroesToCalculateFor.forEach(hero -> {
                            final CounterInfo counterInfo = stringCounterInfoMap.get(hero);
                            if (counterInfo == null) {
                                throw new IllegalStateException(String.format("no counter info for hero '%s'", hero));
                            } else {
                                final double disadvantage = counterInfo.getDisadvantage();
                                scores.computeIfPresent(hero, (s, aDouble) -> disadvantage + aDouble);
                            }
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
                } else if (content.equals("!clear")) {
                    final CompletableFuture<MessageSet> messagesFuture = event.getChannel().getMessages(Integer.MAX_VALUE);
                    messagesFuture.thenAccept(messages -> {
                        final List<Message> toDelete = messages.stream()
                                .filter(msg -> {
                                    return msg.getAuthor().getId() == api.getYourself().getId() || msg.getContent().startsWith("!");
                                })
                                .filter(msg -> System.currentTimeMillis() - msg.getCreationTimestamp().toEpochMilli() < 7 * 2 * 24 * 60 * 60 * 1000)
                                .collect(Collectors.toList());
                        log.info("Collected {} messages to delete", toDelete.size());
                        toDelete.forEach(msg -> {
                            event.getChannel().deleteMessages(msg).handle((aVoid, throwable) -> {
                                if (throwable != null) {
                                    log.error("Message deletion error:", throwable);
                                }
                                return null;
                            });
                        });
                    });
                }
            } catch (Throwable t) {
                log.error("Error occurred: ", t);
            }
        });
        log.info("You can invite the bot by using the following url: " + api.createBotInvite());
    }
}
