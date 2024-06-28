package arbor.astralis.qotd;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public final class QOTD {
    
    private QOTD() {
        // Helper class no instantiation
    }
    
    public static Mono<?> postRandomApprovedQuestion(GatewayDiscordClient client, long guildId) {
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
            Main.LOGGER.warn("Cannot post question for guild due to no setup: " + guildId);
            return Mono.empty();
        }
        
        MessageChannel questionChannel = (MessageChannel) client.getChannelById(Snowflake.of(questionChannelId)).block();
        Optional<Question> nextQuestion = guildSettings.takeNextRandomApprovedQuestion();
        
        if (nextQuestion.isPresent()) {
            return dispatchQuestionForGuild(nextQuestion.get(), questionChannel, guildSettings)
                .then(possiblyNotifyLowApprovedQuestionCount(questionChannel, modChannelId, guildSettings, client));
        } else {
            return tryNotifyNoApprovedQuestionsForGuild(questionChannel, modChannelId, guildSettings, client);
        }
    }

    private static Mono<?> dispatchQuestionForGuild(
        Question question,
        MessageChannel questionChannel,
        GuildSettings guildSettings
    ) {
        var embed = EmbedCreateSpec.builder()
            .description("### " + question.getText() + "\n\nby <@" + question.getAuthorId() + ">")
            .build();

        String content = "";
        
        if (guildSettings.getPingRoleId().isPresent()) {
            content = "<@&" + guildSettings.getPingRoleId().get() + ">";
        }

        var message = MessageCreateSpec.builder()
            .content(content)
            .embeds(List.of(embed))
            .build();
        
        Mono<Message> messageFlow = questionChannel.createMessage(message);
        
        guildSettings.markQuestionDispatched(question.getId());
        
        return messageFlow;
    }
    
    private static Mono<?> possiblyNotifyLowApprovedQuestionCount(
        MessageChannel questionChannel,
        @Nullable Long modChannelId,
        GuildSettings guildSettings,
        GatewayDiscordClient client
    ) {
        MessageChannel channel = questionChannel;
        
        if (modChannelId != null) {
            channel = (MessageChannel) client.getChannelById(Snowflake.of(modChannelId)).block();
            
            if (channel == null) {
                channel = questionChannel;
            }
        }
        
        int remainingQuestions = guildSettings.getUndispatchedApprovedQuestionCount();
        
        if (remainingQuestions <= 4) {
            return channel.createMessage(Branding.getLowApprovedUndispatchedQuestionsMessage(remainingQuestions));
        }
        
        return Mono.empty();
    }
    
    private static Mono<?> tryNotifyNoApprovedQuestionsForGuild(MessageChannel questionChannel, Long modChannelId, GuildSettings guildSettings, GatewayDiscordClient client) {
        MessageChannel channel = questionChannel;

        if (modChannelId != null) {
            channel = (MessageChannel) client.getChannelById(Snowflake.of(modChannelId)).block();

            if (channel == null) {
                channel = questionChannel;
            }
        }

        return channel.createMessage(Branding.getNoMoreApprovedUndispatchedQuestionsMessage());
    }
}
