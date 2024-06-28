package arbor.astralis.qotd.commands;

import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.discordjson.json.ImmutableApplicationCommandRequest;
import discord4j.rest.util.Color;
import reactor.core.publisher.Mono;

import java.util.Optional;

public final class HelpCommand implements ApplicationCommand {

    @Override
    public String getName() {
        return ApplicationCommand.NAME_PREFIX + "help";
    }

    @Override
    public String getShortDescription() {
        return "Get help on using the server's QOTD bot";
    }

    @Override
    public Optional<String> getParametersHelpText() {
        return Optional.empty();
    }

    @Override
    public String getExtendedDescription() {
        return "Open this help dialog";
    }

    @Override
    public void create(ImmutableApplicationCommandRequest.Builder request) {
        
    }

    @Override
    public Mono<?> onInteraction(ApplicationCommandInteractionEvent event) {
        var commandHelpText = new StringBuilder();
        for (ApplicationCommand command : ApplicationCommands.getAll()) {
            String commandTitle = "**/" + command.getName() + "**";

            Optional<String> parametersText = command.getParametersHelpText();
            if (parametersText.isPresent()) {
                commandTitle += " " + parametersText.get();
            }
            
            commandHelpText.append(commandTitle).append("\n");
            commandHelpText.append(command.getExtendedDescription()).append("\n\n");
        }

        var embed = EmbedCreateSpec.builder()
            .color(Color.BISMARK)
            .description(commandHelpText.toString())
            .build();

        return event.getInteraction().getChannel().flatMap(channel -> {
            if (channel == null) {
                return Mono.empty();
            }
            
            var spec = InteractionApplicationCommandCallbackSpec.create().withEmbeds(embed);
            
            return event.reply(spec);
        });
    }
    
}
