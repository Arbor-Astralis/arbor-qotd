package arbor.astralis.qotd;

public final class Question {

    private final long id;
    private final long authorId;
    private final String text;
    
    public Question(long id, long authorId, String text) {
        this.id = id;
        this.authorId = authorId;
        this.text = text;
    }

    public long getId() {
        return id;
    }

    public long getAuthorId() {
        return authorId;
    }

    public String getText() {
        return text;
    }
}
