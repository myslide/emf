/**
 * <copyright>
 *
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: 
 *   IBM - Initial API and implementation
 *
 * </copyright>
 *
 * $Id: ModelImporter.java,v 1.5 2005/05/16 14:18:24 marcelop Exp $
 */
package org.eclipse.emf.importer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import org.eclipse.emf.codegen.ecore.Generator;
import org.eclipse.emf.codegen.ecore.genmodel.GenModel;
import org.eclipse.emf.codegen.ecore.genmodel.GenModelFactory;
import org.eclipse.emf.codegen.ecore.genmodel.GenPackage;
import org.eclipse.emf.common.CommonPlugin;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.common.util.UniqueEList;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.plugin.EcorePlugin;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.importer.util.ImporterUtil;


/**
 * @since 2.1.0
 */
public abstract class ModelImporter
{
  public static class EPackageList extends UniqueEList.FastCompare
  {
    public EPackageList()
    {
      super();
    }

    public EPackageList(Collection collection)
    {
      super(collection);
    }

    public EPackageList(int initialCapacity)
    {
      super(initialCapacity);
    }

    protected Object[] newData(int capacity)
    {
      return new EPackage [capacity];
    }
  }

  public static class GenPackageList extends UniqueEList.FastCompare
  {
    public GenPackageList()
    {
      super();
    }

    public GenPackageList(Collection collection)
    {
      super(collection);
    }

    public GenPackageList(int initialCapacity)
    {
      super(initialCapacity);
    }

    protected Object[] newData(int capacity)
    {
      return new GenPackage [capacity];
    }
  }

  public static class EPackageInfo
  {
    protected boolean generate = false;
    protected StringBuffer ecoreFileName;
    protected String basePackage;
    protected String prefix;

    public String getBasePackage()
    {
      return basePackage;
    }

    public void setBasePackage(String basePackage)
    {
      this.basePackage = basePackage;
    }

    public StringBuffer getEcoreFileName()
    {
      return ecoreFileName;
    }

    public void setEcoreFileName(StringBuffer ecoreFileName)
    {
      this.ecoreFileName = ecoreFileName;
    }

    public boolean isGenerate()
    {
      return generate;
    }

    public void setGenerate(boolean generate)
    {
      this.generate = generate;
    }

    public String getPrefix()
    {
      return prefix;
    }

    public void setPrefix(String prefix)
    {
      this.prefix = prefix;
    }
  }

  protected List fileExtensions;

  //  protected IFile originalGenModelFile;
  protected IPath originalGenModelPath;
  protected GenModel originalGenModel;

  protected IPath genModelProjectLocation;
  protected IPath genModelContainerPath;
  protected String genModelFileName;
  protected IPath genModelPath;
  protected GenModel genModel;

  protected List ePackages;
  protected Map ePackageToInfoMap;

  protected List referencedGenPackages;

  protected List modelLocationURIs;
  protected String modelLocation;

  protected String modelPluginID;
  protected String modelPluginDirectory;

  protected boolean usePlatformURI = true;
  protected IWorkspaceRoot workspaceRoot;
  
  protected ResourceSet externalGenModelResourceSet;
  protected List externalGenModelList;

  public void dispose()
  {
    genModel = null;
    originalGenModel = null;
    workspaceRoot = null;

    if (referencedGenPackages != null)
    {
      referencedGenPackages.clear();
      referencedGenPackages = null;
    }
    clearEPackagesCollections();
  }

  public String getID()
  {
    return null;
  }

  public List getFileExtensions()
  {
    if (fileExtensions == null)
    {
      fileExtensions = new ArrayList();
    }
    return fileExtensions;
  }

  public boolean usePlatformURI()
  {
    return usePlatformURI;
  }

  public void setUsePlatformURI(boolean usePlatformURI)
  {
    this.usePlatformURI = usePlatformURI;
  }

  public void defineOriginalGenModelPath(IPath path)
  {
    if (getOriginalGenModelPath() == null)
    {
      originalGenModelPath = path;
      if (getOriginalGenModelPath() != null)
      {
        URI genModelURI = createFileURI(getOriginalGenModelPath().toString());
        loadOriginalGenModel(genModelURI);
      }
    }
  }

  public IPath getOriginalGenModelPath()
  {
    return originalGenModelPath;
  }

  protected List computeEPackagesBeingReloaded()
  {
    if (getOriginalGenModel() != null)
    {
      List ePackages = new EPackageList();
      for (Iterator i = getOriginalGenModel().getGenPackages().iterator(); i.hasNext();)
      {
        GenPackage genPackage = (GenPackage)i.next();
        EPackage originalEPackage = genPackage.getEcorePackage();
        String nsURI = originalEPackage.getNsURI();
        if (nsURI != null)
        {
          for (Iterator j = getEPackages().iterator(); j.hasNext();)
          {
            EPackage ePackage = (EPackage)j.next();
            if (nsURI.equals(ePackage.getNsURI()))
            {
              ePackages.add(ePackage);
              break;
            }
          }
        }
      }
      return ePackages;
    }
    else
    {
      return Collections.EMPTY_LIST;
    }
  }

  public void setGenModelFileName(String name)
  {
    genModelFileName = name == null || name.trim().length() == 0 ? null : name;
    genModelPath = null;
  }

  public String getGenModelFileName()
  {
    return genModelFileName;
  }
  
  public String computeDefaultGenModelFileName()
  {
    List modelLocationURIs = getModelLocationURIs();
    if (!modelLocationURIs.isEmpty())
    {
      URI modelURI = (URI)modelLocationURIs.get(0);
      String name = URI.decode(modelURI.lastSegment());
      int index = name.lastIndexOf('.');
      return (index >= 0 ? name.substring(0, index) : name) + ".genmodel";
    }
    return null;
  }

  public IStatus checkGenModelFileName()
  {
    String message = null;
    String name = getGenModelFileName();
    if (name == null || name.trim().length() == 0)
    {
      message = ImporterPlugin.INSTANCE.getString("_UI_GeneratorModelFileNameCannotBeEmpty_message");
    }
    else if (!name.endsWith(".genmodel"))
    {
      message = ImporterPlugin.INSTANCE.getString("_UI_GeneratorModelFileNameMustEndWithGenModel_message");
    }

    if (message == null)
    {
      return Status.OK_STATUS;
    }
    else
    {
      return new Status(IStatus.ERROR, ImporterPlugin.ID, ImporterUtil.ACTION_DEFAULT, message, null);
    }
  }

  public IStatus checkEcoreModelFileName(String fileName, String packageName)
  {
    String message = null;

    if (fileName == null || fileName.equals(""))
    {
      message = packageName == null ?
        ImporterPlugin.INSTANCE.getString("_UI_EcoreModelFileNameCannotBeEmpty_message") :
        ImporterPlugin.INSTANCE.getString("_UI_EcoreModelFileNameForPackageCannotBeEmpty_message", new Object []{ packageName });
    }
    else if (!fileName.endsWith(".ecore"))
    {
      message = packageName == null ?
        ImporterPlugin.INSTANCE.getString("_UI_EcoreModelFileNameMustEndWithEcore_message") :
        ImporterPlugin.INSTANCE.getString("_UI_EcoreModelFileNameForPackageMustEndWithEcore_message", new Object []{ packageName });
    }
    if (message == null)
    {
      return Status.OK_STATUS;
    }
    else
    {
      return new Status(IStatus.ERROR, ImporterPlugin.ID, ImporterUtil.ACTION_DEFAULT, message, null);
    }
  }

  public void setGenModelProjectLocation(IPath genModelProjectLocation)
  {
    this.genModelProjectLocation = genModelProjectLocation;
  }

  public IPath getGenModelProjectLocation()
  {
    return genModelProjectLocation;
  }
  
  public void setGenModelContainerPath(IPath path)
  {
    genModelContainerPath = path;
    genModelPath = null;
  }

  public IPath getGenModelContainerPath()
  {
    return genModelContainerPath;
  }
  
  public IPath computeGenModelContainerPath(IPath projectPath)
  {
    return projectPath == null ? null : projectPath.append(getGenModelDefaultFolderPath());
  }

  protected IPath getGenModelDefaultFolderPath()
  {
    return new Path("model");
  }

  public IPath getGenModelPath()
  {
    if (genModelPath == null && getGenModelFileName() != null && getGenModelContainerPath() != null)
    {
      genModelPath = getGenModelContainerPath().append(getGenModelFileName());
    }
    return genModelPath;
  }

  public GenModel getGenModel()
  {
    if (genModel == null)
    {
      genModel = GenModelFactory.eINSTANCE.createGenModel();
      genModel.setImporterID(getID());
    }
    return genModel;
  }

  public boolean addGenModelToResource(boolean replace)
  {
    GenModel genModel = getGenModel();
    if (replace || genModel.eResource() == null)
    {
      IPath genModelPath = getGenModelPath();
      if (genModelPath != null)
      {
        URI genModelURI = createFileURI(genModelPath.toString());
        Resource genModelResource = createResourceSet().createResource(genModelURI);
        genModelResource.getContents().add(genModel);
        return true;
      }
    }
    return false;
  }

  public ResourceSet getGenModelResourceSet()
  {
    Resource resource = getGenModel().eResource();
    if (resource == null && addGenModelToResource(false))
    {
      resource = getGenModel().eResource();
    }

    if (resource != null)
    {
      return resource.getResourceSet();
    }
    else
    {
      return null;
    }
  }

  protected GenModel getOriginalGenModel()
  {
    return originalGenModel;
  }

  public List getEPackages()
  {
    if (ePackages == null)
    {
      ePackages = new EPackageList();
    }
    return ePackages;
  }

  protected Map getEPackageToInfoMap()
  {
    if (ePackageToInfoMap == null)
    {
      ePackageToInfoMap = new HashMap();
    }
    return ePackageToInfoMap;
  }

  public EPackageInfo getEPackageInfo(EPackage ePackage)
  {
    EPackageInfo ePackageInfo = (EPackageInfo)getEPackageToInfoMap().get(ePackage);
    if (ePackageInfo == null)
    {
      ePackageInfo = new EPackageInfo();
      getEPackageToInfoMap().put(ePackage, ePackageInfo);
    }
    return ePackageInfo;
  }

  public List getReferencedGenPackages()
  {
    if (referencedGenPackages == null)
    {
      referencedGenPackages = new GenPackageList();
    }
    return referencedGenPackages;
  }

  protected GenPackage getGenPackage(EPackage ePackage)
  {
    if (getOriginalGenModel() != null && !getOriginalGenModel().getGenPackages().isEmpty())
    {
      for (Iterator r = getOriginalGenModel().getGenPackages().iterator(); r.hasNext();)
      {
        GenPackage referencedGenPackage = (GenPackage)r.next();
        if (referencedGenPackage.getEcorePackage() != null && referencedGenPackage.getEcorePackage().getNsURI().equals(ePackage.getNsURI()))
        {
          return referencedGenPackage;
        }
      }
    }
    return null;
  }

  public void setModelLocation(String location)
  {
    modelLocation = location == null || location.trim().length() == 0 ? null : location;
    modelLocationURIs = null;
  }

  public String getModelLocation()
  {
    return modelLocation;
  }

  public List getModelLocationURIs()
  {
    if (getModelLocation() == null)
    {
      return Collections.EMPTY_LIST;
    }
    else if (modelLocationURIs == null)
    {
      modelLocationURIs = new ArrayList();
      for (StringTokenizer stringTokenizer = new StringTokenizer(getModelLocation()); stringTokenizer.hasMoreTokens();)
      {
        String uri = stringTokenizer.nextToken();
        modelLocationURIs.add(URI.createURI(uri));
      }
    }
    return modelLocationURIs;
  }
  
  public URI getFirstModelLocationURI(boolean resolve)
  {
    List modelLocationURIs = getModelLocationURIs();
    if (!modelLocationURIs.isEmpty())
    {
      URI modelLocationURI = (URI)modelLocationURIs.get(0);
      return resolve ? CommonPlugin.resolve(modelLocationURI) : modelLocationURI;
    }
    return null;
  }

  public void setModelFile(IFile file)
  {
    if (file != null)
    {
      URI modelURI = URI.createPlatformResourceURI(file.getFullPath().toString(), true);
      setModelLocation(modelURI.toString());
    }
    else
    {
      setModelLocation(null);
    }
  }

  public EPackage getReferredEPackage(GenPackage genPackage)
  {
    String nsURI = genPackage.getEcorePackage().getNsURI();
    if (nsURI != null)
    {
      for (Iterator j = ePackages.iterator(); j.hasNext();)
      {
        EPackage ePackage = (EPackage)j.next();
        if (nsURI.equals(ePackage.getNsURI()))
        {
          return ePackage;
        }
      }
    }
    return null;
  }

  public List filterReferencedEPackages(List ePackages)
  {
    if (ePackages.isEmpty())
    {
      return Collections.EMPTY_LIST;
    }
    else if (getReferencedGenPackages().isEmpty())
    {
      return new ArrayList(ePackages);
    }
    else
    {
      List filteredEPackages = new EPackageList(ePackages);
      for (Iterator i = getReferencedGenPackages().iterator(); i.hasNext();)
      {
        GenPackage genPackage = (GenPackage)i.next();
        EPackage ePackage = getReferredEPackage(genPackage);
        if (ePackage != null)
        {
          filteredEPackages.remove(ePackage);
        }
      }
      return filteredEPackages;
    }
  }
  
  public List getExternalGenModels()
  {
    if (externalGenModelList == null)
    {
      externalGenModelList = new UniqueEList.FastCompare();
      if (externalGenModelResourceSet == null)
      {
        externalGenModelResourceSet = createResourceSet();
      }
      Map ePackageToGenModelMap = EcorePlugin.getEPackageNsURIToGenModelLocationMap();
      for (Iterator i = getEPackages().iterator(); i.hasNext(); )
      {
        EPackage ePackage = (EPackage)i.next();
        URI genModelURI = (URI)ePackageToGenModelMap.get(ePackage.getNsURI());
        if (genModelURI != null)
        {
          try
          {
            Resource genModelResource = externalGenModelResourceSet.getResource(genModelURI, true);
            externalGenModelList.add(genModelResource.getContents().get(0));
          }
          catch (Exception exception)
          {
            ImporterPlugin.INSTANCE.log(exception);
          }
        }
      }
    }
    return externalGenModelList;
  }
  
  public ResourceSet createResourceSet()
  {
    return ImporterUtil.createResourceSet();
  }

  protected void loadOriginalGenModel(URI genModelURI)
  {
    Resource resource = createResourceSet().getResource(genModelURI, true);
    originalGenModel = (GenModel)resource.getContents().get(0);

    if (getOriginalGenModel() != null)
    {
      getOriginalGenModel().reconcile();
      setGenModelFileName(getOriginalGenModelPath().lastSegment());
      setGenModelContainerPath(getOriginalGenModelPath().removeLastSegments(1));
      genModelPath = getOriginalGenModelPath();
    }

    for (Iterator i = getOriginalGenModel().getUsedGenPackages().iterator(); i.hasNext();)
    {
      GenPackage referencedGenPackage = (GenPackage)i.next();
      getReferencedGenPackages().add(referencedGenPackage);
    }
  }

  public IStatus computeEPackages(IProgressMonitor progressMonitor) throws Exception
  {
    clearEPackagesCollections();
    IStatus status = doComputeEPackages(progressMonitor);
    adjustEPackages();
    presetEPackagesToGenerate();
    return status;
  }
  
  protected void presetEPackagesToGenerate()
  {
    int size = getEPackages().size(); 
    if (size == 1)
    {
      getEPackageInfo((EPackage)getEPackages().get(0)).setGenerate(true);
    }
    else if (size > 1)
    {
      List ePackagesBeingReloaded = computeEPackagesBeingReloaded();
      for (Iterator i = ePackagesBeingReloaded.iterator(); i.hasNext();)
      {
        getEPackageInfo((EPackage)i.next()).setGenerate(true);
      }
    }    
  }

  protected IStatus doComputeEPackages(IProgressMonitor progressMonitor) throws Exception
  {
    return Status.OK_STATUS;
  }

  public void clearEPackagesCollections()
  {
    if (ePackages != null)
    {
      ePackages.clear();
      ePackages = null;
    }
    if (ePackageToInfoMap != null)
    {
      ePackageToInfoMap.clear();
      ePackageToInfoMap = null;
    }
    if (externalGenModelList != null)
    {
      externalGenModelList.clear();
      externalGenModelList = null;
    }
  }

  protected void adjustEPackages()
  {
    for (Iterator i = getEPackages().iterator(); i.hasNext();)
    {
      EPackage ePackage = (EPackage)i.next();
      adjustEPackage(ePackage);
    }
  }

  public void adjustEPackage(EPackage ePackage)
  {
    EPackageInfo ePackageInfo = getEPackageInfo(ePackage);
    String qualifiedName = ePackage.getName();

    int index = qualifiedName.lastIndexOf(".");
    if (index != -1)
    {
      ePackageInfo.setBasePackage(qualifiedName.substring(0, index));
      ePackage.setName(qualifiedName.substring(index + 1));
    }
    else
    {
      ePackageInfo.setBasePackage(null);
    }
    String prefix = ePackage.getName();
    ePackageInfo.setPrefix(Character.toUpperCase(prefix.charAt(0)) + prefix.substring(1));

    String ecoreFileName = null;
    GenPackage genPackage = getGenPackage(ePackage);

    if (genPackage != null)
    {
      String ePackagePath = genPackage.getEcorePackage().eResource().getURI().lastSegment();
      ecoreFileName = URI.decode(ePackagePath);
    }
    else
    {
      ecoreFileName = ePackage.eResource() == null ? qualifiedName + ".ecore" : ePackage.eResource().getURI().lastSegment();
    }
    ePackageInfo.setEcoreFileName(new StringBuffer(ecoreFileName));
  }

  protected IWorkspaceRoot getWorkspaceRoot()
  {
    if (workspaceRoot == null)
    {
      workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
    }
    return workspaceRoot;
  }

  public void prepareGenModelAndEPackages(IProgressMonitor progressMonitor)
  {
    String baseLocation = getGenModelPath().removeLastSegments(1).toString() + "/";
    ResourceSet resourceSet = getGenModelResourceSet();
    
    resourceSet.getURIConverter().getURIMap().remove(URI.createPlatformResourceURI(getModelProjectName() + "/"));

    // Create resources for all the root EPackages.
    //
    List ePackages = computeEPackagesToGenerate();
    for (Iterator i = ePackages.iterator(); i.hasNext();)
    {
      EPackage ePackage = (EPackage)i.next();
      EPackageInfo ePackageInfo = getEPackageInfo(ePackage);

      if (ePackageInfo.getEcoreFileName() != null)
      {
        String ecoreFileName = baseLocation + ePackageInfo.getEcoreFileName().toString();
        URI ecoreURI = createFileURI(ecoreFileName);
        Resource resource = resourceSet.getResource(ecoreURI, false);
        if (resource == null)
        {
          resource = resourceSet.createResource(ecoreURI);
        }
        resource.getContents().add(ePackage);
      }
    }

    // Create resources for all the referenced EPackages
    // The referencedEPackage is a "local" instance of the realEPackage.  We 
    // will add the former o a resource that has the same URI of the later.
    for (Iterator i = getReferencedGenPackages().iterator(); i.hasNext();)
    {
      GenPackage genPackage = (GenPackage)i.next();
      EPackage realEPackage = genPackage.getEcorePackage();
      GenModel genModel = genPackage.getGenModel();

      EPackage referredEPackage = getReferredEPackage(genPackage);
      if (referredEPackage != null)
      {
        URI ecoreURI = realEPackage.eResource().getURI();
        Resource resource = resourceSet.createResource(ecoreURI);
        resource.getContents().add(referredEPackage);
      }
    }

    // Initialize the GenModel with all the computed data.
    //
    getGenModel().initialize(ePackages);
    getGenModel().getUsedGenPackages().addAll(getReferencedGenPackages());
    traverseGenPackages(getGenModel().getGenPackages());
    adjustGenModel(progressMonitor);

    // Restore all configured settings from the original.
    //
    getGenModel().reconcile(getOriginalGenModel());
  }

  public void saveGenModelAndEPackages(IProgressMonitor progressMonitor) throws Exception
  {
    String projectName = getModelProjectName();
    IProject project = getWorkspaceRoot().getProject(projectName);
    if (!project.exists())
    {
      Set referencedGenModels = new HashSet();
      for (Iterator i = getGenModel().getUsedGenPackages().iterator(); i.hasNext();)
      {
        GenPackage genPackage = (GenPackage)i.next();
        referencedGenModels.add(genPackage.getGenModel());
      }
      createProject(progressMonitor, project, referencedGenModels);
    }

    for (Iterator i = getGenModel().getGenPackages().iterator(); i.hasNext();)
    {
      GenPackage genPackage = (GenPackage)i.next();
      genPackage.getEcorePackage().eResource().save(getEcoreSaveOptions());
    }
    getGenModel().eResource().save(getGenmodelSaveOptions());
  }

  protected void createProject(IProgressMonitor progressMonitor, IProject project, Collection referencedGenModels)
  {
    IWorkspaceRoot workspaceRoot = getWorkspaceRoot();

    // Determine which projects will need to be referenced.
    //
    List referencedModelProjects = new ArrayList();
    List referencedEditProjects = new ArrayList();
    for (Iterator i = referencedGenModels.iterator(); i.hasNext();)
    {
      GenModel referencedGenModel = (GenModel)i.next();
      referencedModelProjects.add(workspaceRoot.getProject(new Path(referencedGenModel.getModelDirectory()).segment(0)));
      if (!referencedGenModel.getModelDirectory().equals(referencedGenModel.getEditDirectory()))
      {
        referencedEditProjects.add(workspaceRoot.getProject(new Path(referencedGenModel.getEditDirectory()).segment(0)));
      }
    }

    String projectName = project.getName();
    String path = getGenModel().getModelDirectory();
    int index = path.indexOf(projectName);
    if (index >= 0)
    {
      path = path.substring(index);
    }
    char firstChar = path.charAt(0);
    if (firstChar != '/' && firstChar != '\\')
    {
      path = "/" + path;
    }

    // Create the model project.
    //
    List referencedProjects = new ArrayList(referencedModelProjects);
    IProject emfProject = Generator.createEMFProject(
      new Path(path),
      getGenModelProjectLocation(),
      referencedProjects,
      progressMonitor,
      Generator.EMF_MODEL_PROJECT_STYLE | Generator.EMF_EMPTY_PROJECT_STYLE);
  }

  protected void adjustGenModel(IProgressMonitor progressMonitor)
  {
    String modelName = URI.decode(getGenModelPath().removeFileExtension().lastSegment());
    int index = modelName.lastIndexOf('.');
    if (index != -1)
    {
      modelName = modelName.substring(0, index);
    }
    modelName = Character.toUpperCase(modelName.charAt(0)) + modelName.substring(1);

    GenModel genModel = getGenModel();
    genModel.setModelName(modelName);
    genModel.setModelPluginID(getModelPluginID());
    genModel.setModelDirectory(getModelPluginDirectory());
  }

  protected List computeEPackagesToGenerate()
  {
    List ePackages = new EPackageList();
    for (Iterator i = getEPackages().iterator(); i.hasNext();)
    {
      EPackage ePackage = (EPackage)i.next();
      EPackageInfo ePackageInfo = getEPackageInfo(ePackage);
      if (ePackageInfo.isGenerate() && ePackageInfo.getEcoreFileName() != null)
      {
        ePackages.add(ePackage);
      }
    }
    return filterReferencedEPackages(ePackages);
  }

  public void traverseGenPackages(List genPackages)
  {
    for (Iterator i = genPackages.iterator(); i.hasNext();)
    {
      GenPackage genPackage = (GenPackage)i.next();
      EPackage ePackage = genPackage.getEcorePackage();
      EPackageInfo ePackageInfo = getEPackageInfo(ePackage);

      genPackage.setBasePackage(ePackageInfo.getBasePackage());
      genPackage.setPrefix(ePackageInfo.getPrefix());

      if (genPackage.getPrefix() == null || genPackage.getPrefix().length() == 0)
      {
        String name = ePackage.getName();
        genPackage.setPrefix(Character.toUpperCase(name.charAt(0)) + name.substring(1));
      }

      adjustGenPackageDuringTraverse(genPackage);
      traverseGenPackages(genPackage.getNestedGenPackages());
    }
  }

  protected void adjustGenPackageDuringTraverse(GenPackage genPackage)
  {

  }

  protected URI makeRelative(URI uri, URI relativeTo)
  {
    if ("file".equals(uri.scheme()))
    {
      IFile file = getWorkspaceRoot().getFileForLocation(new Path(uri.toFileString()));
      if (file != null)
      {
        URI platformURI = URI.createPlatformResourceURI(file.getFullPath().toString(), true);
        URI result = platformURI.deresolve(relativeTo, false, true, false);
        if (result.isRelative())
        {
          return result;
        }
      }
    }

    URI result = uri.deresolve(relativeTo, true, true, false);
    if (result.isRelative())
    {
      return result;
    }

    return uri;
  }

  protected URI makeAbsolute(URI uri, URI relativeTo)
  {
    if (uri.isRelative())
    {
      return uri.resolve(relativeTo);
    }
    return uri;
  }

  public URI createFileURI(String pathName)
  {
    return usePlatformURI() ? URI.createPlatformResourceURI(pathName, true) : URI.createFileURI(pathName);
  }

  public String getModelPluginID()
  {
    return modelPluginID == null ? ImporterUtil.validPluginID(getModelProjectName()) : modelPluginID;
  }

  public void setModelPluginID(String modelPluginID)
  {
    this.modelPluginID = modelPluginID;
  }

  public String getModelPluginDirectory()
  {
    if (modelPluginDirectory == null)
    {
      String result = getModelProjectName();
      if (result != null)
      {
        if (result.charAt(0) != '/')
        {
          result = "/" + result;
        }
        return result + "/src";
      }
    }
    return modelPluginDirectory;
  }

  public void setModelPluginDirectory(String modelPluginDirectory)
  {
    this.modelPluginDirectory = modelPluginDirectory;
  }

  public String getModelProjectName()
  {
    IPath path = getGenModelProjectLocation();
    if (path != null)
    {
      return URI.decode(path.lastSegment().toString());
    }

    IPath genModelPath = getGenModelPath();
    if (genModelPath != null)
    {
      return URI.decode(genModelPath.segment(0).toString());
    }

    return null;
  }

  protected Map getEcoreSaveOptions()
  {
    return Collections.EMPTY_MAP;
  }

  protected Map getGenmodelSaveOptions()
  {
    return Collections.EMPTY_MAP;
  }
}
