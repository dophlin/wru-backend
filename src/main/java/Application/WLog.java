package Application;

/**
 * Created by dophlin on 7/21/2016.
 */
public class WLog {
    public enum Type {INFO, DEBUG, RELEASE};
    public static void console(Type type, String log) {
        switch (type) {
            case INFO:
                System.out.print("Info --> ");
                break;
            case DEBUG:
                System.out.print("Debug--> ");
                break;
            case RELEASE:
                System.out.print("Release--> ");
                break;
        }
        System.out.println(log);
    }
}
