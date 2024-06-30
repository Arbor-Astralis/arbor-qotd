package arbor.astralis.qotd.commands;

import arbor.astralis.qotd.Branding;
import arbor.astralis.qotd.GuildSettings;
import arbor.astralis.qotd.Question;
import arbor.astralis.qotd.Settings;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ImmutableApplicationCommandRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class AddQuestionCommand implements ApplicationCommand {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String QUESTION_PARAMETER_NAME = "question";
    
    private static final String BUTTON_ACTION_ACCEPT = "accept";
    private static final String BUTTON_ACTION_REJECT = "reject";
    
    
    @Override
    public String getName() {
        return ApplicationCommand.NAME_PREFIX + "add";
    }

    @Override
    public String getShortDescription() {
        return "Suggest a new question be added to the question pool";
    }

    @Override
    public Optional<String> getParametersHelpText() {
        return Optional.of("<your question>");
    }

    @Override
    public void create(ImmutableApplicationCommandRequest.Builder request) {
        var option = ApplicationCommandOptionData.builder()
            .type(3)
            .name(QUESTION_PARAMETER_NAME)
            .maxLength(128)
            .minLength(8)
            .description("The question to be asked")
            .required(true)
            .build();
        
        request.addOption(option);
    }

    @Override
    public Mono<?> onInteraction(ApplicationCommandInteractionEvent event) {
        Optional<Snowflake> guildIdOptional = event.getInteraction().getGuildId();
        
        if (guildIdOptional.isEmpty()) {
            return event.deleteReply();
        }
        
        Map<String, String> optionValues = CommandHelper.marshalOptionValues(event);
        
        String question = optionValues.get(QUESTION_PARAMETER_NAME);
        question = sanitizeQuestion(question);
        
        if (question == null || question.isBlank()) {
            return event.deleteReply();
        }

        Snowflake guildId = guildIdOptional.get();
        long submitMemberId = event.getInteraction().getUser().getId().asLong();
        
        GuildSettings guildSettings = Settings.forGuild(guildId.asLong());

        if (guildSettings.getModChannelId().isEmpty()) {
            return acknowledgeAsApproved(event, guildSettings, question, submitMemberId);
        } else {
            return acknowledgeAsUnapproved(event, guildSettings, question, submitMemberId);
        }
    }

    private Mono<?> acknowledgeAsUnapproved(
        ApplicationCommandInteractionEvent event, 
        GuildSettings guildSettings, 
        String question,
        long submitMemberId
    ) {
        Optional<Long> modChannelId = guildSettings.getModChannelId();
        
        if (modChannelId.isEmpty()) {
            return event.reply(Branding.getUnexpectedErrorMessage("modChannelId is empty"));
        }

        Guild guild = event.getInteraction().getGuild().block();
        
        if (guild == null) {
            return event.reply(Branding.getUnexpectedErrorMessage("guild == null"));
        }
        
        GuildChannel channel = guild.getChannelById(Snowflake.of(modChannelId.get())).block();

        if (!(channel instanceof MessageChannel messageChannel)) {
            return event.reply(Branding.getUnexpectedErrorMessage("modChannelId points to non-MessageChannel"));
        }

        Question newQuestion = guildSettings.addUnapprovedQuestion(question, submitMemberId);

        User user = event.getInteraction().getUser();
        
        var embed = EmbedCreateSpec.builder()
            .description("**" + question + "**\n\nby <@" + user.getId().asLong() + ">")
            .build();

        ActionRow actionRow = ActionRow.of(
            Button.primary(
                CommandHelper.createCommandButtonPayload(this, newQuestion.getId(), BUTTON_ACTION_ACCEPT), 
                "Accept"
            ),
            Button.danger(
                CommandHelper.createCommandButtonPayload(this, newQuestion.getId(), BUTTON_ACTION_REJECT), 
                "Reject"
            )
        );

        var modChannelMessage = MessageCreateSpec.builder()
            .content(Branding.getQuestionApprovalMessageForModerators())
            .embeds(List.of(embed))
            .addAllComponents(List.of(actionRow))
            .build();

        return messageChannel.createMessage(modChannelMessage)
            .and(event.reply().withEphemeral(true).withContent(Branding.getQuestionPendingApprovalMessageForSubmitter()));
    }

    private Mono<?> acknowledgeAsApproved(
        ApplicationCommandInteractionEvent event, 
        GuildSettings guildSettings,
        String question,
        long submitMemberId
    ) {
        guildSettings.addApprovedQuestion(question, submitMemberId);
        
        return event.reply()
            .withEphemeral(true)
            .withContent(Branding.getQuestionApprovedMessage());
    }

    private String sanitizeQuestion(String rawQuestion) {
        String question = rawQuestion.trim();
        
        if (!question.endsWith("?")) {
            question += "?";
        }
        
        return makeFirstLetterUppercase(question);
    }

    private String makeFirstLetterUppercase(String question) {
        for (int i = 0; i < question.length(); i++) {
            char character = question.charAt(i);
            
            if (Character.isLetter(character)) {
                if (Character.isLowerCase(character)) {
                    character = Character.toUpperCase(character);

                    String sanitizedResult = question.substring(0, i) + character;
                    if (i < question.length() - 1) {
                        sanitizedResult += question.substring(i + 1);
                    }

                    return sanitizedResult;
                } else {
                    return question;
                }
            }
        }
        
        return question;
    }

    @Override
    public Mono<?> onButtonInteraction(String[] payload, ButtonInteractionEvent event) {
        if (payload.length != 3) {
            LOGGER.warn("Malformed ButtonInteraction for AddQuestionCommand, size mismatch: " + payload.length);
            return Mono.empty();
        }
        
        String rawQuestionId = payload[1];
        long questionId = -1;
        
        try {
             questionId = Long.parseLong(rawQuestionId);
        } catch (NumberFormatException e) {
            LOGGER.warn("Failed to parse questionId: " + rawQuestionId + " for payload: " + event.getCustomId());
            return Mono.empty();
        }
        
        String actionId = payload[2];

        Optional<Snowflake> guildId = event.getInteraction().getGuildId();
        if (guildId.isEmpty()) {
            LOGGER.warn("No guildId for AddQuestionCommand ButtonInteraction: " + event.getCustomId());
            return Mono.empty();
        }
        
        GuildSettings guildSettings = Settings.forGuild(guildId.get().asLong());
        
        if (BUTTON_ACTION_ACCEPT.equals(actionId)) {
            return approveQuestion(questionId, guildSettings, event);
        } else if (BUTTON_ACTION_REJECT.equals(actionId)) {
            return rejectQuestion(questionId, guildSettings, event);
        } else {
            LOGGER.warn("Undefined actionId for AddQuestionCommand ButtonInteraction: " + event.getCustomId());
            return Mono.empty();
        }
    }

    private Mono<?> rejectQuestion(long questionId, GuildSettings guildSettings, ButtonInteractionEvent event) {
        guildSettings.markQuestionRejected(questionId);

        Snowflake actionedUserId = event.getInteraction().getUser().getId();
        
        return event.edit()
            .withContent("Rejected by <@" + actionedUserId.asLong() + ">")
            .withComponents();
    }

    private Mono<?> approveQuestion(long questionId, GuildSettings guildSettings, ButtonInteractionEvent event) {
        guildSettings.markQuestionApproved(questionId);
        
        Snowflake actionedUserId = event.getInteraction().getUser().getId();
        
        return event.edit()
            .withContent("Accepted by <@" + actionedUserId.asLong() + ">")
            .withComponents();
    }
}
