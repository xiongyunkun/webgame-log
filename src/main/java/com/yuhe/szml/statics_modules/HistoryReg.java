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
import com.yuhe.szml.db.statics.HistoryRegDB;
import com.yuhe.szml.utils.DateUtils2;

public class HistoryReg extends AbstractStaticsModule {
	// 记录当天服的注册人数信息：当天注册数，男生注册数，女生注册数，总注册数
	// 数据格式：<HostID, <date, <Type, Number>>>
	private Map<String, Map<String, Map<String, Integer>>> StaticsNumMap = new HashMap<String, Map<String, Map<String, Integer>>>();

	@Override
	public boolean execute(Map<String, List<Map<String, String>>> platformResults) {
		Set<String> flagSet = new HashSet<String>(); // 标志位，用来记录到底哪些hostid哪些date需要更新
		Iterator<String> pIt = platformResults.keySet().iterator();
		while (pIt.hasNext()) {
			String platformID = pIt.next();
			List<Map<String, String>> platformResult = platformResults.get(platformID);
			for (Map<String, String> logMap : platformResult) {
				String hostID = logMap.get("HostID");
				String sex = logMap.get("Sex");
				String time = logMap.get("Time");
				String[] times = StringUtils.split(time, " ");
				String date = times[0];
				Map<String, Map<String, Integer>> hostResults = StaticsNumMap.get(hostID);
				if (hostResults == null) {
					hostResults = loadFromDB(platformID, hostID, date, time);
					StaticsNumMap.put(hostID, hostResults);
				}
				Map<String, Integer> dateResult = hostResults.get(date);
				if (dateResult == null) {
					dateResult = new HashMap<String, Integer>();
					dateResult.put("RegNum", 0);
					dateResult.put("Male", 0);
					dateResult.put("Female", 0);
					String yesterday = DateUtils2.getOverDate(date, -1);
					int totalRegNum = 0;
					if (hostResults.containsKey(yesterday)) {
						totalRegNum = hostResults.get(yesterday).get("TotalRegNum");
					}
					dateResult.put("TotalRegNum", totalRegNum);
					hostResults = new HashMap<String, Map<String, Integer>>(); // 其他天数的数据都要清除
					hostResults.put(date, dateResult);
				}
				int regNum = dateResult.get("RegNum");
				int male = dateResult.get("Male");
				int female = dateResult.get("Female");
				int totalRegNum = dateResult.get("TotalRegNum");
				regNum += 1;
				totalRegNum += 1;
				if (sex.equals("1")) {
					male += 1;
				} else {
					female += 1;
				}
				dateResult.put("RegNum", regNum);
				dateResult.put("Male", male);
				dateResult.put("Female", female);
				dateResult.put("TotalRegNum", totalRegNum);
				flagSet.add(platformID + ";" + hostID + ";" + date);
			}
		}
		// 记录数据库
		for (String str : flagSet) {
			String[] strs = StringUtils.split(str, ";");
			if (strs.length == 3) {
				Map<String, Integer> numMap = StaticsNumMap.get(strs[1]).get(strs[2]);
				HistoryRegDB.batchInsert(strs[0], strs[1], strs[2], numMap);
			}
		}
		return true;
	}

	/**
	 * 从数据库tblAddPlayerLog表中获得注册人数以及总注册人数信息
	 * 
	 * @param platformID
	 * @param hostID
	 * @param date
	 * @return
	 */
	private Map<String, Map<String, Integer>> loadFromDB(String platformID, String hostID, String date,
			String endTime) {
		Map<String, Map<String, Integer>> hostResults = new HashMap<String, Map<String, Integer>>();
		Map<String, Integer> dateResult = new HashMap<String, Integer>();

		int regNum = 0;
		int male = 0;
		int female = 0;
		int totalRegNum = 0;
		String tblName = platformID + "_log.tblAddPlayerLog_" + date.replace("-", "");
		;
		List<String> options = new ArrayList<String>();
		options.add("HostID = '" + hostID + "'");
		options.add("Time >= '" + date + " 00:00:00'");
		options.add("Time < '" + endTime + "'");
		Connection conn = DBManager.getConn();
		ResultSet resultSet = CommonDB.query(conn, tblName, options);
		try {
			while (resultSet.next()) {
				int sex = resultSet.getInt("Sex");
				if (sex == 1) {
					male += 1;
				} else {
					female += 1;
				}
				regNum += 1;
				totalRegNum += 1;
			}
			// 再查一下昨天的历史注册表需要汇总下历史人数
			tblName = platformID + "_statics.tblHistoryReg";
			List<String> newOptions = new ArrayList<String>();
			newOptions.add("HostID = '" + hostID + "'");
			String yesterday = DateUtils2.getOverDate(date, -1);
			newOptions.add("Date='" + yesterday + "'");
			resultSet = CommonDB.query(conn, tblName, newOptions);
			while (resultSet.next()) {
				totalRegNum += resultSet.getInt("TotalRegNum");
				break;
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			DBManager.closeConn(conn);
		}
		dateResult.put("RegNum", regNum);
		dateResult.put("Male", male);
		dateResult.put("Female", female);
		dateResult.put("TotalRegNum", totalRegNum);
		hostResults.put(date, dateResult);
		return hostResults;
	}

}
