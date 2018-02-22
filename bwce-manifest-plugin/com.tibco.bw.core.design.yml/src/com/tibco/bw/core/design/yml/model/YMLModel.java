package com.tibco.bw.core.design.yml.model;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.IllegalFormatException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.MissingFormatArgumentException;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class YMLModel {
	
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private Map<String,Object> others;

	public YMLModel() {
		others= new LinkedHashMap<String, Object>();
	}
	
	@JsonAnyGetter
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	public Map<String, Object> getOthers() {
		return others;
	}
	@JsonAnySetter
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	public void setOthers(String name, Object value) {
		others.put(name,value);
	}
	
	public Map<String, Object> getFirstLevelMapOf(String key)throws IllegalFormatException{
		ArrayList<Object> returnList = null;

		if(getOthers().get(key) instanceof ArrayList<?>)
			returnList = (ArrayList<Object>) getOthers().get(key);
		else if(getOthers().get(key) != null){
			String msg = MessageFormat.format(Messages.YMLModel_missingKey, key);
			throw new MissingFormatArgumentException(msg);
		}
		if(returnList == null){
			returnList = new ArrayList<>();
			returnList.add(new LinkedHashMap<String, Object>());			
			getOthers().put(key, returnList);
		}
		
		Map<String, Object> returnMap= (Map<String, Object>) returnList.get(0);
		return returnMap;
	}
}
