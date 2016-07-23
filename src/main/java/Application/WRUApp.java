package Application;

import org.apache.log4j.BasicConfigurator;

/**
 * Created by dophlin on 7/21/2016.
 */
public class WRUApp {
    public static void main(String[] args) {
        BasicConfigurator.configure();
        WLog.console(WLog.Type.INFO, "WRU is started...");
        MainVertx.getSingleton().deployRestSignal();
    }
}
