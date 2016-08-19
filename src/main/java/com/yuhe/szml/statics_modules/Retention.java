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

import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.storm.shade.org.apache.commons.lang.StringUtils;

import com.yuhe.szml.db.DBManager;
import com.yuhe.szml.db.ServerDB;
import com.yuhe.szml.db.log.CommonDB;
import com.yuhe.szml.db.statics.RetentionDB;
import com.yuhe.szml.utils.DateUtils2;

/**
 * 统计留存率，付费留存率数据
 * 
 * @author xiongyunkun
 *
 */
public class Retention extends AbstractStaticsModule {
	// 记录历史注册数据，数据格式：Map<HostID, Map<Date,Set<Uid>>>
	private static Map<String, Map<String, Set<String>>> RegResults = new HashMap<String, Map<String, Set<String>>>();
	// 记录首充人数，数据格式:Map<HostID, Map<Date,Set<Uid>>>
	private static Map<String, Map<String, Set<String>>> FirstPayResults = new HashMap<String, Map<String, Set<String>>>();
	// 记录登陆的玩家Uid，数据格式：Map<HostID, Map<Date,Set<Uid>>>,这里记录今天和昨天的数据,前天之前的数据都会删除
	private static Map<String, Map<String, Set<String>>> LoginDayUids = new HashMap<String, Map<String, Set<String>>>();
	// 记录每次统计周期内的登陆玩家uid，统计完后会清空，数据格式：Map<PlatformID, Map<HostID,
	// Map<Date,Set<Uid>>>>
	private Map<String, Map<String, Map<String, Set<String>>>> PeriodLoginUids = new HashMap<String, Map<String, Map<String, Set<String>>>>();

	@Override
	public boolean execute(Map<String, List<Map<String, String>>> platformResults) {
		Map<String, Map<String, Map<String, Set<String>>>> platformUids = getLoginUid(platformResults);
		mergeTodayLoginUids(platformUids);
		Iterator<String> pIt = platformUids.keySet().iterator();
		while (pIt.hasNext()) {
			String platformID = pIt.next();
			Map<String, Map<String, Set<String>>> pLoginUids = PeriodLoginUids.get(platformID);
			if (pLoginUids == null) {
				pLoginUids = new HashMap<String, Map<String, Set<String>>>();
				PeriodLoginUids.put(platformID, pLoginUids);
			}
			Iterator<String> hIt = platformUids.get(platformID).keySet().iterator();
			while (hIt.hasNext()) {
				String hostID = hIt.next();
				Map<String, Set<String>> hLoginUids = pLoginUids.get(hostID);
				if (hLoginUids == null) {
					hLoginUids = new HashMap<String, Set<String>>();
					pLoginUids.put(hostID, hLoginUids);
				}
				Iterator<String> dIt = platformUids.get(platformID).get(hostID).keySet().iterator();
				while (dIt.hasNext()) {
					String date = dIt.next();
					Set<String> dLoginUids = hLoginUids.get(date);
					if (dLoginUids == null) {
						dLoginUids = new HashSet<String>();
						hLoginUids.put(date, dLoginUids);
					}
					dLoginUids.addAll(platformUids.get(platformID).get(hostID).get(date));
				}
			}
		}
		return true;
	}

	/**
	 * 从登陆和登出日志中获得Uid,并且uid进行过滤去重
	 * 
	 * @param platformResults
	 * @return
	 */
	private Map<String, Map<String, Map<String, Set<String>>>> getLoginUid(
			Map<String, List<Map<String, String>>> platformResults) {
		Map<String, Map<String, Map<String, Set<String>>>> platformUids = new HashMap<String, Map<String, Map<String, Set<String>>>>();
		Iterator<String> pIt = platformResults.keySet().iterator();
		while (pIt.hasNext()) {
			String platformID = pIt.next();
			List<Map<String, String>> platformResult = platformResults.get(platformID);
			Map<String, Map<String, Set<String>>> hostResults = platformUids.get(platformID);
			if (hostResults == null) {
				hostResults = new HashMap<String, Map<String, Set<String>>>();
				platformUids.put(platformID, hostResults);
			}
			for (Map<String, String> hostResult : platformResult) {
				String hostID = hostResult.get("HostID");
				String uid = hostResult.get("Uid");
				String time = hostResult.get("Time");
				String[] times = StringUtils.split(time, " ");
				String date = times[0];
				Map<String, Set<String>> dateResults = hostResults.get(hostID);
				if (dateResults == null) {
					dateResults = new HashMap<String, Set<String>>();
					hostResults.put(hostID, dateResults);
				}
				Set<String> uidSet = dateResults.get(date);
				if (uidSet == null) {
					uidSet = new HashSet<String>();
					dateResults.put(date, uidSet);
				}
				uidSet.add(uid);
			}
		}
		return platformUids;
	}

	/**
	 * 汇总合并今天的登陆的玩家Uid
	 * 
	 * @param loginUids
	 * @return
	 */
	private void mergeTodayLoginUids(Map<String, Map<String, Map<String, Set<String>>>> platformUids) {
		Iterator<String> pfIt = platformUids.keySet().iterator();
		while (pfIt.hasNext()) {
			String platformID = pfIt.next();
			Map<String, Map<String, Set<String>>> loginUids = platformUids.get(platformID);
			Iterator<String> pIt = loginUids.keySet().iterator();
			while (pIt.hasNext()) {
				String hostID = pIt.next();
				Map<String, Set<String>> hostResults = LoginDayUids.get(hostID);
				if (hostResults == null) {
					hostResults = new HashMap<String, Set<String>>();
					LoginDayUids.put(hostID, hostResults);
				}
				Map<String, Set<String>> hostUids = loginUids.get(hostID);
				Iterator<String> hIt = hostUids.keySet().iterator();
				while (hIt.hasNext()) {
					String date = hIt.next();
					Set<String> uids = hostUids.get(date);
					Set<String> dayUids = hostResults.get(date);
					if (dayUids == null) {
						dayUids = loadLoginUidFromDB(platformID, hostID, date);
						Set<String> logoutUids = loadLogoutUidFromDB(platformID, hostID, date);
						dayUids.addAll(logoutUids);
						hostResults.put(date, dayUids);
						// 今天没有数据，记录今天数据的同时需要将除了今天和昨天的数据都要删除
						String yesterday = DateUtils2.getOverDate(date, -1);
						Iterator<String> dIt = hostResults.keySet().iterator();
						while (dIt.hasNext()) {
							String tDate = dIt.next();
							if (!tDate.equals(date) && !tDate.equals(yesterday)) {
								dIt.remove(); // 除去今天和昨天的都要删除
							}
						}
					}
					dayUids.addAll(uids);
				}
			}
		}
	}

	/**
	 * 从数据库中加载当天的登陆玩家的uid
	 * 
	 * @param platformID
	 * @param hostID
	 * @param date
	 * @return
	 */
	private static Set<String> loadLoginUidFromDB(String platformID, String hostID, String date) {
		Set<String> uids = new HashSet<String>();
		String tblName = platformID + "_log.tblLoginLog_" + date.replace("-", "");
		List<String> options = new ArrayList<String>();
		options.add("HostID = '" + hostID + "'");
		options.add("Time >= '" + date + " 00:00:00'");
		options.add("Time <= '" + date + " 23:59:59'");
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
	 * 从数据库中加载当天的登出玩家的uid
	 * 
	 * @param platformID
	 * @param hostID
	 * @param date
	 * @return
	 */
	private static Set<String> loadLogoutUidFromDB(String platformID, String hostID, String date) {
		Set<String> uids = new HashSet<String>();
		String tblName = platformID + "_log.tblLogoutLog_" + date.replace("-", "");
		List<String> options = new ArrayList<String>();
		options.add("HostID = '" + hostID + "'");
		options.add("Time >= '" + date + " 00:00:00'");
		options.add("Time <= '" + date + " 23:59:59'");
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
	 * 返回该服该天的注册Uid列表，如果该列表为none，则从数据库中加载数据，并缓存在RegResults中
	 * 
	 * @param platformID
	 * @param hostID
	 * @param date
	 */
	private static Set<String> loadRegUid(String platformID, String hostID, String date) {
		Map<String, Set<String>> hostRegUids = RegResults.get(hostID);
		if (hostRegUids == null) {
			hostRegUids = new HashMap<String, Set<String>>();
			RegResults.put(hostID, hostRegUids);
		}
		Set<String> dateResults = hostRegUids.get(date);
		if (dateResults == null) {
			dateResults = new HashSet<String>();
			// 从对应数据库中加载
			String tblName = platformID + "_log.tblAddPlayerLog_" + date.replace("-", "");
			List<String> options = new ArrayList<String>();
			options.add("HostID = '" + hostID + "'");
			options.add("Time >= '" + date + " 00:00:00'");
			options.add("Time <= '" + date + " 23:59:59'");
			Connection conn = DBManager.getConn();
			ResultSet resultSet = CommonDB.query(conn, tblName, options);
			try {
				while (resultSet.next()) {
					String uid = resultSet.getString("Uid");
					dateResults.add(uid);
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				DBManager.closeConn(conn);
			}
			hostRegUids.put(date, dateResults);
		}
		return dateResults;
	}

	/**
	 * 返回该服该天的首充玩家uid列表,如果该列表为none，则从数据库中加载数据，并缓存在RegResults中
	 * 
	 * @param platformID
	 * @param hostID
	 * @param date
	 * @return
	 */
	private Set<String> loadFirstPayUid(String platformID, String hostID, String date) {
		Map<String, Set<String>> hostFirstPayUids = FirstPayResults.get(hostID);
		if (hostFirstPayUids == null) {
			hostFirstPayUids = new HashMap<String, Set<String>>();
			FirstPayResults.put(hostID, hostFirstPayUids);
		}
		Set<String> dateResults = hostFirstPayUids.get(date);
		if (dateResults == null) {
			dateResults = new HashSet<String>();
			// 从对应数据库中加载
			String tblName = platformID + "_statics.tblUserPayStatics";
			List<String> options = new ArrayList<String>();
			options.add("HostID = '" + hostID + "'");
			options.add("FirstCashTime >= '" + date + " 00:00:00'");
			options.add("FirstCashTime <= '" + date + " 23:59:59'");
			Connection conn = DBManager.getConn();
			ResultSet resultSet = CommonDB.query(conn, tblName, options);
			try {
				while (resultSet.next()) {
					String uid = resultSet.getString("Uid");
					dateResults.add(uid);
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				DBManager.closeConn(conn);
			}
			hostFirstPayUids.put(date, dateResults);
		}
		return dateResults;
	}

	/**
	 * 统计登陆留存率
	 * 
	 * @param platformID
	 * @param hostID
	 * @param date
	 */
	private void staticsLoginRetention(String platformID, String hostID, String date) {
		// 获得当天的登陆玩家Uid
		Set<String> loginUids = LoginDayUids.get(hostID).get(date);
		int loginNum = loginUids.size();
		Set<String> dateRegUids = loadRegUid(platformID, hostID, date);
		int regNum = dateRegUids.size();
		// 先记录今天的注册数和登陆数
		Map<String, String> keyValues = new HashMap<String, String>();
		keyValues.put("NewNum", Integer.toString(regNum));
		keyValues.put("LoginNum", Integer.toString(loginNum));
		RetentionDB.insertLoginRetention(platformID, hostID, date, keyValues);
		// 再统计每天的留存率
		int[] days = { -1, -2, -3, -4, -5, -6, -7, -10, -13, -15, -29, -30 };
		for (int day : days) {
			String dayStr = DateUtils2.getOverDate(date, day);
			Set<String> regDay = loadRegUid(platformID, hostID, dayStr);
			float rate = staticsRetentionRate(loginUids, regDay);
			Map<String, String> map = new HashMap<String, String>();
			String col = Math.abs(day) + "Days";
			map.put(col, Float.toString(rate));
			RetentionDB.insertLoginRetention(platformID, hostID, date, map);
		}
	}

	/**
	 * 统计付费留存率
	 * 
	 * @param platformID
	 * @param hostID
	 * @param date
	 */
	private void staticsPayUserRetention(String platformID, String hostID, String date) {
		// 获得当天的登陆玩家Uid
		Set<String> loginUids = LoginDayUids.get(hostID).get(date);
		int loginNum = loginUids.size();
		Set<String> dateRegUids = loadFirstPayUid(platformID, hostID, date);
		int regNum = dateRegUids.size();
		// 先记录今天的注册数和登陆数
		Map<String, String> keyValues = new HashMap<String, String>();
		keyValues.put("FirstPayUserNum", Integer.toString(regNum));
		keyValues.put("LoginNum", Integer.toString(loginNum));
		RetentionDB.insertPayRetention(platformID, hostID, date, keyValues);
		// 再统计每天的留存率
		int[] days = { -1, -2, -3, -4, -5, -6, -7, -10, -13, -15, -29, -30 };
		for (int day : days) {
			String dayStr = DateUtils2.getOverDate(date, day);
			Set<String> regDay = loadFirstPayUid(platformID, hostID, dayStr);
			float rate = staticsRetentionRate(loginUids, regDay);
			Map<String, String> map = new HashMap<String, String>();
			String col = Math.abs(day) + "Days";
			map.put(col, Float.toString(rate));
			RetentionDB.insertPayRetention(platformID, hostID, date, map);
		}
	}

	/**
	 * 根据登陆玩家Uid和注册玩家uid计算留存率
	 * 
	 * @param loginUids
	 * @param regUids
	 * @return
	 */
	public float staticsRetentionRate(Set<String> loginUids, Set<String> regUids) {
		float rate = 0;
		float regNum = regUids.size();
		int loginNum = loginUids.size();
		if (loginNum > 0 && regNum > 0) {
			int num = 0;
			for (String uid : loginUids) {
				if (regUids.contains(uid)) {
					num++;
				}
			}
			float bench = 100;
			rate = Math.round(num * 10000 / regNum) / bench;
		}
		return rate;
	}

	/**
	 * 获得某天的注册玩家uid列表
	 * 
	 * @param hostID
	 * @param date
	 * @return
	 */
	public static Set<String> getRegUids(String platformID, String hostID, String date) {
		return loadRegUid(platformID, hostID, date);
	}

	/**
	 * 获得某天的登陆玩家uid列表
	 * 
	 * @param hostID
	 * @param date
	 * @return
	 */
	public static Set<String> getLoginUids(String platformID, String hostID, String date) {
		Map<String, Set<String>> hostUids = LoginDayUids.get(hostID);
		if(hostUids == null){
			hostUids = new HashMap<String, Set<String>>();
			LoginDayUids.put(hostID, hostUids);
		}
		Set<String> loginUids = hostUids.get(date);
		if(loginUids == null){
			loginUids = loadLoginUidFromDB(platformID, hostID, date);
			Set<String> logoutUids = loadLogoutUidFromDB(platformID, hostID, date);
			loginUids.addAll(logoutUids);
			hostUids.put(date, loginUids);
		}
		return loginUids;
	}

	@Override
	public boolean cronExecute() {
		synchronized (PeriodLoginUids) {
			Iterator<String> pIt = PeriodLoginUids.keySet().iterator();
			while (pIt.hasNext()) {
				String platformID = pIt.next();
				Iterator<String> hIt = PeriodLoginUids.get(platformID).keySet().iterator();
				while (hIt.hasNext()) {
					String hostID = hIt.next();
					Iterator<String> dIt = PeriodLoginUids.get(platformID).get(hostID).keySet().iterator();
					while (dIt.hasNext()) {
						String date = dIt.next();
						staticsLoginRetention(platformID, hostID, date); // 登陆留存
						staticsPayUserRetention(platformID, hostID, date);// 付费留存
					}
				}
			}
			// 其他没有数据的服也要统计
			String today = DateFormatUtils.format(System.currentTimeMillis(), "yyyy-MM-dd");
			Map<String, String> hostMap = ServerDB.getStaticsServers();
			Iterator<String> hIt = hostMap.keySet().iterator();
			while (hIt.hasNext()) {
				String hostID = hIt.next();
				String platformID = hostMap.get(hostID);
				Map<String, Set<String>> todayUids = LoginDayUids.get(hostID);
				if (todayUids == null) {
					todayUids = new HashMap<String, Set<String>>();
					LoginDayUids.put(hostID, todayUids);
				}
				Set<String> uids = todayUids.get(today);
				if (uids == null) {
					uids = loadLoginUidFromDB(platformID, hostID, today);
					Set<String> logoutUids = loadLogoutUidFromDB(platformID, hostID, today);
					uids.addAll(logoutUids);
					todayUids.put(today, uids);
					staticsLoginRetention(platformID, hostID, today); // 登陆留存
					staticsPayUserRetention(platformID, hostID, today);// 付费留存
				}
			}
			// 清空
			PeriodLoginUids = new HashMap<String, Map<String, Map<String, Set<String>>>>();
		}
		return true;
	}

}
