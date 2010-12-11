/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.wicket.settings.def;

import java.util.List;
import java.util.Map;

import org.apache.wicket.Application;
import org.apache.wicket.IResourceFactory;
import org.apache.wicket.Localizer;
import org.apache.wicket.javascript.IJavaScriptCompressor;
import org.apache.wicket.markup.html.IPackageResourceGuard;
import org.apache.wicket.markup.html.PackageResourceGuard;
import org.apache.wicket.resource.PropertiesFactory;
import org.apache.wicket.resource.loader.ClassStringResourceLoader;
import org.apache.wicket.resource.loader.ComponentStringResourceLoader;
import org.apache.wicket.resource.loader.IStringResourceLoader;
import org.apache.wicket.resource.loader.PackageStringResourceLoader;
import org.apache.wicket.resource.loader.ValidatorStringResourceLoader;
import org.apache.wicket.settings.IResourceSettings;
import org.apache.wicket.util.file.IFileUploadCleaner;
import org.apache.wicket.util.file.IResourceFinder;
import org.apache.wicket.util.file.IResourcePath;
import org.apache.wicket.util.file.Path;
import org.apache.wicket.util.lang.Args;
import org.apache.wicket.util.lang.Generics;
import org.apache.wicket.util.resource.locator.IResourceStreamLocator;
import org.apache.wicket.util.resource.locator.ResourceStreamLocator;
import org.apache.wicket.util.time.Duration;
import org.apache.wicket.util.watch.IModificationWatcher;
import org.apache.wicket.util.watch.ModificationWatcher;

/**
 * @author Jonathan Locke
 * @author Chris Turner
 * @author Eelco Hillenius
 * @author Juergen Donnerstag
 * @author Johan Compagner
 * @author Igor Vaynberg (ivaynberg)
 * @author Martijn Dashorst
 * @author James Carman
 */
public class ResourceSettings implements IResourceSettings
{
	/**
	 * Whether we should disable gzip compression for resources.
	 */
	private boolean disableGZipCompression = false;

	/** I18N support */
	private Localizer localizer;

	/** Map to look up resource factories by name */
	private final Map<String, IResourceFactory> nameToResourceFactory = Generics.newHashMap();

	/** The package resource guard. */
	private IPackageResourceGuard packageResourceGuard = new PackageResourceGuard();

	/** The factory to be used for the property files */
	private org.apache.wicket.resource.IPropertiesFactory propertiesFactory;

	/** Filesystem Path to search for resources */
	private IResourceFinder resourceFinder = new Path();

	/** Frequency at which files should be polled */
	private Duration resourcePollFrequency = null;

	/** resource locator for this application */
	private IResourceStreamLocator resourceStreamLocator;

	/** ModificationWatcher to watch for changes in markup files */
	private IModificationWatcher resourceWatcher;

	/** a cleaner for the temporary files created by FileUpload functionality */
	private IFileUploadCleaner fileUploadCleaner;

	/** Chain of string resource loaders to use */
	private final List<IStringResourceLoader> stringResourceLoaders = Generics.newArrayList(4);

	/** Flags used to determine how to behave if resources are not found */
	private boolean throwExceptionOnMissingResource = true;

	/** Determines behavior of string resource loading if string is missing */
	private boolean useDefaultOnMissingResource = true;

	/** Default cache duration */
	private Duration defaultCacheDuration = Duration.hours(1);

	/** The Javascript compressor */
	private IJavaScriptCompressor javascriptCompressor;

	/** escape string for '..' within resource keys */
	private String parentFolderPlaceholder = null;

	// use timestamps on resource file names
	private boolean useTimestampOnResourcesName = true;

	/**
	 * Construct
	 * 
	 * @param application
	 */
	public ResourceSettings(final Application application)
	{
		stringResourceLoaders.add(new ComponentStringResourceLoader());
		stringResourceLoaders.add(new PackageStringResourceLoader());
		stringResourceLoaders.add(new ClassStringResourceLoader(application.getClass()));
		stringResourceLoaders.add(new ValidatorStringResourceLoader());
	}

	/**
	 * @see org.apache.wicket.settings.IResourceSettings#addResourceFactory(java.lang.String,
	 *      org.apache.wicket.IResourceFactory)
	 */
	public void addResourceFactory(final String name, IResourceFactory resourceFactory)
	{
		nameToResourceFactory.put(name, resourceFactory);
	}

	/**
	 * @see org.apache.wicket.settings.IResourceSettings#addResourceFolder(java.lang.String)
	 */
	public void addResourceFolder(final String resourceFolder)
	{
		// Get resource finder
		final IResourceFinder finder = getResourceFinder();

		// Make sure it's a path
		if (!(finder instanceof IResourcePath))
		{
			throw new IllegalArgumentException(
				"To add a resource folder, the application's resource finder must be an instance of IResourcePath");
		}

		// Cast to resource path and add folder
		final IResourcePath path = (IResourcePath)finder;
		path.add(resourceFolder);
	}

	/**
	 * @see org.apache.wicket.settings.IResourceSettings#getDisableGZipCompression()
	 */
	public boolean getDisableGZipCompression()
	{
		return disableGZipCompression;
	}

	/**
	 * @see org.apache.wicket.settings.IResourceSettings#getLocalizer()
	 */
	public Localizer getLocalizer()
	{
		if (localizer == null)
		{
			localizer = new Localizer();
		}
		return localizer;
	}

	/**
	 * @see org.apache.wicket.settings.IResourceSettings#getPackageResourceGuard()
	 */
	public IPackageResourceGuard getPackageResourceGuard()
	{
		return packageResourceGuard;
	}

	/**
	 * @see org.apache.wicket.settings.IResourceSettings#getPropertiesFactory()
	 */
	public org.apache.wicket.resource.IPropertiesFactory getPropertiesFactory()
	{
		if (propertiesFactory == null)
		{
			propertiesFactory = new PropertiesFactory(Application.get());
		}
		return propertiesFactory;
	}

	/**
	 * @see org.apache.wicket.settings.IResourceSettings#getResourceFactory(java.lang.String)
	 */
	public IResourceFactory getResourceFactory(final String name)
	{
		return nameToResourceFactory.get(name);
	}

	/**
	 * @see org.apache.wicket.settings.IResourceSettings#getResourceFinder()
	 */
	public IResourceFinder getResourceFinder()
	{
		return resourceFinder;
	}

	/**
	 * @see org.apache.wicket.settings.IResourceSettings#getResourcePollFrequency()
	 */
	public Duration getResourcePollFrequency()
	{
		return resourcePollFrequency;
	}

	/**
	 * @see org.apache.wicket.settings.IResourceSettings#getResourceStreamLocator()
	 */
	public IResourceStreamLocator getResourceStreamLocator()
	{
		if (resourceStreamLocator == null)
		{
			// Create compound resource locator using source path from
			// application settings
			resourceStreamLocator = new ResourceStreamLocator(getResourceFinder());
		}
		return resourceStreamLocator;
	}

	/**
	 * @see org.apache.wicket.settings.IResourceSettings#getResourceWatcher(boolean)
	 */
	public IModificationWatcher getResourceWatcher(boolean start)
	{
		if (resourceWatcher == null && start)
		{
			final Duration pollFrequency = getResourcePollFrequency();
			if (pollFrequency != null)
			{
				resourceWatcher = new ModificationWatcher(pollFrequency);
			}
		}
		return resourceWatcher;
	}

	/**
	 * @see org.apache.wicket.settings.IResourceSettings#setResourceWatcher(org.apache.wicket.util.watch.IModificationWatcher)
	 */
	public void setResourceWatcher(IModificationWatcher watcher)
	{
		resourceWatcher = watcher;
	}

	public IFileUploadCleaner getFileUploadCleaner()
	{
		return fileUploadCleaner;
	}

	public void setFileUploadCleaner(IFileUploadCleaner fileUploadCleaner)
	{
		this.fileUploadCleaner = fileUploadCleaner;
	}

	/**
	 * @see org.apache.wicket.settings.IResourceSettings#getStringResourceLoaders()
	 */
	public List<IStringResourceLoader> getStringResourceLoaders()
	{
		return stringResourceLoaders;
	}

	/**
	 * @see org.apache.wicket.settings.IResourceSettings#getThrowExceptionOnMissingResource()
	 */
	public boolean getThrowExceptionOnMissingResource()
	{
		return throwExceptionOnMissingResource;
	}

	/**
	 * @see org.apache.wicket.settings.IResourceSettings#getUseDefaultOnMissingResource()
	 */
	public boolean getUseDefaultOnMissingResource()
	{
		return useDefaultOnMissingResource;
	}

	/**
	 * @see org.apache.wicket.settings.IResourceSettings#setDisableGZipCompression(boolean)
	 */
	public void setDisableGZipCompression(boolean disableGZipCompression)
	{
		this.disableGZipCompression = disableGZipCompression;
	}

	/**
	 * @see org.apache.wicket.settings.IResourceSettings#setLocalizer(org.apache.wicket.Localizer)
	 */
	public void setLocalizer(final Localizer localizer)
	{
		this.localizer = localizer;
	}

	/**
	 * @see org.apache.wicket.settings.IResourceSettings#setPackageResourceGuard(org.apache.wicket.markup.html.IPackageResourceGuard)
	 */
	public void setPackageResourceGuard(IPackageResourceGuard packageResourceGuard)
	{
		if (packageResourceGuard == null)
		{
			throw new IllegalArgumentException("Argument packageResourceGuard may not be null");
		}
		this.packageResourceGuard = packageResourceGuard;
	}

	/**
	 * @see IResourceSettings#setPropertiesFactory(org.apache.wicket.resource.IPropertiesFactory)
	 */
	public void setPropertiesFactory(org.apache.wicket.resource.IPropertiesFactory factory)
	{
		propertiesFactory = factory;
	}

	/**
	 * @see org.apache.wicket.settings.IResourceSettings#setResourceFinder(org.apache.wicket.util.file.IResourceFinder)
	 */
	public void setResourceFinder(final IResourceFinder resourceFinder)
	{
		this.resourceFinder = resourceFinder;

		// Cause resource locator to get recreated
		resourceStreamLocator = null;
	}

	/**
	 * @see org.apache.wicket.settings.IResourceSettings#setResourcePollFrequency(org.apache.wicket.util.time.Duration)
	 */
	public void setResourcePollFrequency(final Duration resourcePollFrequency)
	{
		this.resourcePollFrequency = resourcePollFrequency;
	}

	/**
	 * @see org.apache.wicket.settings.IResourceSettings#setResourceStreamLocator(org.apache.wicket.util.resource.locator.IResourceStreamLocator)
	 */
	public void setResourceStreamLocator(IResourceStreamLocator resourceStreamLocator)
	{
		this.resourceStreamLocator = resourceStreamLocator;
	}

	/**
	 * @see org.apache.wicket.settings.IResourceSettings#setThrowExceptionOnMissingResource(boolean)
	 */
	public void setThrowExceptionOnMissingResource(final boolean throwExceptionOnMissingResource)
	{
		this.throwExceptionOnMissingResource = throwExceptionOnMissingResource;
	}

	/**
	 * @see org.apache.wicket.settings.IResourceSettings#setUseDefaultOnMissingResource(boolean)
	 */
	public void setUseDefaultOnMissingResource(final boolean useDefaultOnMissingResource)
	{
		this.useDefaultOnMissingResource = useDefaultOnMissingResource;
	}

	/**
	 * @see org.apache.wicket.settings.IResourceSettings#getDefaultCacheDuration()
	 */
	public final Duration getDefaultCacheDuration()
	{
		return defaultCacheDuration;
	}

	/**
	 * @see org.apache.wicket.settings.IResourceSettings#setDefaultCacheDuration(org.apache.wicket.util.time.Duration)
	 */
	public final void setDefaultCacheDuration(Duration duration)
	{
		Args.notNull(duration, "duration");
		defaultCacheDuration = duration;
	}

	/**
	 * @see org.apache.wicket.settings.IResourceSettings#getJavascriptCompressor()
	 */
	public IJavaScriptCompressor getJavascriptCompressor()
	{
		return javascriptCompressor;
	}

	/**
	 * @see org.apache.wicket.settings.IResourceSettings#setJavascriptCompressor(org.apache.wicket.javascript.IJavaScriptCompressor)
	 */
	public IJavaScriptCompressor setJavascriptCompressor(IJavaScriptCompressor compressor)
	{
		IJavaScriptCompressor old = javascriptCompressor;
		javascriptCompressor = compressor;
		return old;
	}

	/**
	 * @see org.apache.wicket.settings.IResourceSettings#getParentFolderPlaceholder()
	 */
	public String getParentFolderPlaceholder()
	{
		return parentFolderPlaceholder;
	}

	/**
	 * @see org.apache.wicket.settings.IResourceSettings#setParentFolderPlaceholder(String)
	 */
	public void setParentFolderPlaceholder(final String sequence)
	{
		parentFolderPlaceholder = sequence;
	}

	/**
	 * @see IResourceSettings#getUseTimestampOnResources()
	 */
	public boolean getUseTimestampOnResources()
	{
		return useTimestampOnResourcesName;
	}

	/**
	 * @see org.apache.wicket.settings.IResourceSettings#setUseTimestampOnResources(boolean)
	 */
	public void setUseTimestampOnResources(boolean useTimestampOnResourcesName)
	{
		this.useTimestampOnResourcesName = useTimestampOnResourcesName;
	}
}
