package arbor.astralis.qotd.commands;

import arbor.astralis.qotd.Branding;
import arbor.astralis.qotd.GuildSettings;
import arbor.astralis.qotd.Main;
import arbor.astralis.qotd.Settings;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ImmutableApplicationCommandRequest;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class SetModChannelCommand implements ApplicationCommand {
    
    private static final String CHANNEL_PARAMETER_NAME = "channel";
    
    
    @Override
    public String getName() {
        return ApplicationCommand.NAME_PREFIX + "set-mod-channel";
    }

    @Override
    public String getShortDescription() {
        return "Sets a channel for moderators to approve new QOTDs";
    }

    @Override
    public String getExtendedDescription() {
        return "(Admin only) " + getShortDescription();
    }

    @Override
    public Optional<String> getParametersHelpText() {
        return Optional.of("<#channel-id>");
    }

    @Override
    public void create(ImmutableApplicationCommandRequest.Builder request) {
        var option = ApplicationCommandOptionData.builder()
            .type(7)
            .channelTypes(List.of(0 /* GUILD_TEXT */))
            .name(CHANNEL_PARAMETER_NAME)
            .description("The QOTD moderation channel to designate")
            .required(true)
            .build();

        request.addOption(option);
    }

    @Override
    public Mono<?> onInteraction(ApplicationCommandInteractionEvent event) {
        boolean isAdmin = CommandHelper.isTriggerUserAdmin(event);
        
        if (!isAdmin) {
            return event.reply().withContent(Branding.getAdminOnlyAccessMessage());
        }
        
        Map<String, String> optionValues = CommandHelper.marshalOptionValues(event);
        
        String rawChannelId = optionValues.get(CHANNEL_PARAMETER_NAME);
        if (rawChannelId == null || rawChannelId.isBlank()) {
            return event.reply()
                .withEphemeral(true)
                .withContent(Branding.getUnexpectedErrorMessage("Missing rawChannelId"));
        }
        
        long channelId = -1;
        
        try {
            channelId = Long.parseLong(rawChannelId);
        } catch (NumberFormatException e) {
            Main.LOGGER.error("Failed to parse channel ID", e);
        }
        
        if (channelId < 0) {
            return event.reply()
                .withEphemeral(true)
                .withContent(Branding.getUnexpectedErrorMessage("Error parsing rawChannelId"));
        }

        Optional<Snowflake> guildId = event.getInteraction().getGuildId();
        
        if (guildId.isEmpty()) {
            return event.reply()
                .withEphemeral(true)
                .withContent(Branding.getUnexpectedErrorMessage("Missing guildId"));
        }
        
        GuildSettings settings = Settings.forGuild(guildId.get().asLong());
        settings.setQuestionModChannel(channelId);
        
        return event.reply()
            .withContent(Branding.getModChannelSetSuccessfulMessage(channelId));
    }
}
