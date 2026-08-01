package dev.fairi.ravengardqols.client.feature.playerlist;

import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PlayerLevelParser {
    private static final Pattern TRAILING_LEVEL = Pattern.compile("^(.*?)\\s+([0-9]{1,4})\\s*\\D*$");
    private static final Pattern LEGACY_FORMATTING = Pattern.compile("§[0-9A-FK-ORa-fk-or]");

    private PlayerLevelParser() {
    }

    public static ParsedPlayer parse(String displayName) {
        String cleanName = LEGACY_FORMATTING.matcher(displayName).replaceAll("").trim();
        Matcher matcher = TRAILING_LEVEL.matcher(cleanName);
        if (!matcher.matches()) {
            return new ParsedPlayer(cleanName, OptionalInt.empty());
        }

        try {
            int level = Integer.parseInt(matcher.group(2));
            return new ParsedPlayer(matcher.group(1).trim(), OptionalInt.of(level));
        } catch (NumberFormatException ignored) {
            return new ParsedPlayer(cleanName, OptionalInt.empty());
        }
    }

    public record ParsedPlayer(String name, OptionalInt level) {
    }
}
