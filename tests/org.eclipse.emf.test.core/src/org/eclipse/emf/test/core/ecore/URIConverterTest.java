/**
 * <copyright>
 *
 * Copyright (c) 2006-2008 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   IBM - Initial API and implementation
 *
 * </copyright>
 *
 * $Id: URIConverterTest.java,v 1.10 2010/12/03 20:42:55 khussey Exp $
 */
package org.eclipse.emf.test.core.ecore;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.URIConverter;
import org.eclipse.emf.ecore.resource.impl.ExtensibleURIConverterImpl;
import org.eclipse.emf.test.common.TestUtil;
import org.eclipse.emf.test.core.AllSuites;

public class URIConverterTest extends TestCase
{
  protected URIConverter uriConverter;
  
  /**
   * @param name
   */
  public URIConverterTest(String name)
  {
    super(name);
  }

  public static Test suite()
  {
    TestSuite ts = new TestSuite("URIConverterTest");
    ts.addTest(new URIConverterTest("testReadArchiveURI"));
    return ts;
  }
  
  @Override
  protected void setUp() throws Exception
  {
    // On real applications the URIConverter is usually obtained from 
    // a ResourceSet
    //
    uriConverter = new ExtensibleURIConverterImpl();
  }
  
  public void testReadArchiveURI() throws Exception
  {
    String pluginDirectory = TestUtil.getPluginDirectory(AllSuites.PLUGIN_ID);
    if (pluginDirectory.charAt(0) != '/')
    {
      pluginDirectory = "/" + pluginDirectory;
    }

    URI uri = URI.createFileURI(pluginDirectory + "/data/file.txt");
    String contentsFromUncompressedFile = readFile(uriConverter.createInputStream(uri));
    
    uri = URI.createURI("jar:file:" + pluginDirectory + "/%64ata/data.jar!/%66ile.txt");    
    String contents = readFile(uriConverter.createInputStream(uri));    
    assertEquals(contentsFromUncompressedFile, contents);

    uri = URI.createURI("archive:file:" + pluginDirectory + "/%64ata/data.jar!/%66ile.txt");    
    contents = readFile(uriConverter.createInputStream(uri)); 
    assertEquals(contentsFromUncompressedFile, contents);    

    uri = URI.createURI("archive:file:" + pluginDirectory + "/%661/data.jar!/%66ile.txt");    
    new File(URI.createURI(uri.authority().replaceAll("!", "")).toFileString()).delete();
    writeFile(uriConverter.createOutputStream(uri), contents);    
    contents = readFile(uriConverter.createInputStream(uri));    
    assertEquals(contentsFromUncompressedFile, contents);
    new File(URI.createURI(uri.authority().replaceAll("!", "")).toFileString()).delete();
    
    uri = URI.createURI("archive:file:" + pluginDirectory + "/%64ata/data.zip!/%66ile.txt");    
    contents = readFile(uriConverter.createInputStream(uri)); 
    assertEquals(contentsFromUncompressedFile, contents);

    uri = URI.createURI("archive:file:" + pluginDirectory + "/%661/data.zip!/%66ile.txt");    
    new File(URI.createURI(uri.authority().replaceAll("!", "")).toFileString()).delete();
    writeFile(uriConverter.createOutputStream(uri), contents);    
    contents = readFile(uriConverter.createInputStream(uri));    
    assertEquals(contentsFromUncompressedFile, contents);
    new File(URI.createURI(uri.authority().replaceAll("!", "")).toFileString()).delete();
    
    //Reads the data.zip file from our CVS repository using http
    //
    uri = URI.createURI("archive:http://dev.eclipse.org/viewcvs/index.cgi/org.eclipse.emf/org.eclipse.emf/tests/org.eclipse.emf.test.core/%64ata/data.zip?root=Modeling_Project&view=co!/%66ile.txt");    
    contents = readFile(uriConverter.createInputStream(uri)); 
    assertEquals(contentsFromUncompressedFile, contents);
  }
  
  protected String readFile(InputStream inputStream) throws IOException
  {
    try
    {
      if (!(inputStream instanceof BufferedInputStream))
      {
        inputStream = new BufferedInputStream(inputStream);
      }
      
      StringBuilder sb = new StringBuilder();
      byte[] buffer = new byte[1024];
      int size = 0;
      while ((size = inputStream.read(buffer)) > -1)
      {
        sb.append(new String(buffer, 0, size));
      }
      return sb.toString();
    }
    finally
    {
      inputStream.close();
    }
  }

  protected void writeFile(OutputStream outputStream, String contents) throws IOException
  {
    try
    {
      outputStream.write(contents.getBytes());
    }
    finally
    {
      outputStream.close();
    }
  }
}
