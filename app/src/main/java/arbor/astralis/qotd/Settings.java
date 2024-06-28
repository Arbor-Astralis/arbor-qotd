package arbor.astralis.qotd;

import arbor.astralis.qotd.persistence.GuildQuestionStore;
import arbor.astralis.qotd.persistence.GuildSettingsDocument;
import arbor.astralis.qotd.persistence.SqliteGuildQuestionStore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Scanner;

public final class Settings {
    
    private static final String BOT_SETTINGS_FOLDER_NAME = "bot-data";
    private static final String BOT_TOKEN_FILE_NAME = "bot-token";
    
    private Settings() {
        
    }
    
    public static Optional<String> initialize() throws IOException {
        resolveDataFolder();
        
        return loadBotToken();
    }

    private static void resolveDataFolder() throws IOException {
        Path settingsFolder = getDataFolder();
        if (!Files.isDirectory(settingsFolder)) {
            Files.createDirectories(settingsFolder);
        }
    }

    private static Optional<String> loadBotToken() throws IOException {
        Path tokenFile = getDataFolder().resolve(BOT_TOKEN_FILE_NAME);
        
        if (!Files.exists(tokenFile)) {
            Files.createFile(tokenFile);
            return Optional.empty();
        }
        
        String token = "";
        
        try (Scanner scanner = new Scanner(Files.newBufferedReader(tokenFile))) {
            if (scanner.hasNextLine()) {
                token = scanner.nextLine().replace("\n", "");
            }
        }
        
        if (token.isBlank()) {
            return Optional.empty();
        }
        
        return Optional.of(token);
    }
    
    private static Path getDataFolder() {
        return Paths.get(System.getProperty("user.dir"), BOT_SETTINGS_FOLDER_NAME);
    }
    
    public static GuildSettings forGuild(long guildId) {
        Path guildDataDirectory = resolveGuildDataDirectory(guildId);
        
        Path settingsFile = getGuildSettingsFile(guildId);
        var persistedSettings = new GuildSettingsDocument();
        
        if (Files.exists(settingsFile)) {
            var mapper = new ObjectMapper();
            try {
                persistedSettings = mapper.readValue(settingsFile.toFile(), GuildSettingsDocument.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        
        Path qotdStore = guildDataDirectory.resolve("qotd.db");
        GuildQuestionStore persistentStore = new SqliteGuildQuestionStore(qotdStore);
        
        var settings = new GuildSettings(guildId, persistentStore);
        settings.setQuestionModChannel(persistedSettings.getModChannelId());
        settings.setQuestionChannel(persistedSettings.getQuestionChannelId());
        settings.setPingRoleId(persistedSettings.getPingRoleId());
        
        return settings;
    }

    private static Path resolveGuildDataDirectory(long guildId) {
        Path guildDataDirectory = getDataFolder().resolve(String.valueOf(guildId));
        
        if (!Files.isDirectory(guildDataDirectory)) {
            try {
                Files.createDirectories(guildDataDirectory);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        
        return guildDataDirectory;
    }

    public static void persistForGuild(GuildSettings guildSettings) {
        Path settingsFile = getGuildSettingsFile(guildSettings.getGuildId());

        var document = new GuildSettingsDocument();
        document.setModChannelId(guildSettings.getModChannelId().orElse(null));
        document.setQuestionChannelId(guildSettings.getQuestionChannelId().orElse(null));
        document.setPingRoleId(guildSettings.getPingRoleId().orElse(null));
        
        var mapper = new ObjectMapper();
        try {
            String serializedData = mapper.writeValueAsString(document);
            
            Files.deleteIfExists(settingsFile);
            
            try(BufferedWriter writer = Files.newBufferedWriter(settingsFile)) {
                writer.write(serializedData);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Path getGuildSettingsFile(long guildId) {
        return resolveGuildDataDirectory(guildId).resolve("settings.json");
    }
}
