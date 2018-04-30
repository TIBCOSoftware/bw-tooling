/*Copyright © 2018. TIBCO Software Inc. All Rights Reserved.*/

package com.tibco.bw.core.design.yml.pcf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.IllegalFormatException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UnknownFormatConversionException;

import com.tibco.bw.core.design.yml.model.YMLEditingModel;
import com.tibco.bw.core.design.yml.model.YMLModel;

public class YMLModelPCFWrapper {
	YMLModel model;
	private YMLEditingModel editingModel;

	public YMLModelPCFWrapper(YMLModel model, YMLEditingModel editingModel) {
		super();
		this.model = model;
		this.editingModel = editingModel;
	}
	
	public List<String> getServicesList() throws IllegalFormatException {
		if(model == null){
			initializeModelInstance();
		}
		List<String> returnList = Collections.emptyList();
		Object servicesList = model.getOthers().get(YMLModelPCFConstants.KEY_SERVICES);
		
		if (servicesList == null  ) {
			 putServicesList(null);
			 servicesList = model.getOthers().get(YMLModelPCFConstants.KEY_SERVICES);
		}
		
		if( !(servicesList instanceof List<?>)){
			//it is perhaps a map or simple string. we cannot support this.
			throw new UnknownFormatConversionException(Messages.YMLModelPCFWrapper_unrecognisedServices);
		}
			
		returnList = new ArrayList<String>();	
		for (Object obj : (ArrayList<?>)servicesList) {
			if(obj instanceof String){
				returnList.add((String)obj);
			}
		}
		return returnList;
	}
	
	public void putServicesList(List<String> servicesList){
		List<Object> listToAdd = new ArrayList<Object>();
		
		if(servicesList == null){
			model.getOthers().put(YMLModelPCFConstants.KEY_SERVICES, listToAdd);
			return; //Nothing to do now.
		}
		
		//Convert the incoming list to list of Object
		for (String string : (ArrayList<String>)servicesList) {
			if(string instanceof String){
				listToAdd.add(string);
			}
		}
		//Add the populated list.
		model.getOthers().put(YMLModelPCFConstants.KEY_SERVICES, listToAdd);
	}
	
	public void cleanEmptyServicesValues(){
		Set<String> listSet = new HashSet<>();
		try {
			List<String> servicesList = getServicesList();
			listSet.addAll(servicesList);
			servicesList.clear();
			servicesList.addAll(listSet);
			servicesList.removeAll(Arrays.asList("")); //$NON-NLS-1$
			putServicesList(servicesList);
			if(servicesList.isEmpty()){
				model.getOthers().remove(YMLModelPCFConstants.KEY_SERVICES);
			}
		} catch (IllegalFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public Map<String, Object> getApplicationMap() throws IllegalFormatException{
		List<Object> applicationList = null;
		if(model== null ){
			initializeModelInstance();
		}
		
		Object object = model.getOthers().get(YMLModelPCFConstants.KEY_APPLICATIONS);
		if(object == null){
			applicationList = putApplicationMap(new LinkedHashMap<String, Object>());
			object =applicationList;
		}
		
		if(object instanceof ArrayList<?>){
			applicationList = (ArrayList<Object>) object;
		}else if(object != null){
			//it is perhaps a map or simple string. we cannot support this.
			throw new UnknownFormatConversionException(Messages.YMLModelPCFWrapper_unrecognizedApplications);
		}
		
		if(applicationList.get(0) == null ){
			putApplicationMap(new LinkedHashMap<String, Object>());
		}
		if(!(applicationList.get(0) instanceof Map<?, ?>)){
			throw new UnknownFormatConversionException(Messages.YMLModelPCFWrapper_unrecognizedApplications);
		}
		Map<String, Object> testmap= (Map<String, Object>) applicationList.get(0);
		return testmap;
	}
	
	public List<Object> putApplicationMap(Map<String, Object> map){
		Object object = model.getOthers().get(YMLModelPCFConstants.KEY_APPLICATIONS);
		List<Object> applicationList = null;
		if(object == null){
			applicationList = new ArrayList<>();
			object = applicationList;
			
			object = applicationList;
		}
		if(object instanceof ArrayList<?>){
			applicationList.add( map);
		}
		
		model.getOthers().put(YMLModelPCFConstants.KEY_APPLICATIONS, applicationList);
		return applicationList;
	}
	
	public void clearApplications() {
		try {
			Map<String, Object> applicationMap = getApplicationMap();
			if(applicationMap != null && (applicationMap instanceof Map<?, ?>)){
			
			   Iterator<Entry<String, Object>> iter= applicationMap.entrySet().iterator();
			   while(iter.hasNext()){
				   boolean equals = iter.next().getValue().equals(""); //$NON-NLS-1$
				   if(equals){
					   iter.remove();
				   }
			   }
			   if(applicationMap.isEmpty()){
				   model.getOthers().remove(YMLModelPCFConstants.KEY_APPLICATIONS);
			   }
			}
		} catch (IllegalFormatException e) {
			e.printStackTrace();
		}
	}

	public void setModel(YMLModel model) {
		this.model = model;
		
	}
	
	public Map<String, Object> getEnvironmentVariableMap(){
		if(model== null ){
			return Collections.emptyMap();
		}
		
		Object object = model.getOthers().get(YMLModelPCFConstants.KEY_ENV);
		if(object == null){
			object = new LinkedHashMap<String, Object>();
			putEnvMap((Map<String,Object>)object);
		}
		
		Map<String, Object> envMap = null;
		if(object instanceof Map<?, ?>)
			envMap = (Map<String, Object>) object;
		else if(object != null){
			//it is perhaps a map or simple string. we cannot support this.
			throw new UnknownFormatConversionException(Messages.YMLModelPCFWrapper_unrecognizedApplications);
		}
		
		
		return envMap;
	}

	private void putEnvMap(Map<String, Object> linkedHashMap) {
		Object object = model.getOthers().get(YMLModelPCFConstants.KEY_ENV);
		if(object == null){
			model.getOthers().put(YMLModelPCFConstants.KEY_ENV, linkedHashMap);
		}
	}

	public void clearEnvMap() {
		Map<String, Object> envMap = getEnvironmentVariableMap();
		if(envMap!=null){
			envMap.remove(""); //$NON-NLS-1$
			if(envMap.isEmpty()){
				model.getOthers().remove(YMLModelPCFConstants.KEY_ENV);
			}
		}
	}
	
	public List<Character> getValidCharsList() {
		List<Character> chars = Arrays.asList('0', '1','2','3','4','5','6','7','8','9','\b');
		return chars;
	}
	
	private void initializeModelInstance(){
		//FixME:Editing model should not be written here need to refactor the code
		//Sections should only pass specific model to modify
		model = new YMLModel();
		editingModel.setYmlModel(model);
	}

	public static void setApplicationName(String appName, YMLModel model) {
		if(appName!=null){
				ArrayList<Object> applicationList = (ArrayList<Object>)model.getOthers().get(YMLModelPCFConstants.KEY_APPLICATIONS);	
				((Map<String, Object>)applicationList.get(0)).put(YMLModelPCFConstants.KEY_NAME,appName);
		}
	}
	
	public static void setPath(String path, YMLModel model ){
		Map<String, Object> map = model.getFirstLevelMapOf(YMLModelPCFConstants.KEY_APPLICATIONS);
		map.put(YMLModelPCFConstants.KEY_PATH,path);
	}
}
