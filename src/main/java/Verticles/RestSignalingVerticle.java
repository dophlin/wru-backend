package Verticles;

import Application.WLog;
import Models.ContactJsonModel;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.impl.Json;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.UUID;

public class RestSignalingVerticle extends AbstractVerticle {
    private MongoClient mMongoClient;
    // ----------------------------------------------------------------
    public void start() {
        WLog.console(WLog.Type.INFO, "RestSignalingVerticle is running...");
        prepareMongo();
        prepareRouters();
    }
    // ----------------------------------------------------------------
    private void prepareMongo() {
        mMongoClient = MongoClient.createShared(vertx, new JsonObject().put("db_name", "db_wru_01"));
    }
    // ----------------------------------------------------------------
    private void prepareRouters() {
        HttpServer httpServer = vertx.createHttpServer();
        // ------------------------------------------------------------
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.post("/register").handler(routingContext -> {
            String msisdn = routingContext.request().getParam("msisdn");
            String verificationCode = generateVerificationCode();
            JsonObject credJson = new JsonObject().put("_id", msisdn).put("code", verificationCode);
            mMongoClient.save("credentials", credJson, res -> {
                if(res.succeeded()) {
                    callSuccessResponse(routingContext.response(), new JsonObject().put("done", true));
                } else {
                    callFailedResponse(routingContext.response());
                }
            });
        });
        // ------------------------------------------------------------
        router.post("/register/verify").handler(routingContext -> {
            String msisdn = routingContext.request().getFormAttribute("msisdn");
            String verificationCode = routingContext.request().getFormAttribute("code");
            JsonObject queryJson = new JsonObject().put("_id", msisdn);
            mMongoClient.findOne("credentials", queryJson, new JsonObject(), resFindCode -> {
                if(resFindCode.succeeded() && resFindCode.result() != null && resFindCode.result().getString("code").equals(verificationCode)) {
                    mMongoClient.removeDocument("credentials", queryJson, resRemoveCred -> {
                        if(resRemoveCred.succeeded()) {
                            String token = generateToken();
                            JsonObject tokenJson = new JsonObject().put("_id", token).put("msisdn", msisdn);
                            mMongoClient.save("tokens", tokenJson, tokenRes -> {
                                if(tokenRes.succeeded()) {
                                    mMongoClient.save("users", new JsonObject().put("_id", msisdn), resUserSave -> {
                                        if(resUserSave.succeeded()) {
                                            callSuccessResponse(routingContext.response(), new JsonObject().put("done", true).put("token", token));
                                        } else {
                                            callFailedResponse(routingContext.response());
                                        }
                                    });
                                } else {
                                    callFailedResponse(routingContext.response());
                                }
                            });
                        } else {
                            callFailedResponse(routingContext.response());
                        }
                    });
                } else {
                    callFailedResponse(routingContext.response());
                }
            });
        });
        // ------------------------------------------------------------
        router.route("/sync/contacts").handler(routingContext -> {
            String token = routingContext.request().getFormAttribute("token");
            extractMSISDNFromToken(token, msisdn -> {
                if(msisdn != null) {
                    String contacts = routingContext.request().getFormAttribute("contacts");
                    ArrayList<ContactJsonModel> contactJsonModels = new ArrayList<>();
                    contactJsonModels = Json.decodeValue(contacts, contactJsonModels.getClass());
                    ArrayList<String> wruUsers = new ArrayList<>();
                    final int[] contactInd = {0};
                    for (ContactJsonModel contactJsonModel : contactJsonModels) {
                        JsonObject contactJson = new JsonObject()
                                .put("_id", msisdn + "_" + contactJsonModel.getMsisdn())
                                .put("contact_msisdn", contactJsonModel.getMsisdn())
                                .put("contact_printName", contactJsonModel.getPrintName());
                        mMongoClient.save("contacts", contactJson, resContactSave -> {
                        });
                        ArrayList<ContactJsonModel> finalContactJsonModels = contactJsonModels;
                        mMongoClient.findOne("users", new JsonObject().put("_id", contactJsonModel.getMsisdn()), new JsonObject(), resFindUser -> {
                            if (resFindUser.succeeded() && resFindUser.result() != null) {
                                wruUsers.add(contactJsonModel.getMsisdn());
                            }
                            contactInd[0]++;
                            if(contactInd[0] == finalContactJsonModels.size()) {
                                callSuccessResponse(routingContext.response(), new JsonObject().put("done", true).put("wruUsers", wruUsers));
                            }
                        });
                    }
                } else {
                    callFailedResponse(routingContext.response());
                }
            });
        });
        // ------------------------------------------------------------
        httpServer.requestHandler(router::accept).listen(8080);
    }
    // ----------------------------------------------------------------
    private void extractMSISDNFromToken(String token, Handler<String> handler) {
        mMongoClient.findOne("tokens", new JsonObject().put("_id", token), new JsonObject(), res -> {
            if(res.succeeded() && res.result() != null) {
                handler.handle(res.result().getString("msisdn"));
            } else {
                handler.handle(null);
            }
        });
    }
    // ----------------------------------------------------------------
    private String generateToken() {
        return UUID.randomUUID().toString();
    }
    // ----------------------------------------------------------------
    private void callSuccessResponse(HttpServerResponse httpServerResponse, JsonObject resJson) {
        httpServerResponse.putHeader("content-type", "application/json");
        httpServerResponse.end(resJson.encodePrettily());
    }
    // ----------------------------------------------------------------
    private void callFailedResponse(HttpServerResponse httpServerResponse) {
        httpServerResponse.putHeader("content-type", "application/json");
        JsonObject resJson = new JsonObject().put("done", false);
        httpServerResponse.end(resJson.encodePrettily());
    }
    // ----------------------------------------------------------------
    private String generateVerificationCode() {
        return String.valueOf(Math.round(Math.random() * 9999) + 10000);
    }
    // ----------------------------------------------------------------
    public void stop() {
        WLog.console(WLog.Type.INFO, "RestSignalingVerticle is stopped!");
        if(mMongoClient != null) {
            mMongoClient.close();
        }
    }
    // ----------------------------------------------------------------
}
