package cn.yuchuxi.php;

import cn.nukkit.Server;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.plugin.PluginLogger;
import cn.nukkit.utils.Config;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.net.URISyntaxException;


public class PHPServer extends PluginBase {
    private static PHPServer instance;
    private PluginLogger logger;
    private Bot bot;
    private Config config;

    static public PHPServer getInstance() { // 用于获取该插件状态
        return PHPServer.instance;
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
        PHPServer.instance = this;
        this.saveDefaultConfig();
        logger.info("Loaded");
    }

    @Override
    public void onEnable() {
        this.config = new Config(this.getDataFolder() + "/config.yml", Config.YAML); // 处理配置文件
        config.set("server.startimes", config.getInt("server.startimes") + 1);
        config.save();
        try {
            this.bot = new Bot(config.getString("bot.ws"), config.getInt("bot.reconnectimes")) { // 初始化bot
                @Override
                public void groupMessageL(String message, long group, long user, JsonObject msg) { // 收到群消息
                    if (message.charAt(0) == '#' || message.charAt(0) == '>') {
                        runRemoteCommand(message, group, user);
                        logger.info(String.format("[RemoteCommand] Sender: %d, Command: %s", user, message));
                        return;
                    }
                    if (group == config.getLong("bot.group.repost")) {
                        Server.getInstance().broadcastMessage(String.format("[§6聊天转发§f] %s: %s", msg.getAsJsonObject(
                                "sender").get("nickname").getAsString(), message));
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
        bot.sendGroupMessage(String.format("[%s] 关闭～", config.getString("server.name")),
                config.getLong("bot.group" + ".admin"), "onServerStarted");
        bot.close(1880, "onDisable");
        logger.info("Disabled");
    }

    public void runRemoteCommand(@NotNull String cmd, long group, long user) {
        int head = cmd.charAt(1);
        String command = cmd.substring(2);
        switch (head) {
            case '/' -> // 世界命令
                    RunCommand.runWorldCommand(command, new RunCommand.Permission(user), new CallBack() {
                        @Override
                        public void run(boolean success, String head, String message) {
                            bot.sendGroupMessage(String.format("[%s]\n%s", head, message), group, "onCommandBack");
                        }
                    });
            case '>' -> // JShell命令
                    RunCommand.runJShellCommand(command, new RunCommand.Permission(user), new CallBack() {
                        @Override
                        public void run(boolean success, String head, String message) {
                            bot.sendGroupMessage(String.format("[%s]\n%s", head, message), group, "onCommandBack");
                        }
                    });
            case '$' -> // 服务主机命令
                    RunCommand.runShell(command, new RunCommand.Permission(user), new CallBack() {
                        @Override
                        public void run(boolean success, String head, String message) {
                            bot.sendGroupMessage(String.format("[%s]\n%s", head, message), group, "onCommandBack");
                        }
                    });
            default -> {
                if ("#查服".equals(cmd) && (config.getLong("bot.group.main") == group || config.getLong("bot.group" +
                        ".repost") == group || config.getLong("bot.group.admin") == group)) {
                    //runRemoteCommand("#/status", group, config.getLong("bot.user.root"));
                    runRemoteCommand("#/list", group, config.getLong("bot.user.root"));
                } else {
                    bot.sendGroupMessage(String.format("[? head]\n%d", head), group, "onCommandBack");
                }
            }
        }
    }
}

