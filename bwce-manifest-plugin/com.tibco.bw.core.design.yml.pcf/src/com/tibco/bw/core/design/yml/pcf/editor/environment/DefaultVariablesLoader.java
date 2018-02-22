package com.tibco.bw.core.design.yml.pcf.editor.environment;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.Properties;
import java.util.logging.Logger;

import com.tibco.bw.core.design.yml.pcf.Messages;

public class DefaultVariablesLoader {
	
	private DefaultVariablesLoader(){
		
	}
	
	public static Properties loadProperties(){
		Properties props = new Properties();
		
		try {
			URL urlToDefaultPropertiesFile = getURLToDefaultPropertiesFile();
			URLConnection openConnection = urlToDefaultPropertiesFile.openConnection();
			InputStream inputStream = openConnection.getInputStream();
			props.load(inputStream);

			String name = DefaultVariablesLoader.class.getName();
			String msg = MessageFormat.format(Messages.EnvironmentPropertiesLoader_infoMessage, props.size());
			Logger.getLogger(name).info(msg);
		}catch (MalformedURLException e) {
			e.printStackTrace();
		}catch (IOException e) {
			e.printStackTrace();
		}
		
		return props;
	}
	
	private static URL getURLToDefaultPropertiesFile() throws MalformedURLException{
		StringBuilder urlBuilder = new StringBuilder();
		urlBuilder = urlBuilder.append("platform:/plugin"); //$NON-NLS-1$
		urlBuilder = urlBuilder.append("/com.tibco.bw.core.design.yml.pcf");  //$NON-NLS-1$
		urlBuilder = urlBuilder.append("/com/tibco/bw/core/design/yml/pcf/editor/environment"); //$NON-NLS-1$
		urlBuilder = urlBuilder.append("/defaultvalues.properties");  //$NON-NLS-1$
		
		String urlString = urlBuilder.toString();
		return new URL(urlString);
	}
}
