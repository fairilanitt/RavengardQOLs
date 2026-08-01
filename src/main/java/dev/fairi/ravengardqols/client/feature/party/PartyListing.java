package dev.fairi.ravengardqols.client.feature.party;

import java.util.List;

public record PartyListing(
    String leaderUuid,
    String leaderName,
    int leaderLevel,
    int minimumLevel,
    int maximumLevel,
    List<String> members,
    long updatedAt
) {
    public PartyListing {
        members = List.copyOf(members == null ? List.of() : members);
    }

    public int size() {
        return members.size();
    }

    public boolean isFull() {
        return size() >= 3;
    }
}
