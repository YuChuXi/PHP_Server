package cn.yuchuxi.php;

import cn.nukkit.Server;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.plugin.PluginLogger;
import cn.nukkit.utils.Config;
import com.google.gson.JsonObject;

import java.net.URISyntaxException;


public class PHP_Server extends PluginBase {
    private static PHP_Server instance;
    private PluginLogger logger;
    private Bot bot;
    private Config config;
    private Server server;

    static public PHP_Server getInstance() { // 用于获取该插件状态
        return PHP_Server.instance;
    }

    public Bot getBot() {
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
        this.config = new Config(this.getDataFolder() + "/config.yml", Config.YAML); // 处理配置文件
        config.set("server.startimes", config.getInt("server.startimes") + 1);
        config.save();

        try {
            this.bot = new Bot<PHP_Server>(config.getString("bot.ws"), config.getInt("bot.reconnectimes"), this) { // 初始化bot
                @Override
                public void groupMessageL(String message, long group, long user, JsonObject msg) { // 收到群消息
                    if (group == config.getLong("bot.group.repost")) {
                        server.broadcastMessage("[§6聊天转发§f]" + msg.getAsJsonObject("sender").
                                get("nickname").getAsString() + "：" + message);
                    }
                }

                @Override
                public void privateMessageL(String message, long user, JsonObject msg) { // 收到私聊消息

                }
            };
            bot.connect(); // 连接服务器

        } catch (URISyntaxException e) { // ws服务器地址无效？
            logger.error("The URI cannot be generated, please check the Bot configuration");
        }
        logger.info("PHP_Server Loaded.");
    }

    @Override
    public void onEnable() {
        bot.sendGroupMessage("测试服已开启", config.getLong("bot.group.main"), "onEnable");
        this.getServer().getPluginManager().registerEvents(new EventsListener(), this);

        //RegisteredListener chatListener = new RegisteredListener(listener, executor, priority, this, ignoreCancelled, timing);
        //PlayerChatEvent.getHandlers().register(chatListener);
    }

    @Override
    public void onDisable() {
        //插件关闭
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

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false) // 监听测试
    public void onJoin(PlayerJoinEvent e) {
        bot.sendGroupMessage("[" + config.getString("server.name") + "] " + e.getPlayer().getName() + " 加入了游戏",
                config.getLong("bot.group.repost"), "onPlayerJoin");
    }

}


