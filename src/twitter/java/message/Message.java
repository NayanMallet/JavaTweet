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
    private boolean m_isRead = false;

    public Message(int id, String senderHash, String receiverHash, String content, Date creationDate, boolean isRead) {
        m_id = id;
        m_senderHash = senderHash;
        m_receiverHash = receiverHash;
        m_content = content;
        m_creationDate = creationDate;
        m_isRead = isRead;
    }

    public Message(String senderHash, String receiverHash, String content) {
        m_senderHash = senderHash;
        m_receiverHash = receiverHash;
        m_content = content;
        m_creationDate = Date.valueOf(LocalDateTime.now(ZoneId.of("UTC")).toLocalDate());
    }

    public void show(boolean isSender) {
        if (isSender) {
            System.out.printf("@%s\n%s\n%s\n%s\n", m_senderHash, m_content, m_creationDate, (m_isRead ? "✅" : "❌"));
        } else {
            System.out.printf("%20s@%s\n%20s%s\n%20s%s\n%20s%s\n", "", m_senderHash,"", m_content,"", m_creationDate,"", (m_isRead ? "✅" : "❌"));
        }
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