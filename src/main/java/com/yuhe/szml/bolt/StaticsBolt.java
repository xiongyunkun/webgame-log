package com.yuhe.szml.bolt;

import java.util.List;
import java.util.Map;

import org.apache.storm.topology.BasicOutputCollector;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseBasicBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;

import com.yuhe.szml.statics_modules.AbstractStaticsModule;
import com.yuhe.szml.statics_modules.StaticsIndexes;

public class StaticsBolt extends BaseBasicBolt {

	private static final long serialVersionUID = 1L;

	public void execute(Tuple input, BasicOutputCollector collector) {
		String staticsIndex = input.getString(0);
		@SuppressWarnings("unchecked")
		Map<String, List<Map<String, String>>> platformResults = (Map<String, List<Map<String, String>>>) input.getValue(1);
		Map<String, AbstractStaticsModule> indexMap = StaticsIndexes.GetIndexMap();
		AbstractStaticsModule module = indexMap.get(staticsIndex);
		if(module != null){
			module.execute(platformResults);
		}
	}

	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("staticsIndex"));

	}

}
