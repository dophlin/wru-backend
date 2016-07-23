package Application;

import Verticles.RestSignalingVerticle;
import io.vertx.core.Vertx;

/**
 * Created by dophlin on 7/21/2016.
 */
public class MainVertx {
    private static MainVertx mMainVertx;
    private Vertx mVertx;
    private RestSignalingVerticle mRestSignalingVerticle;
    // ----------------------------------------------------------------
    public static MainVertx getSingleton() {
        if(mMainVertx == null) {
            mMainVertx = new MainVertx();
        }
        return mMainVertx;
    }
    // ----------------------------------------------------------------
    private MainVertx() {
        mVertx = Vertx.vertx();
    }
    // ----------------------------------------------------------------
    public void deployRestSignal() {
        mRestSignalingVerticle = new RestSignalingVerticle();
        mVertx.deployVerticle(mRestSignalingVerticle);
    }
}
