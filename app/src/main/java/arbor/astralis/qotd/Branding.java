package arbor.astralis.qotd;

import javax.annotation.Nullable;

public final class Branding {
    
    public static final String MAINTAINER = "Haru";
    
    private Branding() {
        
    }
    
    public static String getQuestionApprovedMessage() {
        return takeRandom(
            "Done! Keep your eyes on the QOTD channel -- it'll be asked on a random day!"
        );
    }
    
    public static String getUnexpectedErrorMessage(String details) {
        return takeRandom(
            "Oops, something went wrong :( Please let " + MAINTAINER + " know what happened.\nError details: " + details
        );
    }

    private static String takeRandom(String ... messages) {
        int randomIndex = (int) (Math.random() * messages.length);
        return messages[randomIndex];
    }

    public static String getQuestionPendingApprovalMessageForSubmitter() {
        return takeRandom(
            "All done! Your question has been submitted for approval. I'll let you know once it's approved~"
        );
    }

    public static String getQuestionApprovalMessageForModerators() {
        return takeRandom(
            "Hey, there's a new QOTD submission from someone!"
        );
    }

    public static String getModChannelSetSuccessfulMessage(@Nullable Long newChannelId) {
        if (newChannelId == null) {
            return "Moderation channel disabled. New QOTDs will be approved immediately.";
        } else {
            return "Moderation channel updated successfully! New channel is: <#" + newChannelId + ">";
        }
    }

    public static String getAdminOnlyAccessMessage() {
        return "You must be a server administrator to use this command!";
    }

    public static String getQuestionChannelSetSuccessfulMessage(@Nullable Long newChannelId) {
        return "QOTD channel updated successfully! New channel is: <#" + newChannelId + ">";
    }

    public static String getPingRoleSetSuccessfulMessage(long roleId) {
        return "QOTD ping role updated successfully! New role is: <@&" + roleId + ">";
    }

    public static String getQuestionAllRemovedMessage() {
        return "All approved QOTD entries have been purged from the pool";
    }

    public static String getQuestionChannelNotSetMessage() {
        return "Um... guys? The QOTD question channel is not set for this server. I can't post the next question. :(";
    }

    public static String getNoMoreApprovedUndispatchedQuestionsMessage() {
        return takeRandom(
            "Um... guys? There's no more approved QOTD questions for me to send anymore",
            "Does somebody still care around here?! We need some QOTD questions, it's EMPTY!",
            "Any QOTD questions for me tomorrow?"
        );
    }

    public static String getLowApprovedUndispatchedQuestionsMessage(int remainingQuestions) {
        if (remainingQuestions > 1) {
            return "Just so you know... there are " + remainingQuestions + " questions left for QOTD";
        } else if (remainingQuestions == 1) {
            return "Hey uh... There's only 1 question left for QOTD, guys...";
        } else {
            return "Okay guys, I got no more QOTDs for tomorrow...";
        }
    }
}
