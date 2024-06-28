package arbor.astralis.qotd.commands;

import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.discordjson.json.ApplicationCommandInteractionOptionData;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;

import java.util.HashMap;
import java.util.Map;

public final class CommandHelper {

    private static final String PAYLOAD_SEPARATOR = ";";

    private CommandHelper() {
        
    }
    
    public static Map<String, String> marshalOptionValues(ApplicationCommandInteractionEvent event) {
        Map<String, String> optionValues = new HashMap<>(2);

        event.getInteraction().getData().data().toOptional().flatMap(commandData ->
            commandData.options().toOptional()).ifPresent(options -> {
                for (ApplicationCommandInteractionOptionData option : options) {
                    if (option.value().isAbsent()) {
                        continue;
                    }

                    optionValues.put(option.name(), option.value().get());
                }
            }
        );

        return optionValues;
    }

    public static boolean isTriggerUserAdmin(ApplicationCommandInteractionEvent event) {
        Guild guild = event.getInteraction().getGuild().block();
        
        if (guild == null) {
            return false;
        }
        
        var commandUser = event.getInteraction().getUser();
        Member guildMember = guild.getMemberById(commandUser.getId()).block();
        
        if (guildMember == null) {
            return false;
        }
        
        PermissionSet permissions = guildMember.getBasePermissions().block();
        if (permissions == null) {
            return false;
        }
        
        return permissions.contains(Permission.ADMINISTRATOR);
    }

    public static String createCommandButtonPayload(ApplicationCommand command, Object ... data) {
        StringBuilder payload = new StringBuilder(command.getName());
        
        if (data.length > 0) {
            payload.append(PAYLOAD_SEPARATOR);
        }

        for (int i = 0; i < data.length; i++) {
            payload.append(escapePayloadValue(String.valueOf(data[i])));
            
            if (i != data.length - 1) {
                payload.append(PAYLOAD_SEPARATOR);
            }
        }
        
        return payload.toString();
    }
    
    public static String[] parseCommandButtonPayload(String payload) {
        String[] dataParts = payload.split(PAYLOAD_SEPARATOR);

        for (int i = 0; i < dataParts.length; i++) {
            dataParts[i] = unescapePayloadValue(dataParts[i]);
        }
        
        return dataParts;
    }

    private static String unescapePayloadValue(String entry) {
        return entry.replaceAll("\\\\" + PAYLOAD_SEPARATOR, PAYLOAD_SEPARATOR);
    }

    private static String escapePayloadValue(String entry) {
        return entry.replaceAll(PAYLOAD_SEPARATOR, "\\\\" + PAYLOAD_SEPARATOR);
    }
}
