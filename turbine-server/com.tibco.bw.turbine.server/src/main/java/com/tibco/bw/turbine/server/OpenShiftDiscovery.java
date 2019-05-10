/*Copyright © 2019. TIBCO Software Inc. All Rights Reserved.*/
package com.tibco.bw.turbine.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.netflix.turbine.discovery.Instance;
import com.netflix.turbine.discovery.InstanceDiscovery;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.client.DefaultOpenShiftClient;

public class OpenShiftDiscovery implements InstanceDiscovery {
    
	private static final Logger logger = Logger.getLogger(OpenShiftDiscovery.class.getName());
	private static String platform= System.getenv("PLATFORM");
	private static String namespace= System.getenv("NAMESPACE");
    private static final String HYSTRIX_ENABLED = "hystrix.enabled";      
    private static final String TRUE = "true";
    private static final String DEFAULT = "default";
    private static final String OPENSHIFT = "Openshift";
    private static final String KUBERNETES = "Kubernetes";
    private static OpenShiftDiscovery openshiftDiscovery;
    private boolean isOpenshift;
    private KubernetesClient client;
     
    public OpenShiftDiscovery() {
    	
    	isOpenshift=(platform!=null && platform.equalsIgnoreCase(OPENSHIFT));
    	logger.log(Level.INFO,"Configuring for "+ (isOpenshift?OPENSHIFT:KUBERNETES));
    	try{  		
    	    client=(isOpenshift)?new DefaultOpenShiftClient():new DefaultKubernetesClient();
    	}catch (Exception e){
    		logger.log(Level.SEVERE,"Cannot connect to master server.",e);
    	}  	
    	if(namespace==null)    	    			
    		namespace=client.getNamespace();
    	logger.log(Level.INFO,"Using Namespace " + namespace);
    	openshiftDiscovery=this;
    }
 
    public Collection<Instance>  getInstanceList() throws Exception {    	
        List<Instance> result = new ArrayList<Instance>();            
        try{                             				
		 PodList podList=client.pods().inNamespace(namespace).withLabel(HYSTRIX_ENABLED, TRUE).list();
		  for(Pod pod : podList.getItems()){
			  String podip=pod.getStatus().getPodIP();
			  Instance instance = new Instance(podip, DEFAULT, true);
			  result.add(instance);
		  }
        }catch(Exception e){
        	logger.log(Level.SEVERE,"Cannot fetch endpoints.",e);
        } 		 
        return result;
    }    
    
    public static String getNamespace(){
    	return namespace;
    }
    
    public static OpenShiftDiscovery getOpenShiftDiscovery(){
    	return openshiftDiscovery;
    }
}
