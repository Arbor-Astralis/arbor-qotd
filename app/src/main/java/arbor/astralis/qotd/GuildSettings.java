package arbor.astralis.qotd;

import arbor.astralis.qotd.persistence.GuildQuestionStore;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class GuildSettings {
    
    private final GuildQuestionStore store;
    private final long guildId;
    
    private @Nullable Long pingRoleId;
    private @Nullable Long modChannelId;
    private @Nullable Long questionChannelId;
    
    
    public GuildSettings(long guildId, GuildQuestionStore store) {
        this.guildId = guildId;
        this.store = Objects.requireNonNull(store);
    }

    public synchronized Optional<Question> takeNextRandomApprovedQuestion() {
        List<Question> undispatchedQuestions = store.getUndispatchedApprovedQuestions(1);
        
        if (undispatchedQuestions.isEmpty()) {
            return Optional.empty();
        }
        
        int randomIndex = (int) (Math.random() * undispatchedQuestions.size());
        Question randomQuestion = undispatchedQuestions.get(randomIndex);
        
        return Optional.of(randomQuestion);
    }
    
    public synchronized Question addUnapprovedQuestion(String question, long submitMemberId) {
        return store.addUndispatchedQuestion(question, submitMemberId, false);
    }
    
    public synchronized Question addApprovedQuestion(String question, long submitMemberId) {
        return store.addUndispatchedQuestion(question, submitMemberId, true);
    }
    
    public synchronized void removeAllApprovedQuestions() {
        store.removeAllApprovedQuestions();
    }
    
    public synchronized Optional<Long> getModChannelId() {
        return Optional.ofNullable(modChannelId);
    }
    
    public synchronized void setQuestionModChannel(@Nullable Long channelId) {
        this.modChannelId = channelId;
        Settings.persistForGuild(this);
    }
    
    public synchronized Optional<Long> getPingRoleId() {
        return Optional.ofNullable(pingRoleId);
    }
    
    public synchronized void setPingRoleId(@Nullable Long roleId) {
        this.pingRoleId = roleId;
        Settings.persistForGuild(this);
    }

    public synchronized Optional<Long> getQuestionChannelId() {
        return Optional.ofNullable(questionChannelId);
    }

    public synchronized void setQuestionChannel(@Nullable Long channelId) {
        this.questionChannelId = channelId;
        Settings.persistForGuild(this);
    }

    public synchronized long getGuildId() {
        return guildId;
    }
    
    public synchronized void markQuestionDispatched(long questionId) {
        store.markDispatched(questionId);
    }

    public synchronized void markQuestionApproved(long questionId) {
        store.markApproved(questionId);
    }

    public synchronized void markQuestionRejected(long questionId) {
        store.markRejected(questionId);
    }

    public int getUndispatchedApprovedQuestionCount() {
        return store.getUndispatchedApprovedQuestionCount();
    }
}
