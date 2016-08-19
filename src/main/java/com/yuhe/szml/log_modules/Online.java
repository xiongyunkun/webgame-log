package com.yuhe.szml.log_modules;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.DateUtils;

import com.yuhe.szml.db.statics.OnlineDB;
import com.yuhe.szml.utils.DateUtils2;

import net.sf.json.JSONObject;

public class Online extends AbstractLogModule {

	@Override
	public Map<String, List<Map<String, String>>> execute(List<String> logList, Map<String, String> hostMap) {
		Map<String, List<Map<String, String>>> platformResults = new HashMap<String, List<Map<String, String>>>();
		// logList.add("{\"time\":1457616416,\"num\":\"0\",\"hostid\":\"716\"}");
		for (String log : logList) {
			JSONObject json = JSONObject.fromObject(log);
			if (json != null) {
				String onlineNum = json.getString("num");
				String hostID = json.getString("hostid");
				String time = json.getString("time");
				int timestamp = Integer.parseInt(time);
				if(hostID.equals("2007")){
					//西班牙6服单独处理下，加上5个时区差
					timestamp += 18000;
				}
				time = DateUtils2.getFloorTime(timestamp);
				if (Integer.parseInt(onlineNum) > 0 && hostMap.containsKey(hostID)) {
					// 获得该HostID对应的所有平台列表
					String platformID = hostMap.get(hostID);
					Map<String, String> map = new HashMap<String, String>();
					map.put("PlatformID", platformID);
					map.put("HostID", hostID);
					map.put("Time", time);
					map.put("OnlineNum", onlineNum);
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
			OnlineDB db = new OnlineDB();
			db.batchInsert(platformID, platformResult);
			for(Map<String, String> result: platformResult){
				String time = result.get("Time");
				String hostID = result.get("HostID");
				//检查5分钟前数据是否正常
				checkPreNumErro(platformID, hostID, time);
			}
		}
		return platformResults;
	}
	
	/**
	 * 如果前10分钟有数据但是前5分钟没有数据，就认为前5分钟数据
	 * 有问题，直接用前10分钟的数据记录成前5分钟的数据
	 * @param platformID
	 * @param hostID
	 * @param time
	 * @return
	 */
	public boolean checkPreNumErro(String platformID, String hostID, String time){
		String timeFormat = "yyyy-MM-dd HH:mm:ss";
		String[] parsePatterns = { timeFormat };
		OnlineDB db = new OnlineDB();
		try {
			Date date = DateUtils.parseDate(time, parsePatterns);
			long timestamp = date.getTime();
			String pre5Time = DateFormatUtils.format(timestamp - 300000, timeFormat);
			String pre10Time = DateFormatUtils.format(timestamp - 600000, timeFormat);
			Map<String, String> options = new HashMap<String, String>();
			options.put("HostID", hostID);
			options.put("StartTime", pre10Time);
			options.put("EndTime", time);
			Map<String, Integer> numMap = db.queryNum(platformID, options);
			if(numMap.containsKey(pre10Time) && !numMap.containsKey(pre5Time)){
				//如果前10分钟有数据而前5分钟没有则修复前5分钟数据
				int pre10Num = numMap.get(pre10Time);
				Map<String, String> map = new HashMap<String, String>();
				map.put("PlatformID", platformID);
				map.put("HostID", hostID);
				map.put("Time", pre5Time);
				map.put("OnlineNum", Integer.toString(pre10Num));
				List<Map<String, String>> platformResult = new ArrayList<Map<String, String>>();
				platformResult.add(map);
				db.batchInsert(platformID, platformResult);
			}
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}

	@Override
	public String getStaticsIndex() {
		return "HistoryOnline";
	}

}
