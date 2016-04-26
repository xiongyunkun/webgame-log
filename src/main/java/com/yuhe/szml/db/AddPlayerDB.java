package com.yuhe.szml.db;

import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

public class AddPlayerDB {
	/**
	 * 5分钟实时注册人数
	 * 
	 * @param platformID
	 * @param platformNum
	 * @param floorTime
	 * @return
	 */
	public static boolean batchInsert(String platformID, Map<String, Map<String, Integer>> platformNum,
			String floorTime) {
		Iterator<String> hIt = platformNum.keySet().iterator();
		while (hIt.hasNext()) {
			String hostID = hIt.next();
			Map<String, Integer> hostNums = platformNum.get(hostID);
			StringBuilder sb = new StringBuilder();
			String[] values = new String[] { platformID, hostID, Integer.toString(hostNums.getOrDefault("TotalNum", 0)),
					Integer.toString(hostNums.getOrDefault("Male", 0)),
					Integer.toString(hostNums.getOrDefault("Female", 0)), floorTime };
			sb.append("insert into ").append(platformID)
					.append("_statics.tblAddPlayer(PlatformID, HostID, RegNum, Male, Female, Time) values('")
					.append(StringUtils.join(values, "','"))
					.append("') on duplicate key update RegNum = values(RegNum)+RegNum,Male=values(Male)+Male,Female=values(Female)");
			DBManager.execute(sb.toString());
		}
		return true;
	}
}
