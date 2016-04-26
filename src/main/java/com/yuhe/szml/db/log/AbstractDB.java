package com.yuhe.szml.db.log;

import java.util.List;
import java.util.Map;

public abstract class AbstractDB {

	public abstract boolean batchInsert(String platformID, List<Map<String, String>> results);
}
