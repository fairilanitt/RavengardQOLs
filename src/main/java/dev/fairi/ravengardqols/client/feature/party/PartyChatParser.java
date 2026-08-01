package dev.fairi.ravengardqols.client.feature.party;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PartyChatParser {
    private static final Pattern USERNAME = Pattern.compile("(?<![A-Za-z0-9_])([A-Za-z0-9_]{1,16})(?![A-Za-z0-9_])");
    private static final Pattern FORMATTING = Pattern.compile("§[0-9A-FK-ORa-fk-or]");
    private static final long CAPTURE_TIMEOUT_MILLIS = 3_000L;

    private final Set<String> members = new LinkedHashSet<>();
    private boolean capturing;
    private long captureStartedAt;
    private String leader = "";

    public void beginCapture() {
        capturing = true;
        captureStartedAt = System.currentTimeMillis();
        members.clear();
        leader = "";
    }

    public PartyRoster accept(String message) {
        long now = System.currentTimeMillis();
        String clean = FORMATTING.matcher(message).replaceAll("").trim();

        if (clean.toLowerCase().contains("you are not currently in a party")) {
            reset();
            return new PartyRoster("", List.of(), false, now);
        }
        if (!capturing || now - captureStartedAt > CAPTURE_TIMEOUT_MILLIS) {
            return null;
        }

        String lower = clean.toLowerCase();
        if (lower.contains("party leader:")) {
            List<String> names = namesAfterColon(clean);
            if (!names.isEmpty()) {
                leader = names.getFirst();
                members.addAll(names);
            }
        } else if (lower.contains("party moderators:") || lower.contains("party members:")) {
            members.addAll(namesAfterColon(clean));
        }

        if ((clean.startsWith("---") || clean.startsWith("═══")) && !members.isEmpty() && now - captureStartedAt > 100L) {
            PartyRoster roster = snapshot(now);
            reset();
            return roster;
        }
        return null;
    }

    public PartyRoster finishIfExpired() {
        long now = System.currentTimeMillis();
        if (!capturing || now - captureStartedAt <= CAPTURE_TIMEOUT_MILLIS) {
            return null;
        }
        PartyRoster roster = members.isEmpty() ? PartyRoster.empty() : snapshot(now);
        reset();
        return roster;
    }

    private PartyRoster snapshot(long now) {
        List<String> limitedMembers = new ArrayList<>(members);
        return new PartyRoster(leader, limitedMembers.subList(0, Math.min(3, limitedMembers.size())), true, now);
    }

    private static List<String> namesAfterColon(String line) {
        int colon = line.indexOf(':');
        String content = colon >= 0 ? line.substring(colon + 1) : line;
        List<String> names = new ArrayList<>();
        Matcher matcher = USERNAME.matcher(content);
        while (matcher.find()) {
            String candidate = matcher.group(1);
            if (!isRankOrStatus(candidate)) {
                names.add(candidate);
            }
        }
        return names;
    }

    private static boolean isRankOrStatus(String value) {
        return value.equalsIgnoreCase("MVP")
            || value.equalsIgnoreCase("MVPPLUS")
            || value.equalsIgnoreCase("VIP")
            || value.equalsIgnoreCase("VIPPLUS")
            || value.equalsIgnoreCase("NONE")
            || value.equalsIgnoreCase("Offline");
    }

    private void reset() {
        capturing = false;
        members.clear();
        leader = "";
    }
}
