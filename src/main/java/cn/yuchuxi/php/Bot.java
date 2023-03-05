package cn.yuchuxi.php;

import cn.nukkit.plugin.Plugin;
import cn.nukkit.plugin.PluginLogger;
import cn.nukkit.scheduler.PluginTask;
import cn.nukkit.scheduler.ServerScheduler;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;


public abstract class Bot<T extends Plugin> {
    private final Gson gson; // json解析器
    private final T plugin; // 上级插件
    private final PluginLogger logger; // logger
    private final URI botWSURI; // ws uri
    private final WebSocketClient botWSClient; // ws连接
    private final ReConnectTask<T> botReConnectTask; // 重连任务
    private final ServerScheduler scheduler; // 任务调度器

    private int reconnectTime; // 重连间隔 tick
    //private int reconnectTimes; // 重联次数 x

    public Bot(String sws, int reconnectTime, T plugin) throws URISyntaxException { // 初始化
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.gson = new Gson();
        this.botWSURI = new URI(sws);
        this.botReConnectTask = new ReConnectTask<>(plugin, this);
        this.scheduler = plugin.getServer().getScheduler();
        this.reconnectTime = reconnectTime;

        this.botWSClient = new WebSocketClient(this.botWSURI) {
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
                } catch (NullPointerException point) {

                }

            }

            @Override
            public void onClose(int i, String s, boolean b) {
                logger.warning("The WebSocket server is disconnected");
                logger.warning("The WebSocket will reconnect 在" + reconnectTime + "Tick ");
                //if (botRCTHandler!=null){
                //    if(botRCTHandler.isCancelled()){
                //        botRCTHandler.;
                //    }
                //}else {
                //botRCTHandler =
                scheduler.scheduleDelayedTask(botReConnectTask, reconnectTime);
                //}
            }

            @Override
            public void onError(Exception e) {
                logger.error("WebSocket connection throws: " + e.getMessage());
                e.printStackTrace();


            }
        };
    }

    public T getPlugin() {
        return plugin;
    }

    public int getReconnectTime() {
        return reconnectTime;
    }

    public void setReconnectTime(int reconnectTime) {
        this.reconnectTime = reconnectTime;
    }

    public WebSocketClient getBotWSClient() {
        return this.botWSClient;
    }

    public URI getBotWSURI() {
        return botWSURI;
    }

    public boolean connect() { // 连接
        if (!botWSClient.isOpen()) {
            this.botWSClient.connect();
        }
        return true;
    }

    public boolean reconnect() { // 连接
        if (!botWSClient.isOpen()) {
            this.botWSClient.reconnect();
        }
        return true;
    }

    public boolean sendMessage(String message, long user, String echo) { // 发消息
        if (botWSClient.isOpen()) {
            toGoCQ msg = new toGoCQ();
            msg.action = "send_private_msg";
            msg.echo = echo;
            msg.params.auto_escape = false;
            msg.params.user_id = user;
            msg.params.message = message;
            botWSClient.send(gson.toJson(msg));
            return true;
        }
        return false;
    }

    public boolean sendGroupMessage(String message, long group, String echo) { // 发群消息
        if (botWSClient.isOpen()) {
            toGoCQ msg = new toGoCQ();
            msg.action = "send_group_msg";
            msg.echo = echo;
            msg.params = new toGoCQ.Params();
            msg.params.auto_escape = false;
            msg.params.group_id = group;
            msg.params.message = message;

            //logger.info(gson.toJson(msg));
            botWSClient.send(gson.toJson(msg));
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

class ReConnectTask<T extends Plugin> extends PluginTask {
    private final Bot<T> bot;
    private final T plugin;
    private final PluginLogger logger;

    public ReConnectTask(T plugin, Bot<T> bot) {
        super(plugin);
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.bot = bot;
    }

    @Override
    public void onRun(int i) {
        bot.reconnect();
    }
}
