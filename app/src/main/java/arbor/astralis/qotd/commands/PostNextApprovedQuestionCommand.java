package arbor.astralis.qotd.commands;

import arbor.astralis.qotd.Branding;
import arbor.astralis.qotd.QOTD;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.discordjson.json.ImmutableApplicationCommandRequest;
import reactor.core.publisher.Mono;

import java.util.Optional;

public final class PostNextApprovedQuestionCommand implements ApplicationCommand {
    
    @Override
    public String getName() {
        return ApplicationCommand.NAME_PREFIX + "post-next-approved";
    }

    @Override
    public String getShortDescription() {
        return "Posts next approved question in the QOTD channel";
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
        
        return QOTD.postRandomApprovedQuestion(event);
    }
}
