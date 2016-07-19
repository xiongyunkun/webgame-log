package com.yuhe.szml.db;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.yuhe.szml.db.DBManager;

public class PayDB {
	// 5分钟实时在线表
	private static final String[] LOG5MIN_COLS = { "HostID", "PayCashNum", "PayUserNum", "Time", "Currency"};
	// 玩家充值统计表
	private static final String[] USER_PAY_COLS = { "Uid", "Urs", "Name", "HostID", "Currency", "TotalCashNum",
			"TotalNum", "TotalGold", "MinCashNum", "MaxCashNum", "FirstCashNum", "FirstCashTime", "LastCashNum",
			"LastCashTime" };

	/**
	 * 插入5分钟实时充值数据
	 * 
	 * @param platformID
	 * @param platformResult
	 * @return
	 */
	public boolean insert5Min(String platformID, Map<String, Map<String, String>> platformResult) {
		List<String> sqlValues = new ArrayList<String>();
		Iterator<String> hostIt = platformResult.keySet().iterator();
		while (hostIt.hasNext()) {
			String hostID = hostIt.next();
			Map<String, String> hostResult = platformResult.get(hostID);
			List<String> values = new ArrayList<String>();
			for (String col : LOG5MIN_COLS) {
				String value = hostResult.get(col);
				values.add(value);
			}
			sqlValues.add(StringUtils.join(values, "','"));
		}
		String sql = "insert into " + platformID + "_statics.tblPayActualTime(" + StringUtils.join(LOG5MIN_COLS, ",")
				+ ") values('" + StringUtils.join(sqlValues, "'),('")
				+ "') on duplicate key update PayCashNum = values(PayCashNum)+PayCashNum,PayUserNum = values(PayUserNum)+PayUserNum";
		DBManager.execute(sql);
		return true;
	}

	/**
	 * 插入玩家充值统计表
	 * 
	 * @param platformID
	 * @param platformResult
	 * @return
	 */
	public boolean insertUserPayStatics(String platformID, Map<String, Map<String, String>> platformResult) {
		List<String> sqlValues = new ArrayList<String>();
		Iterator<String> hostIt = platformResult.keySet().iterator();
		while (hostIt.hasNext()) {
			String hostID = hostIt.next();
			Map<String, String> hostResult = platformResult.get(hostID);
			List<String> values = new ArrayList<String>();
			for (String col : USER_PAY_COLS) {
				String value = hostResult.get(col);
				values.add(value);
			}
			sqlValues.add(StringUtils.join(values, "','"));
		}
		StringBuilder updateSql = new StringBuilder();
		updateSql.append("TotalCashNum = TotalCashNum + values(TotalCashNum),").append("TotalNum = TotalNum + values(TotalNum),")
		.append("TotalGold = TotalGold + values(TotalGold),").append("MinCashNum = least(MinCashNum, values(MinCashNum)),")
		.append("MaxCashNum = greatest(MaxCashNum, values(MaxCashNum)),").append("LastCashNum = values(LastCashNum),")
		.append("LastCashTime = values(LastCashTime),").append("Name = values(Name)");
		String sql = "insert into " + platformID + "_statics.tblUserPayStatics(" + StringUtils.join(USER_PAY_COLS, ",")
				+ ") values('" + StringUtils.join(sqlValues, "'),('")
				+ "') on duplicate key update " + updateSql.toString();
		DBManager.execute(sql);
		return true;
	}
}
