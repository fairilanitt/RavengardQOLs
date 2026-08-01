package dev.fairi.ravengardqols.client.feature.party;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.exceptions.AuthenticationException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import net.minecraft.client.Minecraft;

public final class PartyRelayClient {
    private static final int MAXIMUM_RESPONSE_BYTES = 262_144;
    private static final Gson GSON = new Gson();

    private final HttpClient http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(6))
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();
    private final URI baseUri;
    private volatile String bearerToken;
    private CompletableFuture<Void> authenticationInFlight;

    public PartyRelayClient(URI baseUri) {
        this.baseUri = baseUri;
    }

    public synchronized CompletableFuture<Void> authenticate() {
        if (bearerToken != null) {
            return CompletableFuture.completedFuture(null);
        }
        if (authenticationInFlight != null) {
            return authenticationInFlight;
        }

        Minecraft minecraft = Minecraft.getInstance();
        AuthChallengeRequest request = new AuthChallengeRequest(
            minecraft.getUser().getName(),
            minecraft.getUser().getProfileId().toString()
        );
        CompletableFuture<Void> authentication = send("POST", "/v1/auth/challenge", request, false)
            .thenApply(json -> GSON.fromJson(json, AuthChallenge.class))
            .thenCompose(challenge -> CompletableFuture
                .runAsync(() -> joinMojangSession(minecraft, challenge.serverId()))
                .thenCompose(ignored -> send("POST", "/v1/auth/complete", new AuthComplete(challenge.challengeId()), false)))
            .thenApply(json -> {
                AuthSession session = GSON.fromJson(json, AuthSession.class);
                if (session.token() == null || session.token().length() < 32) {
                    throw new CompletionException(new IOException("Relay returned an invalid session token"));
                }
                bearerToken = session.token();
                return null;
            });
        authenticationInFlight = authentication;
        authentication.whenComplete((ignored, failure) -> {
            synchronized (PartyRelayClient.this) {
                if (authenticationInFlight == authentication) {
                    authenticationInFlight = null;
                }
            }
        });
        return authentication;
    }

    public CompletableFuture<List<PartyListing>> listParties() {
        return authenticatedSend("GET", "/v1/parties", null)
            .thenApply(json -> List.copyOf(Arrays.asList(GSON.fromJson(json, PartyListing[].class))));
    }

    public CompletableFuture<Void> publishParty(int leaderLevel, int minimumLevel, int maximumLevel, List<String> members) {
        PublishParty body = new PublishParty(leaderLevel, minimumLevel, maximumLevel, members);
        return authenticatedSend("PUT", "/v1/party", body).thenApply(ignored -> null);
    }

    public CompletableFuture<Void> removeParty() {
        return authenticatedSend("DELETE", "/v1/party", null).thenApply(ignored -> null);
    }

    public CompletableFuture<Void> requestJoin(String leaderUuid, int requesterLevel) {
        requireUuid(leaderUuid);
        return authenticatedSend(
            "POST",
            "/v1/parties/" + leaderUuid + "/requests",
            new JoinParty(requesterLevel)
        ).thenApply(ignored -> null);
    }

    public CompletableFuture<List<PartyJoinRequest>> getRequests() {
        return authenticatedSend("GET", "/v1/requests", null)
            .thenApply(json -> List.copyOf(Arrays.asList(GSON.fromJson(json, PartyJoinRequest[].class))));
    }

    public CompletableFuture<Void> decideRequest(String requestId, boolean accepted) {
        requireUuid(requestId);
        return authenticatedSend(
            "POST",
            "/v1/requests/" + requestId + "/decision",
            new RequestDecision(accepted ? "accepted" : "declined")
        ).thenApply(ignored -> null);
    }

    public synchronized void invalidateSession() {
        bearerToken = null;
    }

    private CompletableFuture<String> authenticatedSend(String method, String path, Object body) {
        return authenticate().thenCompose(ignored -> send(method, path, body, true)).handle((result, failure) -> {
            if (failure == null) {
                return CompletableFuture.completedFuture(result);
            }
            Throwable cause = unwrap(failure);
            if (cause instanceof RelayException relayException && relayException.statusCode == 401) {
                bearerToken = null;
            }
            return CompletableFuture.<String>failedFuture(cause);
        }).thenCompose(future -> future);
    }

    private CompletableFuture<String> send(String method, String path, Object body, boolean authenticated) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(baseUri.resolve(path))
            .timeout(Duration.ofSeconds(10))
            .header("Accept", "application/json")
            .header("User-Agent", "RavengardQOLs/0.1");
        if (authenticated) {
            String token = bearerToken;
            if (token == null) {
                return CompletableFuture.failedFuture(new IOException("Not authenticated with Party Finder relay"));
            }
            builder.header("Authorization", "Bearer " + token);
        }

        if (body == null) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            builder.header("Content-Type", "application/json");
            builder.method(method, HttpRequest.BodyPublishers.ofString(GSON.toJson(body)));
        }

        return http.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofByteArray()).thenApply(response -> {
            byte[] bytes = response.body();
            if (bytes.length > MAXIMUM_RESPONSE_BYTES) {
                throw new CompletionException(new IOException("Party Finder relay response was too large"));
            }
            String responseBody = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new CompletionException(new RelayException(response.statusCode(), errorMessage(responseBody)));
            }
            return responseBody;
        });
    }

    private static void joinMojangSession(Minecraft minecraft, String serverId) {
        if (serverId == null || !serverId.matches("[a-f0-9]{64}")) {
            throw new CompletionException(new IOException("Relay returned an invalid authentication challenge"));
        }
        try {
            minecraft.services().sessionService().joinServer(
                minecraft.getUser().getProfileId(),
                minecraft.getUser().getAccessToken(),
                serverId
            );
        } catch (AuthenticationException exception) {
            throw new CompletionException(new IOException("Minecraft session authentication failed", exception));
        }
    }

    private static String errorMessage(String body) {
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            if (json.has("error")) {
                return json.get("error").getAsString();
            }
        } catch (RuntimeException ignored) {
        }
        return "Party Finder relay request failed";
    }

    private static void requireUuid(String value) {
        if (value == null || !value.matches("[0-9a-fA-F-]{32,36}")) {
            throw new IllegalArgumentException("Invalid Party Finder identifier");
        }
    }

    private static Throwable unwrap(Throwable failure) {
        Throwable current = failure;
        while ((current instanceof CompletionException || current instanceof java.util.concurrent.ExecutionException)
            && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private record AuthChallengeRequest(String username, String uuid) {
    }

    private record AuthChallenge(String challengeId, String serverId, long expiresAt) {
    }

    private record AuthComplete(String challengeId) {
    }

    private record AuthSession(String token, long expiresAt) {
    }

    private record PublishParty(int leaderLevel, int minimumLevel, int maximumLevel, List<String> members) {
    }

    private record JoinParty(int requesterLevel) {
    }

    private record RequestDecision(String decision) {
    }

    public static final class RelayException extends IOException {
        private final int statusCode;

        RelayException(int statusCode, String message) {
            super(message);
            this.statusCode = statusCode;
        }
    }
}
