package twitter.java.message;
import java.sql.Date;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class Message {
    private  int m_id;
    private String m_senderHash;
    private String m_receiverHash;
    private String m_content;
    private Date m_creationDate;

    public Message(int id, String senderHash, String receiverHash, String content, Date creationDate) {
        m_id = id;
        m_senderHash = senderHash;
        m_receiverHash = receiverHash;
        m_content = content;
        m_creationDate = creationDate;
    }

    public Message(String senderHash, String receiverHash, String content) {
        m_senderHash = senderHash;
        m_receiverHash = receiverHash;
        m_content = content;
        m_creationDate = Date.valueOf(LocalDateTime.now(ZoneId.of("UTC")).toLocalDate());
    }

    public void show() {
        System.out.printf("ID : %d\nSender : %s\nReceiver : %s\nContent : %s\nCreation date : %s\n", m_id, m_senderHash, m_receiverHash, m_content, m_creationDate.toString());
    }

    public int getId() {
        return m_id;
    }

    public String getSenderHash() {
        return m_senderHash;
    }

    public String getReceiverHash() {
        return m_receiverHash;
    }

    public String getContent() {
        return m_content;
    }

    public Date getCreationDate() {
        return m_creationDate;
    }
}