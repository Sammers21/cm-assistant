package io.github.Sammers21.cm.picker.discord;

import com.google.common.collect.Lists;
import io.github.Sammers21.cm.picker.CounterInfo;
import io.github.Sammers21.cm.picker.Dota2Heroes;
import io.github.Sammers21.cm.picker.DotabuffClient;
import io.github.Sammers21.cm.picker.Hero;
import io.github.Sammers21.cm.picker.impl.DotabuffClientImpl;
import io.vertx.core.Vertx;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageSet;
import org.javacord.api.event.message.MessageCreateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class Main {

    private final static Logger log = LoggerFactory.getLogger(Main.class);


    public static void main(String[] args) {
        if (args.length != 1) {
            throw new IllegalStateException("Invalid amount of arguments." +
                    " Token should be passed as a single argument");
        }

        String token = args[0];
        DiscordApi api = new DiscordApiBuilder().setToken(token).login().join();
        DotabuffClient dotabuffClient = new DotabuffClientImpl(Vertx.vertx());
        Dota2Heroes heroes = dotabuffClient.heroes();
        api.addMessageCreateListener(event -> {
            try {
                final Message message = event.getMessage();
                final String content = message.getContent();
                if (content.equalsIgnoreCase("!ping")) {
                    ping(event);
                } else if (content.startsWith("!vs")) {
                    vs(dotabuffClient, heroes, event, content);
                } else if (content.equals("!clear") || content.equals("!clean")) {
                    clean(api, event);
                } else if (content.equals("!heroes")) {
                    heroesList(heroes, event);
                } else if (content.startsWith("!e") || content.startsWith("!exist") || content.startsWith("!ex") || content.startsWith("!exists")) {
                    exists(heroes, event);
                }
            } catch (Throwable t) {
                log.error("Error occurred: ", t);
            }
        });

        log.info("You can invite the bot by using the following url: " + api.createBotInvite());
    }

    private static void exists(Dota2Heroes heroes, MessageCreateEvent event) {
        final String[] split = event.getMessageContent().split(" ");
        if (split.length != 2) {
            log.error("invalid 'exists' command usage: {} ", event.getMessageContent());
            event.getChannel().sendMessage("Invalid command usage. Correct one: `!exists io`");
            return;
        }

        final String alias = split[1];
        if (heroes.heroExists(alias)) {
            event.getChannel().sendMessage(String.format("Sure, the original hero name: %s", heroes.lookUpHero(alias).getOriginalHeroName()));
        } else {
            event.getChannel().sendMessage("The hero does not exits");
        }
    }

    private static void heroesList(Dota2Heroes heroes, MessageCreateEvent event) {
        heroes.getHeroes().forEach(hero -> {
            event.getChannel().sendMessage(hero.getOriginalHeroName());
        });

    }

    private static void ping(MessageCreateEvent event) {
        event.getChannel().sendMessage("Pong!");
    }

    private static void vs(DotabuffClient dotabuffClient, Dota2Heroes heroes, MessageCreateEvent event, String content) {
        final String[] argsList = content.substring(3).strip().split(" ");
        final Set<String> inputList = Arrays.stream(argsList).map(String::toLowerCase).collect(Collectors.toSet());
        final Set<String> nonValidHeroes = inputList.stream().filter(inputHero -> !heroes.heroExists(inputHero)).collect(Collectors.toSet());
        if (!nonValidHeroes.isEmpty()) {
            nonValidHeroes.forEach(nonValidHero -> event.getChannel().sendMessage(String.format("Героя '%s' в доте не существует", nonValidHero)));
        } else {
            final Set<Hero> inputHeroList = inputList.stream().map(heroes::lookUpHero).collect(Collectors.toSet());
            final Set<Hero> heroesToCalculateFor = new HashSet<>(heroes.getHeroes());
            heroesToCalculateFor.removeAll(inputHeroList);
            final Map<Hero, Double> scores = heroesToCalculateFor.stream().collect(Collectors.toMap(o -> o, o -> 0d));
            inputHeroList.stream()
                    .map(hero ->
                    {
                        log.info("Requesting counters for '{}'", hero);
                        final Map<Hero, CounterInfo> counters = dotabuffClient.counters(hero);
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
                    .limit(16)
                    .forEach(entry -> {
                        event.getChannel().sendMessage(String.format("Герой: '%s', Очков контры: '%.2f'\n", entry.getKey().getOriginalHeroName(), entry.getValue()));
                    });
        }
    }

    private static void clean(DiscordApi api, MessageCreateEvent event) {
        event.getChannel().sendMessage("Зачищаем чатик...");
        event.getChannel().sendMessage("Олды тут?");
        final CompletableFuture<MessageSet> messagesFuture = event.getChannel().getMessages(Integer.MAX_VALUE);
        messagesFuture.thenAccept(messages -> {
            final Set<Message> allMessagesToDelete = messages.stream()
                    .filter(msg -> msg.getAuthor().getId() == api.getYourself().getId() || msg.getContent().startsWith("!"))
                    .collect(Collectors.toSet());
            final Set<Message> youngMessages = allMessagesToDelete.stream()
                    .filter(msg -> System.currentTimeMillis() - msg.getCreationTimestamp().toEpochMilli() < 7 * 2 * 24 * 60 * 60 * 1000)
                    .collect(Collectors.toSet());
            final Set<Message> oldMessages = new HashSet<>(allMessagesToDelete);
            oldMessages.removeAll(youngMessages);

            log.info("Collected {} messages to delete", allMessagesToDelete.size());
            final List<List<Message>> partitions = Lists.partition(new ArrayList<>(youngMessages), 99);
            partitions.forEach(partition -> {
                event.getChannel().deleteMessages(partition).handle((aVoid, throwable) -> {
                    if (throwable != null) {
                        log.error("Young message deletion error:", throwable);
                    }
                    return null;
                });
            });
            oldMessages.forEach(oldMessage -> {
                event.getChannel().deleteMessages(oldMessage).handle((aVoid, throwable) -> {
                    if (throwable != null) {
                        log.error("Old message deletion error:", throwable);
                    }
                    return null;
                });
            });
        });
    }
}
