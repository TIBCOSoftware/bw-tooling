/*Copyright © 2018. TIBCO Software Inc. All Rights Reserved.*/
package com.tibco.bw.core.design.yml.editor;

import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.osgi.framework.Bundle;

public class ImageProvider {
	
	private static final String DELETE_IMAGE_PATH = "icons/obj16/delete_16x16.png"; //$NON-NLS-1$
	private static final String CHOOSE_IMAGE_PATH = "icons/obj16/import_16x16.png"; //$NON-NLS-1$
	private static final String ADD_IMAGE_PATH = "icons/obj16/add.gif"; //$NON-NLS-1$
	private static final String COLLAPSE_IMAGE_PATH = "icons/obj16/collapse.png"; //$NON-NLS-1$

	private ImageProvider(){
		
	}
	
	public static Image getImage( String path){
		Bundle bundle = Platform.getBundle("com.tibco.bw.core.design.process.editor"); //$NON-NLS-1$
		URL url = FileLocator.find(bundle, new Path(path), null);
		ImageDescriptor imageDesc = ImageDescriptor.createFromURL(url);
		Image image = imageDesc.createImage();
		return image;
	}

	public static Image getDeleteImage(){
		return getImage(DELETE_IMAGE_PATH);
	}
	
	public static Image getAddImage(){
		return getImage(ADD_IMAGE_PATH);
	}
	
	public static Image getChooseImage(){
		return getImage(CHOOSE_IMAGE_PATH);
	}

	public static Image getSelectAllImage() {
		return getImage(COLLAPSE_IMAGE_PATH);
	}
}
