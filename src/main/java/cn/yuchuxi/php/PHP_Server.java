package cn.yuchuxi.php;

import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.plugin.PluginLogger;
import com.google.gson.JsonObject;

import java.net.URISyntaxException;


public class PHP_Server extends PluginBase implements Listener {
    private static PHP_Server instance;
    private PluginLogger logger;
    private Bot bot;

    static public PHP_Server getInstance() { // 用于获取该插件状态
        return PHP_Server.instance;
    }

    public Bot getBot() {
        return this.bot;
    }

    @Override
    public void onLoad() {
        this.logger = this.getLogger(); // 保存logger及self
        PHP_Server.instance = this;
        logger.info("PHP_Server Loaded.");

        try {
            this.bot = new Bot("ws://127.0.0.1:47862", this.logger) { // 初始化bot
                @Override
                public void groupMessageL(String message, long group, long user, JsonObject msg) { // 收到群消息

                }

                @Override
                public void privateMessageL(String message, long user, JsonObject msg) { // 收到私聊消息

                }
            };
            bot.connectServer(); // 连接服务器

        } catch (URISyntaxException e) { // ws服务器地址无效？
            logger.error("The URI cannot be generated, please check the Bot configuration");
        }
    }

    @Override
    public void onEnable() {
        bot.sendGroupMessage("测试服已开启", 879578995L, "onEnable");


        //RegisteredListener chatListener = new RegisteredListener(listener, executor, priority, this, ignoreCancelled, timing);
        //PlayerChatEvent.getHandlers().register(chatListener);
    }

    @Override
    public void onDisable() {
        //插件关闭
    }

    //@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true) // 监听测试
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        logger.info("fvjhfjhfvjhvfhjdsvfhdsvfhjdvhsvfjhvfdjhvdhsvdvfhdvhsvfhdv");
        e.getPlayer().sendMessage("你好 " + e.getPlayer());
    }
}

/*
class ChatListener implements Listener {
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onChat() {

    }
}

 */

