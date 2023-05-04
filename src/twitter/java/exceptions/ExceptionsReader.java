package twitter.java.exceptions;

public class ExceptionsReader extends Exception {

    public ExceptionsReader() {
        super();
    }

    public ExceptionsReader(String s) {
        super(s);
    }

//    public static class testException {
//        public static void strVerif(String s) throws ExceptionsReader {
//            if (s.equals(""))
//                throw new ExceptionsReader("Error: empty string");
//            if (s.length() > 50)
//                throw new ExceptionsReader("Error: string too long");
//        }
//
//        public static void intVerif(int i) throws ExceptionsReader {
//            if (i < 0)
//                throw new ExceptionsReader("Error: negative number");
//        }
//
//        public static void floatVerif(float f) throws ExceptionsReader {
//            if (f < 0)
//                throw new ExceptionsReader("Error: negative number");
//        }
//    }


    public static void log(Exception e) {
        System.out.println(e.getMessage());
    }
}