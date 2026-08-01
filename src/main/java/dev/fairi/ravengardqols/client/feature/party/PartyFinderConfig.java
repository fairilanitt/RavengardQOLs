package dev.fairi.ravengardqols.client.feature.party;

import dev.fairi.ravengardqols.RavengardQolsCommon;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.minecraft.client.Minecraft;

public final class PartyFinderConfig {
    private static final String DEFAULT_RELAY = "http://127.0.0.1:8787";
    private static final String FILE_NAME = "ravengardqols-party-finder.properties";

    private PartyFinderConfig() {
    }

    public static URI relayUri() {
        Path path = Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve(FILE_NAME);
        Properties properties = new Properties();
        if (Files.isRegularFile(path)) {
            try (var input = Files.newInputStream(path)) {
                properties.load(input);
            } catch (IOException exception) {
                RavengardQolsCommon.LOGGER.warn("Unable to read Party Finder config", exception);
            }
        } else {
            createDefault(path);
        }

        URI uri = URI.create(properties.getProperty("relay_url", DEFAULT_RELAY).trim());
        validate(uri);
        return stripTrailingSlash(uri);
    }

    private static void createDefault(Path path) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(
                path,
                "# Ravengard QOL's Party Finder relay. Use HTTPS for remote servers.\nrelay_url=" + DEFAULT_RELAY + "\n",
                StandardCharsets.UTF_8
            );
        } catch (IOException exception) {
            RavengardQolsCommon.LOGGER.warn("Unable to create Party Finder config", exception);
        }
    }

    private static void validate(URI uri) {
        String scheme = uri.getScheme();
        if (uri.getHost() == null || scheme == null) {
            throw new IllegalArgumentException("Party Finder relay_url must be an absolute HTTP(S) URL");
        }
        boolean loopback = uri.getHost().equals("127.0.0.1")
            || uri.getHost().equals("::1");
        if (!scheme.equalsIgnoreCase("https") && !(scheme.equalsIgnoreCase("http") && loopback)) {
            throw new IllegalArgumentException("Remote Party Finder relays must use HTTPS");
        }
        if (uri.getUserInfo() != null || uri.getFragment() != null || uri.getQuery() != null) {
            throw new IllegalArgumentException("Party Finder relay_url cannot contain credentials, query, or fragment");
        }
    }

    private static URI stripTrailingSlash(URI uri) {
        String value = uri.toString();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return URI.create(value);
    }
}
