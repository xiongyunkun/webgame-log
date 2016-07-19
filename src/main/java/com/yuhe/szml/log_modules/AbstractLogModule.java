package com.yuhe.szml.log_modules;

import java.util.List;
import java.util.Map;

public abstract class AbstractLogModule {
	
	public abstract Map<String, List<Map<String, String>>> execute(List<String> logList, Map<String,String> hostMap);
	public abstract String getStaticsIndex();

}
