package com.yuhe.szml.log_modules;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.yuhe.szml.db.log.CommonDB;
import com.yuhe.szml.utils.RegUtils;

import net.sf.json.JSONObject;

public class BanChatLog extends AbstractLogModule {

	private static final String[] LOG_COLS = { "Uid", "Urs", "Name", "OperationType", "StartTime", "BanTime", "Reason",
			"ExtInfo" };
	private static final String[] DB_COLS = { "HostID", "Uid", "Name", "Operator", "OperationType", "BanStartTime", "BanEndTime", 
			"Reason", "Time", "ExtInfo" };
	private static String TBL_NAME = "tblBanLog";
	private static Map<String, String> OPERATION_MAP = new HashMap<String, String>() {
		private static final long serialVersionUID = 1L;
		{
			put("chat", "1");
			put("unbanchat", "2");
			put("banlogin", "3");
			put("unbanlogin", "4");
			put("chat_ban_ip", "5");
		}
	};

	@Override
	public Map<String, List<Map<String, String>>> execute(List<String> logList, Map<String, String> hostMap) {
		Map<String, List<Map<String, String>>> platformResults = new HashMap<String, List<Map<String, String>>>();
		SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
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
						try {
							if (col.equals("StartTime")) {
								String timeStr = timeFormat.format(Long.parseLong(value + "000"));
								map.put("BanStartTime", timeStr);
							} else if (col.equals("BanTime")) {
								long endTime = Long.parseLong(value) + Long.parseLong(map.get("StartTime"));
								String timeStr = timeFormat.format(endTime * 1000);
								map.put("BanEndTime", timeStr);
							}
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						if(col.equals("OperationType")){
							value = OPERATION_MAP.getOrDefault(value, "1");
						}
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
			CommonDB.batchInsert(platformID, platformResult, DB_COLS, TBL_NAME);
		}
		return platformResults;
	}

	@Override
	public String getStaticsIndex() {
		// TODO Auto-generated method stub
		return null;
	}

}
