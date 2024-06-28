package arbor.astralis.qotd.commands;

import arbor.astralis.qotd.Branding;
import arbor.astralis.qotd.GuildSettings;
import arbor.astralis.qotd.Settings;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.discordjson.json.ImmutableApplicationCommandRequest;
import reactor.core.publisher.Mono;

import java.util.Optional;

public final class RemoveAllApprovedQuestionsCommand implements ApplicationCommand {
    
    @Override
    public String getName() {
        return ApplicationCommand.NAME_PREFIX + "remove-all-approved";
    }

    @Override
    public String getShortDescription() {
        return "Remove all approved questions from the pool";
    }

    @Override
    public String getExtendedDescription() {
        return "(Admin only) " + getShortDescription();
    }

    @Override
    public Optional<String> getParametersHelpText() {
        return Optional.empty();
    }

    @Override
    public void create(ImmutableApplicationCommandRequest.Builder request) {

    }

    @Override
    public Mono<?> onInteraction(ApplicationCommandInteractionEvent event) {
        boolean isAdmin = CommandHelper.isTriggerUserAdmin(event);

        if (!isAdmin) {
            return event.reply().withContent(Branding.getAdminOnlyAccessMessage());
        }
        
        Optional<Snowflake> guildId = event.getInteraction().getGuildId();

        if (guildId.isEmpty()) {
            return event.reply()
                .withEphemeral(true)
                .withContent(Branding.getUnexpectedErrorMessage("Missing guildId"));
        }
        
        GuildSettings settings = Settings.forGuild(guildId.get().asLong());
        settings.removeAllApprovedQuestions();

        return event.reply()
            .withContent(Branding.getQuestionAllRemovedMessage());
    }
}
