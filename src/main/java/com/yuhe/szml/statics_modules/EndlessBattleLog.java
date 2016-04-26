package com.yuhe.szml.statics_modules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.yuhe.szml.db.log.CommonDB;
import com.yuhe.szml.utils.RegUtils;

import net.sf.json.JSONObject;

public class EndlessBattleLog extends AbstractStaticsModule {

	private static final String[] LOG_COLS = { "Uid", "Name", "ExitType", "CurStage", "Rewards" };
	private static final String[] DB_COLS = { "HostID", "Uid", "Name", "ExitType", "CurStage", "Rewards", "Time" };
	private static String TBL_NAME = "tblEndlessBattlelLog";
	@Override
	public boolean execute(List<String> logList, Map<String, String> hostMap) {
		Map<String, List<Map<String, String>>> platformResults = new HashMap<String, List<Map<String, String>>>();
		for (String logStr : logList) {
			JSONObject json = JSONObject.fromObject(logStr);
			if (json != null) {
				String message = json.getString("message");
				String hostID = json.getString("hostid");
				if (!message.isEmpty() && !message.equals(" ") && hostMap.containsKey(hostID)) {
					Map<String, String> map = new HashMap<String, String>();
					map.put("HostID", hostID);
					String time = RegUtils.getLogTime(message);
					map.put("Time", time);
					for (String col : LOG_COLS) {
						String value = RegUtils.getLogValue(message, col, "");
						map.put(col, value);
					}
					String platformID = hostMap.get(hostID);
					List<Map<String, String>> platformResult = platformResults.get(platformID);
					if (platformResult == null)
						platformResult = new ArrayList<Map<String, String>>();
					platformResult.add(map);
					platformResults.put(platformID, platformResult);
				}
			}
		}
		// 插入数据库
		Iterator<String> it = platformResults.keySet().iterator();
		while (it.hasNext()) {
			String platformID = it.next();
			List<Map<String, String>> platformResult = platformResults.get(platformID);
			CommonDB.batchInsertByDate(platformID, platformResult,DB_COLS, TBL_NAME);
		}
		return true;
	}

}
