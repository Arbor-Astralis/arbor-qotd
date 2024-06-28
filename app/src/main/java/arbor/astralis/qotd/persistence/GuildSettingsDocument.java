package arbor.astralis.qotd.persistence;

import javax.annotation.Nullable;

public final class GuildSettingsDocument {
    
    private @Nullable Long questionChannelId;
    private @Nullable Long modChannelId;
    private @Nullable Long pingRoleId;

    @Nullable
    public Long getQuestionChannelId() {
        return questionChannelId;
    }

    public void setQuestionChannelId(@Nullable Long questionChannelId) {
        this.questionChannelId = questionChannelId;
    }

    @Nullable
    public Long getModChannelId() {
        return modChannelId;
    }

    public void setModChannelId(@Nullable Long modChannelId) {
        this.modChannelId = modChannelId;
    }

    @Nullable
    public Long getPingRoleId() {
        return pingRoleId;
    }

    public void setPingRoleId(@Nullable Long pingRoleId) {
        this.pingRoleId = pingRoleId;
    }
}
