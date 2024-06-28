package arbor.astralis.qotd.persistence;

import arbor.astralis.qotd.Question;

import java.util.List;

public interface GuildQuestionStore {
    
    List<Question> getUndispatchedApprovedQuestions(int limit);
    
    Question addUndispatchedQuestion(String question, long submitMemberId, boolean approved);
    
    void markDispatched(long questionId);
    
    void markApproved(long questionId);
    
    void markRejected(long questionId);

    void removeAllApprovedQuestions();

    int getUndispatchedApprovedQuestionCount();
    
}
