package dev.fairi.ravengardqols.client.feature.party;

public record PartyJoinRequest(String id, String requesterUuid, String requesterName, int requesterLevel, long createdAt) {
}
