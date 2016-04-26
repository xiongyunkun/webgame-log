package com.yuhe.szml.statics_modules;

import java.util.LinkedHashMap;
import java.util.Map;


public class StaticsIndexes {
	private static Map<String, AbstractStaticsModule> indexMap = new LinkedHashMap<String, AbstractStaticsModule>();
	static {
		//这里添加各个统计指标
		indexMap.put("addgold", new GoldLog()); //钻石日志
		indexMap.put("subgold", new GoldLog()); //钻石日志
		indexMap.put("addcreditgold", new GoldLog()); //加绑钻日志
		indexMap.put("subcreditgold", new GoldLog()); //减绑钻日志
		indexMap.put("addmoney", new MoneyLog()); //加金币日志
		indexMap.put("submoney", new MoneyLog()); //减金币日志
		indexMap.put("online", new Online()); //在线人数
		indexMap.put("addplayer", new AddPlayerLog()); //创角日志
		indexMap.put("login", new LoginLog()); //登陆日志
		indexMap.put("logout", new LogoutLog()); //退出日志
		indexMap.put("userlevelup", new LevelUpLog()); //升级日志
		indexMap.put("clientload", new ClientLoadLog());//登陆过程日志
		indexMap.put("chat", new ChatLog());//聊天日志
		indexMap.put("task", new TaskLog()); //任务日志
		indexMap.put("act", new ActLog()); //活动日志
		indexMap.put("shopbuy", new ShopBuyLog()); //商店日志
		indexMap.put("rename", new ReNameLog()); //改名日志
		indexMap.put("mountlevel", new MountLevelLog());//坐骑日志
		indexMap.put("additem", new ItemLog()); //物品日志
		indexMap.put("subitem", new ItemLog()); //物品日志
		indexMap.put("instance", new InstanceLog()); //副本日志
		indexMap.put("message", new MessageLog()); //消息日志
		indexMap.put("exp", new ExpLog()); //经验日志
		indexMap.put("goddoor", new GodDoorLog());//众神之门日志
		indexMap.put("pet", new PetLog()); //宠物日志
		indexMap.put("ban", new BanChatLog()); //封禁日志
		indexMap.put("petequip", new PetEquipLog()); //魔神装备日志
		indexMap.put("petbrand", new PetBrandLog()); //魔神辉印日志
		indexMap.put("teaminst", new TeamInstLog()); //诸神黄昏日志
		indexMap.put("petsoul", new PetSoulLog()); //宝石日志
		indexMap.put("endlessbattle", new EndlessBattleLog()); //方舟之战日志
	}
	/**
	 * 返回统计指标的Map
	 * @return
	 */
	public static Map<String, AbstractStaticsModule> GetIndexMap(){
		return indexMap;
	}
}
