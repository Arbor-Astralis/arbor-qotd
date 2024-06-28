package arbor.astralis.qotd.persistence;

import arbor.astralis.qotd.Question;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SqliteGuildQuestionStore implements GuildQuestionStore {
    
    private static final String TABLE_NAME = "qotd_questions";
    private static final String COLUMN_QUESTION_ID = "question_id";
    private static final String COLUMN_QUESTION_TEXT = "question_text";
    private static final String COLUMN_AUTHOR_ID = "author_user_id";
    private static final String COLUMN_STATUS = "status";
    
    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_APPROVED = "approved";
    private static final String STATUS_REJECTED = "rejected";
    private static final String STATUS_DISPATCHED = "dispatched";
    
    private final Connection connection;
    
    
    public SqliteGuildQuestionStore(Path dataFilePath) {
        boolean fileExists = Files.exists(dataFilePath);
        
        this.connection = openConnection(dataFilePath);

        if (!fileExists) {
            try {
                createTablesAndIndices();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private Connection openConnection(Path dataFilePath) {
        String url = "jdbc:sqlite:" + dataFilePath.toAbsolutePath();
        
        // Has side effect of creating the database if it does not exist
        try {
            return DriverManager.getConnection(url);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void createTablesAndIndices() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            createQuestionsTable(statement);
        }
    }

    private void createQuestionsTable(Statement statement) throws SQLException {
        statement.execute(
            "CREATE TABLE " + TABLE_NAME +
            "(" +
                COLUMN_QUESTION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + 
                COLUMN_QUESTION_TEXT + " TEXT NOT NULL," +
                COLUMN_AUTHOR_ID + " INTEGER NOT NULL," +
                COLUMN_STATUS + " TEXT NOT NULL" +
            ")"
        );

        statement.execute(
            "CREATE INDEX index_status " +
                "ON " + TABLE_NAME + " (" + COLUMN_STATUS + ")"
        );
    }

    @Override
    public synchronized List<Question> getUndispatchedApprovedQuestions(int limit) {
        if (limit < 1) {
            throw new RuntimeException("Invalid limit value: " + limit);
        }

        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT * " +
            "FROM " + TABLE_NAME + " " +
            "WHERE " + COLUMN_STATUS + " = ?")
        ) {
            statement.setString(1, STATUS_APPROVED);
            
            ResultSet resultSet = statement.executeQuery();
            List<Question> questions = new ArrayList<>();
            
            while (resultSet.next()) {
                long id = resultSet.getLong(COLUMN_QUESTION_ID);
                long authorId = resultSet.getLong(COLUMN_AUTHOR_ID);
                String questionText = resultSet.getString(COLUMN_QUESTION_TEXT);
                
                questions.add(new Question(id, authorId, questionText));
            }
            
            return questions;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized Question addUndispatchedQuestion(String question, long submitMemberId, boolean approved) {
        try (PreparedStatement statement = connection.prepareStatement(
            "INSERT INTO " + TABLE_NAME + " (question_text, author_user_id, status) " +
            "VALUES (?, ?, ?)")
        ) {
            statement.setString(1, question);
            statement.setLong(2, submitMemberId);
            statement.setString(3, approved ? STATUS_APPROVED : STATUS_PENDING);

            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        try (Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(
                "SELECT * " +
                    "FROM " + TABLE_NAME + " " +
                    "WHERE " + COLUMN_QUESTION_ID + " = " +
                    "(SELECT MAX(" + COLUMN_QUESTION_ID + ") FROM " + TABLE_NAME + ")"
            );

            if (!resultSet.next()) {
                throw new RuntimeException("No record found for last inserted question!");
            }

            long questionId = resultSet.getLong(COLUMN_QUESTION_ID);
            String questionText = resultSet.getString(COLUMN_QUESTION_TEXT);

            return new Question(questionId, submitMemberId,questionText);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized void markDispatched(long questionId) {
        updateStatus(questionId, STATUS_DISPATCHED);
    }

    @Override
    public void markApproved(long questionId) {
        updateStatus(questionId, STATUS_APPROVED);
    }

    @Override
    public void markRejected(long questionId) {
        updateStatus(questionId, STATUS_REJECTED);
    }

    private void updateStatus(long questionId, String newStatus) {
        try (PreparedStatement statement = connection.prepareStatement(
            "UPDATE " + TABLE_NAME + " " +
            "SET " + COLUMN_STATUS + " = ? " + 
            "WHERE " + COLUMN_QUESTION_ID + " = ?"
        )) {
            statement.setString(1, newStatus);
            statement.setLong(2, questionId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized void removeAllApprovedQuestions() {
        try (Statement statement = connection.createStatement()) {
            statement.executeQuery("DELETE FROM " + TABLE_NAME + " WHERE " + COLUMN_STATUS + " = " + STATUS_APPROVED);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getUndispatchedApprovedQuestionCount() {
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT COUNT(*) " +
            "FROM " + TABLE_NAME + " " +
            "WHERE " + COLUMN_STATUS + " = ?"
        )) {
            statement.setString(1, STATUS_APPROVED);
            ResultSet resultSet = statement.executeQuery(); 
            
            if (!resultSet.next()) {
                throw new RuntimeException("Failed to obtain approved undispatched count");
            }
            
            return resultSet.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
