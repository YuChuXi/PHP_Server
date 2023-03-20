package cn.yuchuxi.php;

import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerChatEvent;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.event.server.ServerStartedEvent;
import cn.nukkit.plugin.PluginLogger;
import cn.nukkit.utils.Config;
import org.jetbrains.annotations.NotNull;

public class EventsListener implements Listener {
    PluginLogger logger;
    Config config;
    Bot bot;

    EventsListener() {
        PHPServer plugin = PHPServer.getInstance();
        this.logger = plugin.getLogger();
        this.bot = plugin.getBot();
        this.config = plugin.getConfig();
    }

    @EventHandler(priority = EventPriority.HIGH) // 服务器启动完毕
    public void onServerStarted(ServerStartedEvent e) {
        bot.sendGroupMessage(String.format("[%s] 启动！", config.getString("server.name")),
                config.getLong("bot.group" + ".admin"), "onServerStarted");
    }


    @EventHandler(priority = EventPriority.HIGH) // 玩家加入游戏
    public void onPlayerJoin(@NotNull PlayerJoinEvent e) {
        bot.sendGroupMessage(String.format("[%s] %s 加入了游戏", config.getString("server.name"), e.getPlayer().getName())
                , config.getLong("bot.group.repost"), "onPlayerJoin");
    }

    @EventHandler(priority = EventPriority.HIGH) // 玩家退出游戏
    public void onPlayerQuit(@NotNull PlayerQuitEvent e) {
        bot.sendGroupMessage(String.format("[%s] %s 退出了游戏", config.getString("server.name"), e.getPlayer().getName())
                , config.getLong("bot.group.repost"), "onPlayerQuit");
    }


    @EventHandler(priority = EventPriority.HIGH) // 玩家发送消息
    public void onPlayerChat(@NotNull PlayerChatEvent e) {
        bot.sendGroupMessage(String.format("[%s] %s: %s", config.getString("server.name"), e.getPlayer().getName(),
                e.getMessage()), config.getLong("bot.group.repost"), "onPlayerChat");
    }


}


