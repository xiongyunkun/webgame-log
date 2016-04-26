package com.yuhe.szml.bolt;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.storm.topology.BasicOutputCollector;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseBasicBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;

import com.yuhe.szml.db.ServerDB;
import com.yuhe.szml.statics_modules.AbstractStaticsModule;
import com.yuhe.szml.statics_modules.StaticsIndexes;



public class LogBolt extends BaseBasicBolt {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static Logger logger = Logger.getLogger(LogBolt.class);

	/**
	 * 接收log日志数据，并且判断日志类型，采用对应的处理类型记录日志
	 */
	@SuppressWarnings("unchecked")
	public void execute(Tuple input, BasicOutputCollector collector) {
		String staticsIndex = input.getString(0);
		List<String> logList = (List<String>) input.getValue(1);
		if (logList.size() > 0) {
			// 根据taticsIndex用对应的处理模块处理
			Map<String, AbstractStaticsModule> indexMap = StaticsIndexes.GetIndexMap();
			AbstractStaticsModule module = indexMap.get(staticsIndex);
			if (module != null) {
				Map<String, String> hostMap = ServerDB.getStaticsServers();
				module.execute(logList, hostMap);
			}
		}
//		collector.emit(new Values(staticsIndex));
	}

	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("log"));

	}

}
