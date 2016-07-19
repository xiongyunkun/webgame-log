package com.yuhe.szml.statics_modules;

import java.util.List;
import java.util.Map;

public class MixLoginStatics extends AbstractStaticsModule {

	@Override
	public boolean execute(Map<String, List<Map<String, String>>> platformResults) {
		Map<String, AbstractStaticsModule> indexMap = StaticsIndexes.GetIndexMap();
		AbstractStaticsModule retentionModule = indexMap.get("Retention");
		AbstractStaticsModule onlineTimeModule = indexMap.get("OnlineTime");
		retentionModule.execute(platformResults);
		onlineTimeModule.execute(platformResults);
		return true;
	}

}
