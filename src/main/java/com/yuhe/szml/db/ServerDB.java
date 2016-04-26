package com.yuhe.szml.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;


public class ServerDB {
	// 记录HostID与PlatformID的对应关系
	private static Map<String, String> HOST_MAP = new HashMap<String, String>();
	private static long HostLastUpdateTime = 0;
	private static int BENCH_DIFF = 300000; // 5分钟

	/**
	 * 获得统计服HostID的Map关系
	 * 
	 * @return
	 */
	public static Map<String, String> getStaticsServers() {
		long nowTime = System.currentTimeMillis();
		if (nowTime - HostLastUpdateTime > BENCH_DIFF || HOST_MAP.size() == 0) {
			String sql = "select a.serverid as HostID, c.platformid as PlatformID from smcs.srvgroupinfo a, "
					+ "smcs.servergroup b, smcs.servers c where a.groupid = b.id and b.name = '统计专区' and a.serverid = c.hostid";
			Connection conn = DBManager.getConn();
			ResultSet results = DBManager.query(conn, sql);
			try {
				while (results.next()) {
					String hostID = results.getString("HostID");
					String platformID = results.getString("PlatformID");
					HOST_MAP.put(hostID, platformID);
				}
				HostLastUpdateTime = nowTime;
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				DBManager.closeConn(conn);
			}
		}
		return HOST_MAP;
	}
}
