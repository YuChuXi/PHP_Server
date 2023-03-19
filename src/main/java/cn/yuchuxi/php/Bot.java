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
import java.util.regex.Pattern;


public abstract class Bot<T extends Plugin> {
    protected final T plugin; // 上级插件
    protected final PluginLogger logger; // logger
    protected final BotWebSocketClient botWSClient; // ws连接
    protected final ServerScheduler scheduler; // 任务调度器
    private final Gson gson; // json解析器
    private final URI botWSURI; // ws uri
    protected int reconnectTime; // 重连间隔 tick
    //private int reconnectTimes; // 重联次数 x
    private static final Pattern cqCodeMatcher=Pattern.compile("\\[CQ:(.+?),(.*?)]");

    public Bot(String sws, int reconnectTime, T plugin) throws URISyntaxException { // 初始化
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.gson = new Gson();
        this.botWSURI = new URI(sws);
        this.scheduler = plugin.getServer().getScheduler();
        this.reconnectTime = reconnectTime;
        this.botWSClient = new BotWebSocketClient(this.botWSURI, this);
    }

    public static String cqCodeToString(String msg) { // cq码转字符串
        //("%[CQ:(.-),.-%]", "[%1]")("&amp;", "&")("&#91;", "[")("&#93;", "]")("&#44;", ",")
        msg = cqCodeMatcher.matcher(msg).replaceAll("[$1]");
        return msg.replace("&amp;", "&").replace("&#91;", "[").replace("&#93;", "]").replace("&#44;", ",");
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

    public URI getBotWSURI() {
        return botWSURI;
    }

    public void close(int code, String s) {
        if (botWSClient.isOpen()) {
            botWSClient.closeConnection(code, s);
        }
    }

    public void connect() { // 连接
        if (!botWSClient.isOpen()) {
            this.botWSClient.connect();
        }
    }

    public void reconnect() { // 连接
        if (!botWSClient.isOpen()) {
            this.botWSClient.reconnect();
        }
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


    public static class toGoCQ { // cq包序列化类
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

class BotWebSocketClient extends WebSocketClient {
    private final Bot bot;
    private final ReConnectTask botReConnectTask; // 重连任务

    public BotWebSocketClient(URI serverUri, Bot bot) {
        super(serverUri);
        this.bot = bot;
        this.botReConnectTask = new ReConnectTask<>(bot.getPlugin(), this);
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        bot.logger.info("The WebSocket server is successfully connected");
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
                    bot.privateMessageL(tree.get("message").getAsString(), tree.getAsJsonObject("sender").get(
                            "user_id").getAsLong(), tree);
                } else if ("group".equals(msgType)) {
                    bot.groupMessageL(tree.get("message").getAsString(), tree.get("group_id").getAsLong(),
                            tree.getAsJsonObject("sender").get("user_id").getAsLong(), tree);
                }
            }
        } catch (NullPointerException point) {}

    }

    @Override
    public void onClose(int i, String s, boolean b) {
        if (i != 1880) {

            bot.logger.warning(String.format("The WebSocket server is disconnected: %d, %s, %b", i, s, b));
            bot.logger.warning(String.format("The WebSocket will reconnect in %d ticks", bot.reconnectTime));
            bot.scheduler.scheduleDelayedTask(this.botReConnectTask, bot.reconnectTime);
        }
    }

    @Override
    public void onError(Exception e) {
        bot.logger.error(String.format("WebSocket connection throws: %s", e.getMessage()));
        e.printStackTrace();

    }
}

class ReConnectTask<T extends Plugin> extends PluginTask { // 重连
    private final WebSocketClient wsc;
    private final T plugin;
    private final PluginLogger logger;

    public ReConnectTask(T plugin, WebSocketClient wsc) {
        super(plugin);
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.wsc = wsc;
    }

    @Override
    public void onRun(int i) {
        if (!wsc.isOpen()) {
            wsc.reconnect();
        }
    }
}
