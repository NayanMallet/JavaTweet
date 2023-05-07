package twitter.java.exceptions;

public class ExceptionsReader extends Exception {

    public ExceptionsReader() {
        super();
    }

    public ExceptionsReader(String s) {
        super(s);
    }

    public static void log(Exception e) {
        System.out.println(e.getMessage());
    }
}