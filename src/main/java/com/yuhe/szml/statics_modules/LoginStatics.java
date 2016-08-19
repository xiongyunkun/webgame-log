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
import com.yuhe.szml.db.statics.LoginStaticsDB;

/**
 * 登陆过程分析统计
 * 
 * @author xiongyunkun
 *
 */
public class LoginStatics extends AbstractStaticsModule {
	private static final int STANDARD_ID = 5; // 标准ID，如果不在这个步骤里面的不用统计
	// 记录当天登陆过程的玩家uid，不在这里面的不予统计，另外时间过了还需要及时清空,格式:<HostID, <date, <Uids>>>
	private Map<String, Map<String, Set<String>>> StandardUids = new HashMap<String, Map<String, Set<String>>>();
	// 记录需要入库的统计结果，入库完毕后这些统计结果会被清空，格式：// 记录格式<platformID, HostID,<Date, <Hour,
	// <Step, StepNum>>>>>
	private Map<String, Map<String, Map<String, Map<String, Map<String, Integer>>>>> PlatformStatics = new HashMap<String, Map<String, Map<String, Map<String, Map<String, Integer>>>>>();

	@Override
	public synchronized boolean execute(Map<String, List<Map<String, String>>> platformResults) {
		Iterator<String> pIt = platformResults.keySet().iterator();
		while (pIt.hasNext()) {
			String platformID = pIt.next();
			List<Map<String, String>> platformResult = platformResults.get(platformID);
			Map<String, Map<String, Map<String, Map<String, Integer>>>> hostResults = PlatformStatics.get(platformID);
			if (hostResults == null) {
				hostResults = new HashMap<String, Map<String, Map<String, Map<String, Integer>>>>();
				PlatformStatics.put(platformID, hostResults);
			}

			for (Map<String, String> result : platformResult) {
				String hostID = result.get("HostID");
				String uid = result.get("Uid");
				String time = result.get("Time");
				String step = result.get("Step");

				String[] timeInfo = getTimeInfo(time);
				String date = timeInfo[0];
				String hour = timeInfo[1];
				if (date != null && hour != null) {
					// 分类整理
					Set<String> standardUids = getStandardUids(platformID, hostID, date);
					if (Integer.parseInt(step) == STANDARD_ID)
						standardUids.add(uid);
					int stepInt = Integer.parseInt(step);
					// 前面登陆验证的步骤都需要统计进去
					if (standardUids.contains(uid) || stepInt <= STANDARD_ID || stepInt == 7) {
						// 只统计今天登陆游戏的，之前登陆的都不予计算
						Map<String, Map<String, Map<String, Integer>>> hostResult = hostResults.get(hostID);
						if (hostResult == null) {
							hostResult = new HashMap<String, Map<String, Map<String, Integer>>>();
							hostResults.put(hostID, hostResult);
						}
						Map<String, Map<String, Integer>> dateResult = hostResult.get(date);
						if (dateResult == null) {
							dateResult = new HashMap<String, Map<String, Integer>>();
							hostResult.put(date, dateResult);
						}
						Map<String, Integer> hourResult = dateResult.get(hour);
						if (hourResult == null) {
							hourResult = new HashMap<String, Integer>();
							dateResult.put(hour, hourResult);
						}
						int stepNum = hourResult.getOrDefault(step, 0);
						hourResult.put(step, stepNum + 1);
					}
				}
			}
		}
		return true;
	}

	/**
	 * 获得今天登陆过游戏的玩家Uid
	 * 
	 * @param platformID
	 * @param hostID
	 * @param date
	 * @return
	 */
	private Set<String> getStandardUids(String platformID, String hostID, String date) {
		Map<String, Set<String>> hostUids = StandardUids.get(hostID);
		if (hostUids == null) {
			hostUids = new HashMap<String, Set<String>>();
			StandardUids.put(hostID, hostUids);
		}
		Set<String> uids = hostUids.get(date);
		if (uids == null) {
			uids = loadUidFromDB(platformID, hostID, date);
			hostUids.put(date, uids);
			// 新增这一天的同时需要把其他天数的数据清空
			Iterator<String> it = hostUids.keySet().iterator();
			while (it.hasNext()) {
				String tDate = it.next();
				if (!tDate.equals(date)) {
					it.remove();
				}
			}
		}
		return uids;
	}

	/**
	 * 从数据库中加载Uid
	 * 
	 * @param platformID
	 * @param hostID
	 * @param date
	 * @return
	 */
	private Set<String> loadUidFromDB(String platformID, String hostID, String date) {
		Set<String> uids = new HashSet<String>();
		String tblName = platformID + "_log.tblClientLoadLog_" + date.replace("-", "");
		List<String> options = new ArrayList<String>();
		options.add("HostID = '" + hostID + "'");
		options.add("Time >= '" + date + " 00:00:00'");
		options.add("Time <= '" + date + " 23:59:59'");
		options.add("Step = '" + STANDARD_ID + "'");
		Connection conn = DBManager.getConn();
		ResultSet resultSet = CommonDB.query(conn, tblName, options);
		try {
			while (resultSet.next()) {
				String uid = resultSet.getString("Uid");
				uids.add(uid);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			DBManager.closeConn(conn);
		}
		return uids;
	}

	/**
	 * 从时间中获得日期和小时
	 * 
	 * @param time
	 * @return
	 */
	private String[] getTimeInfo(String time) {
		String[] timeInfo = new String[2];
		String[] times = StringUtils.split(time, " ");
		if (times.length >= 2) {
			timeInfo[0] = times[0]; // 日期
			String[] hourInfo = StringUtils.split(times[1], ":");
			if (hourInfo.length >= 3) {
				timeInfo[1] = hourInfo[0];
			}
		}
		return timeInfo;
	}

	/**
	 * 定时将统计结果写入数据库
	 */
	@Override
	public synchronized boolean cronExecute() {
		Iterator<String> pIt = PlatformStatics.keySet().iterator();
		while (pIt.hasNext()) {
			String platformID = pIt.next();
			Map<String, Map<String, Map<String, Map<String, Integer>>>> hostResults = PlatformStatics.get(platformID);
			// 再按照小时重新合并整理数据
			Iterator<String> hIt = hostResults.keySet().iterator();
			while (hIt.hasNext()) {
				String hostID = hIt.next();
				Map<String, Map<String, Map<String, Integer>>> hostResult = hostResults.get(hostID);
				Iterator<String> dIt = hostResult.keySet().iterator();
				while (dIt.hasNext()) {
					String date = dIt.next();
					Map<String, Map<String, Integer>> dateResult = hostResult.get(date);
					Iterator<String> hourIt = dateResult.keySet().iterator();
					while (hourIt.hasNext()) {
						String hour = hourIt.next();
						Map<String, Integer> hourResult = dateResult.get(hour);
						Map<String, Integer> stepResult = new HashMap<String, Integer>();
						// 点击接口数是用Step=7的数量减去Step=2的数量
						if (hourResult.containsKey("7") || hourResult.containsKey("2")) {
							int interfaceNum = hourResult.getOrDefault("7", 0) - hourResult.getOrDefault("2", 0);
							if (interfaceNum != 0)
								stepResult.put("1", interfaceNum);
						}
						// 其他步骤统计还是按原有step来记录
						Iterator<String> sIt = hourResult.keySet().iterator();
						while (sIt.hasNext()) {
							String step = sIt.next();
							int stepInt = Integer.parseInt(step);
							if (stepInt >= 10) {
								stepResult.put(step, hourResult.get(step));
							} else if (stepInt >= 3 && stepInt <= 6) { // 进入游戏前的步骤数要减一
								stepResult.put(Integer.toString(stepInt - 1), hourResult.get(step));
							}
						}
						// 记录数据库
						if (stepResult.size() > 0) {
							LoginStaticsDB.insert(platformID, hostID, date, hour, stepResult);
						}
					}
				}
			}
		}
		// 记录入库后重新清空
		if (PlatformStatics.size() > 0) {
			PlatformStatics = new HashMap<String, Map<String, Map<String, Map<String, Map<String, Integer>>>>>();
		}
		return true;
	}

}
