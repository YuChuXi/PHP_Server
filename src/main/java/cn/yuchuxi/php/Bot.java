package cn.yuchuxi.php;

import cn.nukkit.plugin.PluginLogger;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;


public abstract class Bot {
    public WebSocketClient wsc;
    PluginLogger logger;
    Gson gson;
    URI wsUri;

    public Bot(String sws, PluginLogger logger) throws URISyntaxException { // 初始化
        this.logger = PHP_Server.getInstance().getLogger();
        this.gson = new Gson();
        this.wsUri = new URI(sws);

        this.wsc = new WebSocketClient(this.wsUri) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                logger.info("The WebSocket server is successfully connected");
            }

            @Override
            public void onMessage(String s) {
                //logger.info(s);
                JsonObject tree = JsonParser.parseString(s).getAsJsonObject();
                try {
                    String type = tree.get("post_type").getAsString();
                    if ("message".equals(type)) {
                        //logger.info(message.message_type + ":" + message.sender.nickname + ":" + message.message);
                        String msgType = tree.get("message_type").getAsString();
                        if ("private".equals(msgType)) {
                            privateMessageL(tree.get("message").getAsString(),
                                    tree.getAsJsonObject("sender").get("user_id").getAsLong(), tree);
                        } else if ("group".equals(msgType)) {
                            groupMessageL(tree.get("message").getAsString(),
                                    tree.get("group_id").getAsLong(),
                                    tree.getAsJsonObject("sender").get("user_id").getAsLong(), tree);
                        }
                    }
                } catch (NullPointerException pointe) {

                }

            }

            @Override
            public void onClose(int i, String s, boolean b) {
                logger.warning("The WebSocket server is disconnected");
            }

            @Override
            public void onError(Exception e) {
                logger.error("WebSocket connection throws: " + e.getMessage());
                e.printStackTrace();
            }
        };
    }

    public boolean connectServer() {
        this.wsc.connect();
        return true;
    }

    public boolean sendMessage(String message, long user, String echo) { // 发消息
        if (wsc.isOpen()) {
            toGoCQ msg = new toGoCQ();
            msg.action = "send_private_msg";
            msg.echo = echo;
            msg.params.auto_escape = false;
            msg.params.user_id = user;
            msg.params.message = message;
            wsc.send(gson.toJson(msg));
            return true;
        }
        return false;
    }

    public boolean sendGroupMessage(String message, long group, String echo) { // 发群消息
        if (wsc.isOpen()) {
            toGoCQ msg = new toGoCQ();
            msg.action = "send_group_msg";
            msg.echo = echo;
            msg.params = new toGoCQ.Params();
            msg.params.auto_escape = false;
            msg.params.group_id = group;
            msg.params.message = message;

            //logger.info(gson.toJson(msg));
            wsc.send(gson.toJson(msg));
            return true;
        }
        return false;
    }

    public abstract void groupMessageL(String message, long group, long user, JsonObject msg);

    public abstract void privateMessageL(String message, long user, JsonObject msg);


    public static class toGoCQ {
        public String action;
        public Params params;
        public String echo;

        public static class Params {
            public long group_id;
            public long user_id;
            public String message;
            public boolean auto_escape;
        }
    }
}
