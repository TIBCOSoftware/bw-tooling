/*Copyright © 2018. TIBCO Software Inc. All Rights Reserved.*/

package com.tibco.bw.core.design.yml.model.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.Scanner;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.tibco.bw.core.design.yml.model.YMLModel;

public class ManifestYMLFileReader {

	public static YMLModel getDefaultModelFrom(URL urlToDefaultJSONFile) throws IOException,
			JsonParseException, JsonMappingException {

		// create ObjectMapper instance
		ObjectMapper objectMapper = new ObjectMapper();
		
		String contentsOf = contentsOf(urlToDefaultJSONFile);
		
		// convert json string to object
		YMLModel ymlModel = objectMapper.readValue(contentsOf, YMLModel.class);

		return ymlModel;
	}
	
	public static YMLModel getModelFrom(Path filePath) throws JsonParseException, JsonMappingException, IOException{
		YAMLMapper mapper = new YAMLMapper();
		String contents = contentsOf(filePath);
		YMLModel ymlModel = mapper.readValue(contents, YMLModel.class);	
		return ymlModel;
	}
	
	public static YMLModel getModelFrom(InputStream inputStream) throws JsonParseException, JsonMappingException, IOException{
		String contents = getContentsFrom(inputStream);
		YAMLMapper mapper = new YAMLMapper();
		YMLModel ymlModel = mapper.readValue(contents, YMLModel.class);
		
		return ymlModel;
	}
	
	private static String contentsOf(Path filePath) throws IOException{
		File file = filePath.toFile();
		InputStream inputStream = new FileInputStream(file);
		String contents = getContentsFrom(inputStream);
		inputStream.close();
		
		return contents;
	}
	
	private static String contentsOf(URL urlToDefaultJSONFile) throws IOException{
		URLConnection openConnection = urlToDefaultJSONFile.openConnection();
		InputStream inputStream = openConnection.getInputStream();
		
		String contents = getContentsFrom(inputStream);
		inputStream.close();
		
		return contents;
	}

	private static String getContentsFrom(InputStream inputStream) {
		Scanner scanner = new Scanner(inputStream);
		Scanner s = scanner.useDelimiter("\\A"); //$NON-NLS-1$
		String contents = s.hasNext() ? s.next() : ""; //$NON-NLS-1$
		
		s.close();
		scanner.close();
		return contents;
	}
}
