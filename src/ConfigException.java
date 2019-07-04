
public class ConfigException extends Exception {

    public ConfigException(String message) {
        super(message);
        m_message = message;
    }

    public String getMessage() {
        return m_message;
    }

    ///////////////////data member///////////////////////////
    private String m_message = "";
}
