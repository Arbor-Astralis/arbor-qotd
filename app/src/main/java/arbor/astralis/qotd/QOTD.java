package arbor.astralis.qotd;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.util.Color;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public final class QOTD {

    private static final Logger LOGGER = LogManager.getLogger();
    
    private QOTD() {
        // Helper class no instantiation
    }
    
    public static Mono<?> postRandomApprovedQuestion(GatewayDiscordClient client, long guildId) {
        LOGGER.info("Dispatching next approved QOTD for guild: " + guildId);
        return doPostImpl(guildId, client);
    }
    
    public static Mono<?> postRandomApprovedQuestion(ApplicationCommandInteractionEvent event) {
        Optional<Snowflake> guildId = event.getInteraction().getGuildId();
        
        if (guildId.isEmpty()) {
            return Mono.empty();
        }

        return doPostImpl(guildId.get().asLong(), event.getClient());
    }

    private static Mono<?> doPostImpl(long guildId, GatewayDiscordClient client) {
        GuildSettings guildSettings = Settings.forGuild(guildId);
        @Nullable Long questionChannelId = guildSettings.getQuestionChannelId().orElse(null);
        @Nullable Long modChannelId = guildSettings.getModChannelId().orElse(null);

        if (questionChannelId == null) {
            LOGGER.warn("Cannot post question for guild due to no setup: " + guildId);
            return Mono.empty();
        }
        
        MessageChannel questionChannel = (MessageChannel) client.getChannelById(Snowflake.of(questionChannelId)).block();
        Optional<Question> nextQuestion = guildSettings.takeNextRandomApprovedQuestion();
        
        if (nextQuestion.isPresent()) {
            return dispatchQuestionForGuild(nextQuestion.get(), questionChannel, guildSettings)
                .then(possiblyNotifyLowApprovedQuestionCount(questionChannel, guildSettings, client));
        } else {
            return tryNotifyNoApprovedQuestionsForGuild(questionChannel, client);
        }
    }

    private static Mono<?> dispatchQuestionForGuild(
        Question question,
        MessageChannel questionChannel,
        GuildSettings guildSettings
    ) {
        String content = "## " + question.getText();
        
        if (guildSettings.getPingRoleId().isPresent()) {
            content += "\n<@&" + guildSettings.getPingRoleId().get() + ">";
        }
        
        content += "\n\nQuestion submitted by <@" + question.getAuthorId() + ">\n" +
            "Respond in the thread below!";

        var message = MessageCreateSpec.builder()
            .content(content)
            .build();
        
        Mono<Message> messageFlow = questionChannel.createMessage(message);
        
        guildSettings.markQuestionDispatched(question.getId());
        
        return messageFlow;
    }
    
    private static Mono<?> possiblyNotifyLowApprovedQuestionCount(
        MessageChannel questionChannel,
        GuildSettings guildSettings,
        GatewayDiscordClient client
    ) {
        int remainingQuestions = guildSettings.getUndispatchedApprovedQuestionCount();
        
        if (remainingQuestions <= 3) {
            return questionChannel.createMessage(Branding.getLowApprovedUndispatchedQuestionsMessage(remainingQuestions));
        }
        
        return Mono.empty();
    }
    
    private static Mono<?> tryNotifyNoApprovedQuestionsForGuild(
        MessageChannel questionChannel,
        GatewayDiscordClient client
    ) {
        return questionChannel.createMessage(Branding.getNoMoreApprovedUndispatchedQuestionsMessage());
    }
}
