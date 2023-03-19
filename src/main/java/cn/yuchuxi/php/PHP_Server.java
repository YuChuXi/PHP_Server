package cn.yuchuxi.php;

import cn.nukkit.Server;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerChatEvent;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.event.server.ServerStartedEvent;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.plugin.PluginLogger;
import cn.nukkit.utils.Config;
import com.google.gson.JsonObject;

import java.net.URISyntaxException;


public class PHP_Server extends PluginBase {
    private static PHP_Server instance;
    private PluginLogger logger;
    private Bot<PHP_Server> bot;
    private Config config;
    private Server server;

    static public PHP_Server getInstance() { // 用于获取该插件状态
        return PHP_Server.instance;
    }

    public Bot<PHP_Server> getBot() {
        return this.bot;
    }

    public Config getConfig() {
        return this.config;
    }

    @Override
    public void onLoad() {
        this.logger = this.getLogger(); // 保存logger及self
        this.server = this.getServer();
        PHP_Server.instance = this;
        this.saveDefaultConfig();
        logger.info("Loaded");
    }

    @Override
    public void onEnable() {
        this.config = new Config(this.getDataFolder() + "/config.yml", Config.YAML); // 处理配置文件
        config.set("server.startimes", config.getInt("server.startimes") + 1);
        config.save();
        try {
            this.bot = new Bot<>(config.getString("bot.ws"), config.getInt("bot.reconnectimes"), this) { // 初始化bot
                @Override
                public void groupMessageL(String message, long group, long user, JsonObject msg) { // 收到群消息
                    if (group == config.getLong("bot.group.repost")) {
                        server.broadcastMessage(String.format("[§6聊天转发§f] %s: %s", msg.getAsJsonObject("sender").get(
                                "nickname").getAsString(), Bot.cqCodeToString(message)));
                    }
                }

                @Override
                public void privateMessageL(String message, long user, JsonObject msg) { // 收到私聊消息
                }
            };

            if (config.getBoolean("bot.enable", false)) {
                bot.connect(); // 连接服务器
            }
        } catch (URISyntaxException e) { // ws服务器地址无效？
            logger.error("The URI cannot be generated, please check the Bot configuration");
        }

        this.getServer().getPluginManager().registerEvents(new EventsListener(), this); // 设置监听
        logger.info("Enabled");
        //RegisteredListener chatListener = new RegisteredListener(listener, executor, priority, this,
        // ignoreCancelled, timing);
        //PlayerChatEvent.getHandlers().register(chatListener);
    }

    @Override
    public void onDisable() {
        //插件关闭
        bot.sendGroupMessage(String.format("[%s] 关闭～", config.getString("server.name")), config.getLong("bot.group" +
                ".main"), "onServerStarted");
        bot.close(1880, "onDisable");
        logger.info("Disabled");
    }


}


class EventsListener implements Listener {
    PHP_Server plugin;
    PluginLogger logger;
    Config config;
    Bot<PHP_Server> bot;

    EventsListener() {
        this.plugin = PHP_Server.getInstance();
        this.logger = plugin.getLogger();
        this.bot = plugin.getBot();
        this.config = plugin.getConfig();
    }

    @EventHandler(priority = EventPriority.HIGH) // 服务器启动完毕
    public void onServerStarted(ServerStartedEvent e) {
        bot.sendGroupMessage(String.format("[%s] 启动！", config.getString("server.name")), config.getLong("bot.group" +
                ".main"), "onServerStarted");
    }


    @EventHandler(priority = EventPriority.HIGH) // 玩家加入游戏
    public void onPlayerJoin(PlayerJoinEvent e) {
        bot.sendGroupMessage(String.format("[%s] %s 加入了游戏", config.getString("server.name"), e.getPlayer().getName())
                , config.getLong("bot.group.repost"), "onPlayerJoin");
    }

    @EventHandler(priority = EventPriority.HIGH) // 玩家退出游戏
    public void onPlayerQuit(PlayerQuitEvent e) {
        bot.sendGroupMessage(String.format("[%s] %s 退出了游戏", config.getString("server.name"), e.getPlayer().getName())
                , config.getLong("bot.group.repost"), "onPlayerQuit");
    }


    @EventHandler(priority = EventPriority.HIGH) // 玩家发送消息
    public void onPlayerChat(PlayerChatEvent e) {
        bot.sendGroupMessage(String.format("[%s] %s: %s", config.getString("server.name"), e.getPlayer().getName(),
                e.getMessage()), config.getLong("bot.group.repost"), "onPlayerChat");
    }


}


