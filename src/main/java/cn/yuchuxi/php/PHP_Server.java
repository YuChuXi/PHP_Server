package cn.yuchuxi.php;

import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.TextFormat;

public class PHP_Server extends PluginBase {
    @Override
    public void onEnable() {
        this.getLogger().info(TextFormat.RED + "My first plugin enabled!");
    }

    @Override
    public void onLoad() {
        this.getLogger().info(TextFormat.RED + "PHP_Server Loaded.");
    }
}
