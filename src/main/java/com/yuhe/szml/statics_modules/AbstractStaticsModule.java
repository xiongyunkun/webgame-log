package com.yuhe.szml.statics_modules;

import java.util.List;
import java.util.Map;

public abstract class AbstractStaticsModule {
	
	public abstract boolean execute(List<String> logList, Map<String,String> hostMap);

}
