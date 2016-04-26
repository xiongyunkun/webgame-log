package com.yuhe.szml.statics_modules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.yuhe.szml.db.UserInfoDB;
import com.yuhe.szml.db.log.CommonDB;
import com.yuhe.szml.utils.RegUtils;

import net.sf.json.JSONObject;

public class LogoutLog extends AbstractStaticsModule {

	private static final String[] LOG_COLS = { "Uid", "Urs", "OnTime", "Lv", "SceneUid", "X", "Y", "VipLevel", "IsVip",
			"Fighting", "Name", "Gold", "TotalGold", "Money", "Ip" };
	private static final String[] DB_COLS = { "HostID", "Uid", "Urs", "OnTime", "Lv", "SceneUid", "X", "Y", "VipLevel",
			"IsVip", "Fighting", "Name", "Time", "Gold", "TotalGold", "Money", "Ip" };
	private static String TBL_NAME = "tblLogoutLog";
	private static Map<String, String> DEFAULT_VALUES = new HashMap<String, String>() {
		private static final long serialVersionUID = 1L;

		{
			put("Gold", "0");
			put("TotalGold", "0");
			put("Money", "0");
		}
	};

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
						String value = RegUtils.getLogValue(message, col, DEFAULT_VALUES.getOrDefault(col, ""));
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
			CommonDB.batchInsertByDate(platformID, platformResult, DB_COLS, TBL_NAME);
			UserInfoDB.batchUpdate(platformID, platformResult);
		}
		return true;
	}

}
