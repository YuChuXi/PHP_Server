package cn.yuchuxi.php;

import cn.nukkit.utils.MainLogger;
import cn.nukkit.utils.TextFormat;
import party.iroiro.luajava.lua54.Lua54;

public class LuaEngine {
	Lua54 L;
	MainLogger logger;
	
	public LuaEngine() {
		L = new Lua54();
		logger = MainLogger.getLogger();
		
		//L.run("print(_ENG)");
	}
	
	public void enable() {
		L.push("LuaJava");
		L.setGlobal("_ENG");
		L.openLibraries();
		logger.info(TextFormat.GREEN + "LuaEngine enabled");
	}
	
	public Lua54 getL() {
		return L;
	}
}
