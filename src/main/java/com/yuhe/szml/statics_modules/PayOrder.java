package com.yuhe.szml.statics_modules;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.yuhe.szml.db.PayDB;
import com.yuhe.szml.utils.DateUtils;

import net.sf.json.JSONObject;

public class PayOrder extends AbstractStaticsModule {

	@Override
	public boolean execute(List<String> logList, Map<String, String> hostMap) {
		Map<String, Map<String, Map<String, String>>> platform5MinResults = new HashMap<String, Map<String, Map<String, String>>>();
		Map<String, Map<String, Map<String, String>>> platformPayResults = new HashMap<String, Map<String, Map<String, String>>>();
		String floorTime = DateUtils.getFloorTime();
		for (String log : logList) {
			JSONObject json = JSONObject.fromObject(log);
			if (json != null) {
				compute5MinCash(hostMap, json, floorTime, platform5MinResults); // 5分钟实时
				computeUserPayStatics(hostMap, json, platformPayResults);// 个人充值总额
			}
		}
		PayDB payDB = new PayDB();
		Iterator<String> it = platform5MinResults.keySet().iterator();
		while (it.hasNext()) {
			String platformID = it.next();
			Map<String, Map<String, String>> platform5MinResult = platform5MinResults.get(platformID);
			payDB.insert5Min(platformID, platform5MinResult);
			Map<String, Map<String, String>> platformPayResult = platformPayResults.get(platformID);
			payDB.insertUserPayStatics(platformID, platformPayResult);
		}
		return true;
	}

	/**
	 * 记录5分钟实时充值数据
	 * 
	 * @param hostMap
	 * @param json
	 * @param time
	 * @param platformResults
	 */
	public void compute5MinCash(Map<String, String> hostMap, JSONObject json, String time,
			Map<String, Map<String, Map<String, String>>> platformResults) {
		String hostID = json.getString("HostID");
		if (hostMap.containsKey(hostID)) {
			String platformID = hostMap.get(hostID);
			String cashNum = json.getString("CashNum");
			Map<String, Map<String, String>> platformResult = platformResults.getOrDefault(platformID,
					new HashMap<String, Map<String, String>>());
			Map<String, String> hostResult = platformResult.getOrDefault(hostID, new HashMap<String, String>());
			// 总充值金额
			double totalCashNum = Double.valueOf(cashNum) + Double.valueOf(hostResult.getOrDefault("PayCashNum", "0")); // 取出原有的充值金额
			hostResult.put("PayCashNum", String.valueOf(totalCashNum));
			hostResult.put("Time", time);
			// 总充值人数，这里只是简单加1，后面如果要详细的话再改
			int payUserNum = Integer.parseInt(hostResult.getOrDefault("PayUserNum", "0")) + 1;
			hostResult.put("PayUserNum", Integer.toString(payUserNum));
			hostResult.put("HostID", hostID);
			hostResult.put("Currency", json.getString("Currency")); //新加入汇率
			platformResult.put(hostID, hostResult);
			platformResults.put(platformID, platformResult);
		}
	}

	/**
	 * 计算个人充值总额
	 * 
	 * @param hostMap
	 * @param json
	 * @param platformPayResults
	 */
	public void computeUserPayStatics(Map<String, String> hostMap, JSONObject json,
			Map<String, Map<String, Map<String, String>>> platformPayResults) {
		String hostID = json.getString("HostID");
		if (hostMap.containsKey(hostID)) {
			String platformID = hostMap.get(hostID);
			String uid = json.getString("Uid");
			Map<String, Map<String, String>> userPayResults = platformPayResults.getOrDefault(platformID,
					new HashMap<String, Map<String, String>>());
			Map<String, String> userPayResult = userPayResults.get(uid);
			if (userPayResult == null) {
				userPayResult = new HashMap<String, String>();
				userPayResult.put("HostID", hostID);
				userPayResult.put("Uid", uid);
				userPayResult.put("Urs", json.getString("Urs"));
				userPayResult.put("Name", json.getString("Name"));
				userPayResult.put("Currency", json.getString("Currency"));
				userPayResult.put("TotalCashNum", json.getString("CashNum")); // 总充值金额
				userPayResult.put("TotalNum", "1"); // 充值次数
				userPayResult.put("TotalGold", json.getString("Gold")); // 充值所获钻石
				userPayResult.put("FirstCashNum", json.getString("CashNum"));// 首充金额
				userPayResult.put("FirstCashTime", json.getString("Time"));// 首充时间
				userPayResult.put("LastCashNum", json.getString("CashNum"));// 最后充值金额
				userPayResult.put("LastCashTime", json.getString("Time"));// 最后充值时间
				userPayResult.put("MinCashNum", json.getString("CashNum"));// 最小充值金额
				userPayResult.put("MaxCashNum", json.getString("CashNum"));// 最大充值金额
			} else {
				double totalCashNum = Double.valueOf(userPayResult.get("TotalCashNum"))
						+ Double.valueOf(json.getString("CashNum"));
				userPayResult.put("TotalCashNum", Double.toString(totalCashNum)); // 总充值金额
				int totalNum = Integer.parseInt(userPayResult.get("TotalNum")) + 1;
				userPayResult.put("TotalNum", Integer.toString(totalNum)); // 充值次数
				int totalGold = Integer.parseInt(userPayResult.get("TotalGold"))
						+ Integer.parseInt(json.getString("Gold"));
				userPayResult.put("TotalGold", Integer.toString(totalGold)); // 充值所获钻石
				userPayResult.put("LastCashNum", json.getString("CashNum"));// 最后充值金额
				userPayResult.put("LastCashTime", json.getString("Time"));// 最后充值时间
				double minCashNum = Math.min(Double.valueOf(userPayResult.get("MinCashNum")),
						Double.valueOf(json.getString("CashNum")));
				userPayResult.put("MinCashNum", Double.toString(minCashNum));// 最小充值金额
				double maxCashNum = Math.max(Double.valueOf(userPayResult.get("MaxCashNum")),
						Double.valueOf(json.getString("CashNum")));
				userPayResult.put("MaxCashNum", Double.toString(maxCashNum));// 最大充值金额
			}
			userPayResults.put(uid, userPayResult);
			platformPayResults.put(platformID, userPayResults);
		}
	}

}
