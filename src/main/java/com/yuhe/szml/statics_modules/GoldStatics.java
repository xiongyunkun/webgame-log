package com.yuhe.szml.statics_modules;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.yuhe.szml.db.DBManager;
import com.yuhe.szml.db.log.CommonDB;
import com.yuhe.szml.db.statics.GoldDB;
import com.yuhe.szml.db.statics.PayDayStaticsDB;
import com.yuhe.szml.utils.DateUtils2;

/**
 * 钻石统计
 * 
 * @author xiongyunkun
 *
 */
public class GoldStatics extends AbstractStaticsModule {
	// 记录钻石消费信息
	// 格式:Map<HostID, Map<Date, Map<GoldType, Map<StaticsType, Map<Channel,
	// Set<Uid>>>>>
	private Map<String, Map<String, Map<String, Map<String, Map<String, Set<String>>>>>> UidSets = new HashMap<String, Map<String, Map<String, Map<String, Map<String, Set<String>>>>>>();
	
	//记录钻石总计信息
	//格式：<HostID, <Date, <StaticsType, Number>>>
	private Map<String, Map<String, Map<String, Integer>>> DayNumMap = new HashMap<String, Map<String, Map<String, Integer>>>();
		
	@Override
	public boolean execute(Map<String, List<Map<String, String>>> platformResults) {
		Iterator<String> pIt = platformResults.keySet().iterator();
		while (pIt.hasNext()) {
			String platformID = pIt.next();
			List<Map<String, String>> platformResult = platformResults.get(platformID);
			for (Map<String, String> logMap : platformResult) {
				String hostID = logMap.get("HostID");
				String uid = logMap.get("Uid");
				String reason = logMap.get("Reason");
				String goldType = logMap.get("GoldType");
				String changes = logMap.get("Changes");
				int changesInt = Integer.parseInt(changes);
				String staticsType = changesInt < 0 ? "1" : "2";
				String time = logMap.get("Time");
				String[] times = StringUtils.split(time, " ");
				String date = times[0];
				Map<String, Map<String, Map<String, Map<String, Set<String>>>>> hostResults = UidSets.get(hostID);
				if (hostResults == null) {
					// 先从数据库中加载
					hostResults = loadFromDB(platformID, hostID, date, time);
				}
				Map<String, Map<String, Map<String, Set<String>>>> dateResults = hostResults.get(date);
				if (dateResults == null) {
					dateResults = new HashMap<String, Map<String, Map<String, Set<String>>>>();
					hostResults.put(date, dateResults);
				}
				Map<String, Map<String, Set<String>>> goldResults = dateResults.get(goldType);
				if (goldResults == null) {
					goldResults = new HashMap<String, Map<String, Set<String>>>();
					dateResults.put(goldType, goldResults);
				}
				Map<String, Set<String>> staticsResults = goldResults.get(staticsType);
				if (staticsResults == null) {
					staticsResults = new HashMap<String, Set<String>>();
					goldResults.put(staticsType, staticsResults);
				}
				Set<String> uids = staticsResults.get(reason);
				if (uids == null) {
					uids = new HashSet<String>();
					staticsResults.put(reason, uids);
				}
				uids.add(uid);
				Map<String, String> values = new HashMap<String, String>();
				values.put("Value", changes);
				values.put("Uids", StringUtils.join(uids, "','"));
				values.put("consumeNum", "1");
				GoldDB.batchInsert(platformID, hostID, date, reason, goldType, staticsType, values);
				staticsTotalGold(platformID, hostID, date, goldType, staticsType, Integer.parseInt(values.get("Value")));
			}
		}
		return true;
	}

	/**
	 * 从钻石日志中加载玩家uid
	 * 
	 * @param platformID
	 * @param hostID
	 * @param date
	 */
	private Map<String, Map<String, Map<String, Map<String, Set<String>>>>> loadFromDB(String platformID, String hostID,
			String date, String endTime) {
		Map<String, Map<String, Map<String, Set<String>>>> hostResults = new HashMap<String, Map<String, Map<String, Set<String>>>>();
		String tblName = platformID + "_log.tblGoldLog_" + date.replace("-", "");
		List<String> options = new ArrayList<String>();
		options.add("HostID = '" + hostID + "'");
		options.add("Time >= '" + date + " 00:00:00'");
		options.add("Time < '" + endTime + "'");
		Connection conn = DBManager.getConn();
		ResultSet resultSet = CommonDB.query(conn, tblName, options);
		try {
			while (resultSet.next()) {
				String uid = resultSet.getString("Uid");
				String reason = resultSet.getString("Reason");
				String goldType = resultSet.getString("GoldType");
				int changes = resultSet.getInt("Changes");
				String staticsType = changes < 0 ? "1" : "2";
				Map<String, Map<String, Set<String>>> typeResults = hostResults.get(goldType);
				if (typeResults == null) {
					typeResults = new HashMap<String, Map<String, Set<String>>>();
					hostResults.put(goldType, typeResults);
				}
				Map<String, Set<String>> staticsResults = typeResults.get(staticsType);
				if (staticsResults == null) {
					staticsResults = new HashMap<String, Set<String>>();
					typeResults.put(staticsType, staticsResults);
				}
				Set<String> uids = staticsResults.get(reason);
				if (uids == null) {
					uids = new HashSet<String>();
					staticsResults.put(reason, uids);
				}
				uids.add(uid);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			DBManager.closeConn(conn);
		}
		Map<String, Map<String, Map<String, Map<String, Set<String>>>>> dateResults = new HashMap<String, Map<String, Map<String, Map<String, Set<String>>>>>();
		dateResults.put(date, hostResults);
		UidSets.put(hostID, dateResults);
		return dateResults;
	}
	/**
	 * 将钻石统计数据更新到tblPayDayStatics表中
	 * @param platformID
	 * @param hostID
	 * @param date
	 * @param goldType
	 * @param staticsType
	 * @param goldNum
	 */
	private void staticsTotalGold(String platformID, String hostID, String date, String goldType, String staticsType,
			int goldNum) {
		Map<String, Map<String, Integer>> hostTotalMap = DayNumMap.get(hostID);
		if(hostTotalMap == null){
			hostTotalMap = new HashMap<String, Map<String, Integer>>();
			DayNumMap.put(hostID, hostTotalMap);
		}
		Map<String, Integer> dateMap = hostTotalMap.get(date);
		if(dateMap == null){
			String yesterday = DateUtils2.getOverDate(date, -1);
			Map<String, Integer> yesterdayMap = hostTotalMap.get(yesterday);
			if(yesterdayMap == null){
				yesterdayMap = loadDatePayInfoFromDB(platformID, hostID, yesterday);
			}
			dateMap = new HashMap<String, Integer>();
			dateMap.put("GoldConsume", yesterdayMap.getOrDefault("GoldConsume", 0));
			dateMap.put("GoldProduce", yesterdayMap.getOrDefault("GoldProduce", 0));
			dateMap.put("TotalGoldProduce", yesterdayMap.getOrDefault("TotalGoldProduce", 0));
			dateMap.put("TotalGoldConsume", yesterdayMap.getOrDefault("TotalGoldConsume", 0));
			dateMap.put("TotalCreditGoldProduce", yesterdayMap.getOrDefault("TotalCreditGoldProduce", 0));
			dateMap.put("TotalCreditGoldConsume", yesterdayMap.getOrDefault("TotalCreditGoldConsume", 0));
			dateMap.put("CreditGoldProduce", yesterdayMap.getOrDefault("CreditGoldProduce", 0));
			dateMap.put("CreditGoldConsume", yesterdayMap.getOrDefault("CreditGoldConsume", 0));
		}
		String goldStr = null;
		String staticsStr = null;
		if(goldType.equals("1")){
			goldStr = "Gold";
		}else{
			goldStr = "CreditGold";
		}
		if(staticsType.equals("1")){
			staticsStr = "Consume";
		}else{
			staticsStr = "Produce";
		}
		String key = goldStr + staticsStr;
		int num = dateMap.getOrDefault(key, 0);
		num += goldNum;
		dateMap.put(key, num);
		//总计数据也要更新
		int totalNum = dateMap.getOrDefault("Total"+key, 0);
		totalNum += goldNum;
		dateMap.put("Total"+key, totalNum);
		PayDayStaticsDB.insertGoldInfo(platformID, hostID, date, dateMap);
	}
	
	/**
	 * 从tblPayDayStatics表中加载昨天的总绑定钻石和非绑定钻石
	 * 
	 * @param platformID
	 * @param hostID
	 * @param date
	 * @return
	 */
	private Map<String, Integer> loadDatePayInfoFromDB(String platformID, String hostID, String date) {
		Map<String, Integer> totalNumMap = new HashMap<String, Integer>();
		String tblName = platformID + "_statics.tblPayDayStatics";
		List<String> options = new ArrayList<String>();
		options.add("Date = '" + date + "'");
		options.add("HostID = '" + hostID + "'");
		Connection conn = DBManager.getConn();
		ResultSet resultSet = CommonDB.query(conn, tblName, options);
		try {
			while (resultSet.next()) {
				totalNumMap.put("GoldConsume", resultSet.getInt("GoldConsume"));
				totalNumMap.put("GoldProduce", resultSet.getInt("GoldProduce"));
				totalNumMap.put("TotalGoldProduce", resultSet.getInt("TotalGoldProduce"));
				totalNumMap.put("TotalGoldConsume", resultSet.getInt("TotalGoldConsume"));
				totalNumMap.put("TotalCreditGoldProduce", resultSet.getInt("TotalCreditGoldProduce"));
				totalNumMap.put("TotalCreditGoldConsume", resultSet.getInt("TotalCreditGoldConsume"));
				totalNumMap.put("CreditGoldProduce", resultSet.getInt("CreditGoldProduce"));
				totalNumMap.put("CreditGoldConsume", resultSet.getInt("CreditGoldConsume"));
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			DBManager.closeConn(conn);
		}
		return totalNumMap;
	}

}
