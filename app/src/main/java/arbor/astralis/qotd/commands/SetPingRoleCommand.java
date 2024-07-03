package arbor.astralis.qotd.commands;

import arbor.astralis.qotd.Branding;
import arbor.astralis.qotd.GuildSettings;
import arbor.astralis.qotd.Settings;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ImmutableApplicationCommandRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

public final class SetPingRoleCommand implements ApplicationCommand {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String ROLE_PARAMETER_NAME = "role";
    
    
    @Override
    public String getName() {
        return ApplicationCommand.NAME_PREFIX + "set-ping-role";
    }

    @Override
    public String getShortDescription() {
        return "Sets a role to ping when posting QOTD";
    }

    @Override
    public String getExtendedDescription() {
        return "(Admin only) " + getShortDescription();
    }

    @Override
    public Optional<String> getParametersHelpText() {
        return Optional.of("<@&role-id>");
    }

    @Override
    public void create(ImmutableApplicationCommandRequest.Builder request) {
        var option = ApplicationCommandOptionData.builder()
            .type(8)
            .name(ROLE_PARAMETER_NAME)
            .description("The role to ping when a new QOTD is posted")
            .required(false)
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

        String rawRoleId = optionValues.get(ROLE_PARAMETER_NAME);
        if (rawRoleId == null || rawRoleId.isBlank()) {
            rawRoleId = String.valueOf(-1);
        }

        long roleId = -1;

        try {
            roleId = Long.parseLong(rawRoleId);
        } catch (NumberFormatException e) {
            LOGGER.error("Failed to parse role ID", e);
        }

        Optional<Snowflake> guildId = event.getInteraction().getGuildId();

        if (guildId.isEmpty()) {
            return event.reply()
                .withEphemeral(true)
                .withContent(Branding.getUnexpectedErrorMessage("Missing guildId"));
        }

        GuildSettings settings = Settings.forGuild(guildId.get().asLong());
        settings.setPingRoleId(roleId >= 0 ? roleId : null);

        return event.reply()
            .withContent(Branding.getPingRoleSetSuccessfulMessage(roleId));
    }
}
