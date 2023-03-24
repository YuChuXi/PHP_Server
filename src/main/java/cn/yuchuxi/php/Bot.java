package cn.yuchuxi.php;

import cn.nukkit.Server;
import cn.nukkit.scheduler.ServerScheduler;
import cn.nukkit.scheduler.Task;
import cn.nukkit.utils.Logger;
import cn.nukkit.utils.MainLogger;
import cn.nukkit.utils.TextFormat;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;


public abstract class Bot {
    protected final Logger logger; // logger
    protected final BotWebSocketClient botWSClient; // ws连接
    protected final ServerScheduler scheduler; // 任务调度器
    private final Gson gson; // json解析器
    private final URI botWSURI; // ws uri
    protected int reconnectTime; // 重连间隔 tick
    //private int reconnectTimes; // 重联次数 x
    
    public Bot(String sws, int reconnectTime) throws URISyntaxException { // 初始化
        this.logger = MainLogger.getLogger();
        this.gson = new Gson();
        this.botWSURI = new URI(sws);
        this.scheduler = Server.getInstance().getScheduler();
        this.reconnectTime = reconnectTime;
        this.botWSClient = new BotWebSocketClient(this.botWSURI, this);
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
            botWSClient.connect();
        }
    }
    
    public void reconnect() { // 连接
        if (!botWSClient.isOpen()) {
            botWSClient.reconnect();
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
    
    public abstract void groupMessageListener(String message, long group, long user, JsonObject msg);
    
    public abstract void privateMessageListener(String message, long user, JsonObject msg);
    
    
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
    private static final Pattern cqCodeMatcher = Pattern.compile("\\[CQ:(.+?),(.*?)]");
    private final Bot bot;
    private final ReConnectTask botReConnectTask; // 重连任务
    
    public BotWebSocketClient(URI serverUri, @NotNull Bot bot) {
        super(serverUri);
        this.bot = bot;
        this.botReConnectTask = new ReConnectTask(this);
    }
    
    @NotNull
    public static String cqCodeToString(String msg) { // cq码转字符串
        //("%[CQ:(.-),.-%]", "[%1]")("&amp;", "&")("&#91;", "[")("&#93;", "]")("&#44;", ",")
        return cqCodeMatcher.matcher(msg).replaceAll("[$1]").replace("&amp;", "&").replace("&#91;", "[")
                            .replace("&#93;", "]").replace("&#44;", ",");
    }
    
    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        bot.logger.info(TextFormat.GREEN + "WebSocket is successfully connected.");
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
                    bot.privateMessageListener(cqCodeToString(tree.get("raw_message").getAsString()),
                                               tree.getAsJsonObject("sender").get("user_id").getAsLong(), tree);
                } else if ("group".equals(msgType)) {
                    bot.groupMessageListener(cqCodeToString(tree.get("raw_message").getAsString()),
                                             tree.get("group_id").getAsLong(),
                                             tree.getAsJsonObject("sender").get("user_id").getAsLong(), tree);
                }
            }
        } catch (NullPointerException ignored) {
        }
        
        /*
        > JsonObject
{"post_type":"message","message_type":"group","time":1686316564,"self_id":911782632,"sub_type":"normal","font":0,
"message_seq":561886,"raw_message":"[CQ:image,file=2848fe0e469584ad62395b5df8b3fbce.image,subType=0,url=https://gchat
.qpic.cn/gchatpic_new/3524806669/595771358-2537271576-2848FE0E469584AD62395B5DF8B3FBCE/0?term=2\u0026amp;
is_origin=0]","sender":{"age":0,"area":"","card":"","level":"","nickname":"或许吧","role":"member","sex":"unknown",
"title":"暧昧","user_id":3524806669},"user_id":3524806669,"anonymous":null,"group_id":595771358,
"message":[{"type":"image","data":{"file":"2848fe0e469584ad62395b5df8b3fbce.image","subType":"0","url":"https://gchat
.qpic.cn/gchatpic_new/3524806669/595771358-2537271576-2848FE0E469584AD62395B5DF8B3FBCE/0?term=2\u0026is_origin=0"}}],
"message_id":-566650269}

null
{"post_type":"message","message_type":"group","time":1686316564,"self_id":911782632,"sub_type":"normal",
"message_id":264201878,"font":0,"message_seq":2472340,"raw_message":"[CQ:reply,id=1311078499][CQ:at,qq=1564186946]
[CQ:at,qq=1564186946]  别，直接寄给我吧","sender":{"age":0,"area":"","card":"人畜无害的索菲","level":"","nickname":"SophieTwilight",
"role":"member","sex":"unknown","title":"","user_id":2247589220},"anonymous":null,"group_id":885350936,
"message":[{"type":"reply","data":{"id":"1311078499"}},{"type":"at","data":{"qq":"1564186946"}},{"type":"text",
"data":{"text":" "}},{"type":"at","data":{"qq":"1564186946"}},{"type":"text","data":{"text":"  别，直接寄给我吧"}}],
"user_id":2247589220}


         */
        
    }
    
    @Override
    public void onClose(int i, String s, boolean b) {
        if (i == 1880) {
            bot.logger.info(TextFormat.GREEN + "WebSocket is successfully close.");
        } else {
            bot.logger.warning(TextFormat.RED + String.format("WebSocket is disconnected: %d, %s, %b", i, s, b));
            bot.logger.warning(
                TextFormat.RED + String.format("WebSocket will reconnect in %d ticks", bot.reconnectTime));
            bot.scheduler.scheduleDelayedTask(botReConnectTask, bot.reconnectTime);
        }
    }
    
    @Override
    public void onError(@NotNull Exception e) {
        bot.logger.error(String.format(TextFormat.RED + "WebSocket connection throws: %s", e.getMessage()));
        e.printStackTrace();
    }
}

class ReConnectTask extends Task { // 重连
    private final WebSocketClient wsc;
    //private final PluginLogger logger;
    
    public ReConnectTask(WebSocketClient wsc) {
        super();
        //this.logger = plugin.getLogger();
        this.wsc = wsc;
    }
    
    @Override
    public void onRun(int i) {
        if (!wsc.isOpen()) {
            wsc.reconnect();
        }
    }
}
