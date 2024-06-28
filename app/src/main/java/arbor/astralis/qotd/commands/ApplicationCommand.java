package arbor.astralis.qotd.commands;

import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.discordjson.json.ImmutableApplicationCommandRequest;
import reactor.core.publisher.Mono;

import java.util.Optional;

public interface ApplicationCommand {
    
    String NAME_PREFIX = "qotd-";

    
    String getName();
    
    String getShortDescription();
    
    Optional<String> getParametersHelpText();
    
    default String getExtendedDescription() {
        return getShortDescription();
    }

    void create(ImmutableApplicationCommandRequest.Builder request);
    
    Mono<?> onInteraction(ApplicationCommandInteractionEvent event);

    default Mono<?> onButtonInteraction(String[] payload, ButtonInteractionEvent event) {
        return Mono.empty();
    }
}
