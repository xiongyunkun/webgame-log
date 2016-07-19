package com.yuhe.szml.log_modules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.yuhe.szml.db.log.CommonDB;
import com.yuhe.szml.utils.RegUtils;

import net.sf.json.JSONObject;

public class ItemLog extends AbstractLogModule {

	private static final String[] LOG_COLS = { "Uid", "Name", "OperationType", "Type", "Reason", "Memo", 
			"ItemName", "ItemType", "ShowType", "Amount", "Position"};
	private static final String[] DB_COLS = { "HostID", "Uid", "Name", "OperationType", "Type", "Reason", "Memo",
			 "ItemName", "ItemType", "ShowType", "Amount", "Position", "Time" };
	private static String TBL_NAME = "tblItemLog";

	@Override
	public Map<String, List<Map<String, String>>> execute(List<String> logList, Map<String, String> hostMap) {
		Map<String, List<Map<String, String>>> platformResults = new HashMap<String, List<Map<String, String>>>();
		for (String logStr : logList) {
			JSONObject json = JSONObject.fromObject(logStr);
			if (json != null) {
				String message = json.getString("message");
				String hostID = json.getString("hostid");
				if (!message.isEmpty() && !message.equals(" ") && hostMap.containsKey(hostID)) {
					Map<String, String> map = new HashMap<String, String>();
					map.put("HostID", hostID);
					String type = json.getString("type");
					String time = RegUtils.getLogTime(message);
					map.put("Time", time);
					for (String col : LOG_COLS) {
						String value = RegUtils.getLogValue(message, col, "");
						if(col.equals("OperationType")){
							if(type.equals("additem"))
								value = "1";
							else
								value = "2";
						}else if(col.equals("ItemName") && value.length()>20){
							value = value.substring(0, 20);
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
			CommonDB.batchInsertByDate(platformID, platformResult, DB_COLS, TBL_NAME);
		}
		return platformResults;
	}

	@Override
	public String getStaticsIndex() {
		// TODO Auto-generated method stub
		return null;
	}

}
