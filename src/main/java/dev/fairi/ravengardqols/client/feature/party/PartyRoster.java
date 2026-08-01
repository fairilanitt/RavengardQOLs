package dev.fairi.ravengardqols.client.feature.party;

import java.util.List;

public record PartyRoster(String leader, List<String> members, boolean inParty, long updatedAtMillis) {
    public static PartyRoster empty() {
        return new PartyRoster("", List.of(), false, System.currentTimeMillis());
    }

    public PartyRoster {
        leader = leader == null ? "" : leader;
        members = List.copyOf(members == null ? List.of() : members);
    }
}
