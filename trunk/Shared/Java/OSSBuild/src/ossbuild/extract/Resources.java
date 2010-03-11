
package ossbuild.extract;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import ossbuild.Namespaces;
import ossbuild.StringUtil;
import static ossbuild.extract.ResourceUtils.*;

/**
 * Reads a simple XML file that describes files to be extracted.
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class Resources {
	//<editor-fold defaultstate="collapsed" desc="Constants">
	public static final String
		  ATTRIBUTE_NAME		= "name"
	;

	public static final Resources
		  Empty = new Resources(StringUtil.empty, new IResourcePackage[0])
	;

	public static final Future CompletedFuture = new Future() {
		@Override
		public final boolean cancel(boolean bln) {
			return true;
		}

		@Override
		public final boolean isCancelled() {
			return false;
		}

		@Override
		public final boolean isDone() {
			return true;
		}

		@Override
		public final Object get() throws InterruptedException, ExecutionException {
			return null;
		}

		@Override
		public final Object get(long l, TimeUnit tu) throws InterruptedException, ExecutionException, TimeoutException {
			return null;
		}
	};
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Variables">
	protected Registry.Reference registryReference = null;
	protected String name;
	protected boolean loaded = false;
	protected boolean processed = false;
	protected final Object lock = new Object();
	protected long totalResourceSize;
	protected int totalResourceCount;
	protected IResourcePackage[] packages;
	protected String[] references;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	public Resources(final IResourcePackage... Packages) {
		initFromPackages(StringUtil.empty, null, Packages);
	}

	public Resources(final String Name, final IResourcePackage... Packages) {
		initFromPackages(Name, null, Packages);
	}

	public Resources(final String Name, final String[] References, final IResourcePackage... Packages) {
		initFromPackages(Name, References, Packages);
	}
	
	public Resources(final IVariableProcessor VariableProcessor, final InputStream XMLData) throws XPathException, ParserConfigurationException, SAXException, IOException {
		initFromXML(ResourceProcessorFactory.DEFAULT_INSTANCE, VariableProcessor, XMLData);
	}

	public Resources(final ResourceProcessorFactory ProcessorFactory, final IVariableProcessor VariableProcessor, final InputStream XMLData) throws XPathException, ParserConfigurationException, SAXException, IOException {
		initFromXML(ProcessorFactory, VariableProcessor, XMLData);
	}

	protected void initFromPackages(final String Name, final String[] References, final IResourcePackage[] Packages) {
		if (Packages == null)
			throw new NullPointerException("Processors cannot be null");

		this.name = Name;
		this.packages = Packages;
		this.references = References;
		
		initAfter();
	}

	protected void initFromXML(final ResourceProcessorFactory ProcessorFactory, final IVariableProcessor VariableProcessor, final InputStream XMLData) throws XPathException, ParserConfigurationException, SAXException, IOException {
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		final DocumentBuilder builder = factory.newDocumentBuilder();
		final XPathFactory xpathFactory = XPathFactory.newInstance();
		final XPath xpath = xpathFactory.newXPath();

		//Creates a context that always has "ossbuild" as a prefix -- useful for xpath evaluations
		xpath.setNamespaceContext(Namespaces.createNamespaceContext());

		this.packages = read(ProcessorFactory, VariableProcessor, xpath, builder.parse(XMLData));

		initAfter();
	}

	protected void initAfter() {
		calculateTotals();
		loaded = true;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Helper Methods">
	protected void calculateTotals() {
		//Calculate total size and resource count
		for(IResourcePackage pkg : packages) {
			if (pkg == null)
				continue;

			totalResourceCount += pkg.getTotalResourceCount();
			totalResourceSize += pkg.getTotalSize();
		}
	}

	protected void notifyProgressBegin(IResourceProgressListener progress, long startTime) {
		if (progress != null)
			progress.begin(getTotalResourceCount(), getTotalPackageCount(), getTotalResourceSize(), startTime);
	}

	protected void notifyProgressReportResourceCompleted(IResourceProgressListener progress, IResourceProcessor resource, IResourcePackage pkg, long totalBytes, int totalResources, int totalPkgs, long startTime, String message) {
		if (progress != null)
			progress.reportResourceComplete(resource, pkg, getTotalResourceCount(), getTotalPackageCount(), getTotalResourceSize(), totalBytes, totalResources, totalPkgs, startTime, Math.abs(System.currentTimeMillis() - startTime), message);
	}

	protected void notifyProgressReportPackageCompleted(IResourceProgressListener progress, IResourcePackage pkg, long totalBytes, int totalResources, int totalPkgs, long startTime, String message) {
		if (progress != null)
			progress.reportPackageComplete(pkg, getTotalResourceCount(), getTotalPackageCount(), getTotalResourceSize(), totalBytes, totalResources, totalPkgs, startTime, Math.abs(System.currentTimeMillis() - startTime), message);
	}

	protected void notifyProgressError(IResourceProgressListener progress, Throwable exception, String message) {
		if (progress != null)
			progress.error(exception, message);
	}

	protected void notifyProgressEnd(IResourceProgressListener progress, boolean success, long totalBytes, int totalResources, int totalPkgs, long startTime, long endTime) {
		if (progress != null)
			progress.end(success, getTotalResourceCount(), getTotalPackageCount(), getTotalResourceSize(), totalBytes, totalResources, totalPkgs, startTime, endTime);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters">
	public String getName() {
		return name;
	}
	
	public boolean isLoaded() {
		return loaded;
	}

	public boolean isProcessed() {
		return processed;
	}

	public String[] getReferences() {
		return references;
	}
	
	public int getTotalPackageCount() {
		return packages.length;
	}
	
	public int getTotalResourceCount() {
		return totalResourceCount;
	}

	public long getTotalResourceSize() {
		return totalResourceSize;
	}
	
	public IResourcePackage[] getPackages() {
		return packages;
	}

	public Registry.Reference getRegistryReference() {
		return registryReference;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Methods">
	//<editor-fold defaultstate="collapsed" desc="Overloads">
	public Future extract(final IResourceCallback callback) {
		final ExecutorService svc = createUnprivilegedExecutorService();
		try {
			return extract(packages, svc, IResourceFilter.None, IResourceProgressListener.None, callback);
		} finally {
			svc.shutdown();
		}
	}

	public Future extract(final IResourceProgressListener progress) {
		final ExecutorService svc = createUnprivilegedExecutorService();
		try {
			return extract(packages, svc, IResourceFilter.None, progress, IResourceCallback.None);
		} finally {
			svc.shutdown();
		}
	}

	public Future extract(final IResourceFilter filter) {
		final ExecutorService svc = createUnprivilegedExecutorService();
		try {
			return extract(packages, svc, filter, IResourceProgressListener.None, IResourceCallback.None);
		} finally {
			svc.shutdown();
		}
	}

	public Future extract(final IResourceFilter filter, final IResourceCallback callback) {
		final ExecutorService svc = createUnprivilegedExecutorService();
		try {
			return extract(packages, svc, filter, IResourceProgressListener.None, callback);
		} finally {
			svc.shutdown();
		}
	}

	public Future extract(final IResourceProgressListener progress, final IResourceCallback callback) {
		final ExecutorService svc = createUnprivilegedExecutorService();
		try {
			return extract(packages, svc, IResourceFilter.None, progress, callback);
		} finally {
			svc.shutdown();
		}
	}

	public Future extract(final IResourceFilter filter, final IResourceProgressListener progress, final IResourceCallback callback) {
		final ExecutorService svc = createUnprivilegedExecutorService();
		try {
			return extract(packages, svc, filter, progress, callback);
		} finally {
			svc.shutdown();
		}
	}
	//</editor-fold>

	/* public boolean preprocessEnvVars() {
		return processEnvVars(packages, IResourceFilter.None);
	}
	
	public boolean preprocessEnvVars(final IResourceFilter filter) {
		return processEnvVars(packages, filter);
	} /**/

	public Future extract(final IResourcePackage[] pkgs, final ExecutorService executor, final IResourceFilter filter, final IResourceProgressListener progress, final IResourceCallback callback) {
		synchronized(lock) {
			if (processed)
				return CompletedFuture;

			return executor.submit(new Runnable() {
				@Override
				public void run() {
					synchronized(lock) {
						try {
							//<editor-fold defaultstate="collapsed" desc="Make compiler happy">
							if (false)
								throw new InterruptedException();
							//</editor-fold>

							//<editor-fold defaultstate="collapsed" desc="Prepare">
							if (callback != null)
								callback.prepare(Resources.this);
							//</editor-fold>

							if (!processed) {
								//Do the real work
								extractResources(pkgs, filter, progress);

								//Mark as processed
								processed = true;
							}

							//<editor-fold defaultstate="collapsed" desc="Completed">
							if (callback != null)
								callback.completed(Resources.this);
							//</editor-fold>
						} catch(InterruptedException ie) {
							//<editor-fold defaultstate="collapsed" desc="Cancelled">
							if (callback != null)
								callback.cancelled(Resources.this);
							//</editor-fold>
						} catch(CancellationException ce) {
							//<editor-fold defaultstate="collapsed" desc="Cancelled">
							if (callback != null)
								callback.cancelled(Resources.this);
							//</editor-fold>
						} catch(RejectedExecutionException ree) {
							//<editor-fold defaultstate="collapsed" desc="Cancelled">
							if (callback != null)
								callback.cancelled(Resources.this);
							//</editor-fold>
						} catch(Throwable t) {
							//<editor-fold defaultstate="collapsed" desc="Error">
							if (callback != null)
								callback.error(Resources.this);
							//</editor-fold>
						}
					}
				}
			});
		}
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Static Methods">
	//<editor-fold defaultstate="collapsed" desc="extractAll">
	public static final Future extractAll(final IResourcePackage... packages) {
		final ExecutorService svc = createUnprivilegedExecutorService();
		try {
			return newInstance(packages).extract(packages, svc, IResourceFilter.None, IResourceProgressListener.None, IResourceCallback.None);
		} finally {
			svc.shutdown();
		}
	}

	public static final Future extractAll(final IResourceCallback callback, final IResourcePackage... packages) {
		final ExecutorService svc = createUnprivilegedExecutorService();
		try {
			return newInstance(packages).extract(packages, svc, IResourceFilter.None, IResourceProgressListener.None, callback);
		} finally {
			svc.shutdown();
		}
	}

	public static final Future extractAll(final IResourceProgressListener progress, final IResourceCallback callback, final IResourcePackage... packages) {
		final ExecutorService svc = createUnprivilegedExecutorService();
		try {
			return newInstance(packages).extract(packages, svc, IResourceFilter.None, progress, callback);
		} finally {
			svc.shutdown();
		}
	}

	public static final Future extractAll(final IResourceFilter filter, final IResourceProgressListener progress, final IResourceCallback callback, final IResourcePackage... packages) {
		final ExecutorService svc = createUnprivilegedExecutorService();
		try {
			return newInstance(packages).extract(packages, svc, filter, progress, callback);
		} finally {
			svc.shutdown();
		}
	}

	public static final Future extractAll(final ExecutorService executor, final IResourceFilter filter, final IResourceProgressListener progress, final IResourceCallback callback, final IResourcePackage... packages) {
		return newInstance(packages).extract(packages, executor, filter, progress, callback);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="newInstance">
	public static final Resources newInstance(final IResourcePackage... Packages) {
		return new Resources(Packages);
	}

	public static final Resources newInstance(final String Name, final IResourcePackage... Packages) {
		return new Resources(Name, Packages);
	}

	public static final Resources newInstance(final String ResourceName) {
		return newInstance(VariableProcessorFactory.newInstance(), ResourceName);
	}

	public static final Resources newInstance(final File XMLFile) {
		return newInstance(VariableProcessorFactory.newInstance(), XMLFile);
	}

	public static final Resources newInstance(final InputStream XMLData) {
		return newInstance(VariableProcessorFactory.newInstance(), XMLData);
	}

	public static final Resources newInstance(final ResourceProcessorFactory ProcessorFactory, final String ResourceName) {
		return newInstance(ProcessorFactory, VariableProcessorFactory.newInstance(), ResourceName);
	}

	public static final Resources newInstance(final ResourceProcessorFactory ProcessorFactory, final File XMLFile) {
		return newInstance(ProcessorFactory, VariableProcessorFactory.newInstance(), XMLFile);
	}

	public static final Resources newInstance(final ResourceProcessorFactory ProcessorFactory, final InputStream XMLData) {
		return newInstance(ProcessorFactory, VariableProcessorFactory.newInstance(), XMLData);
	}

	public static final Resources newInstance(final IVariableProcessor VariableProcessor, final String ResourceName) {
		return newInstance(ResourceProcessorFactory.DEFAULT_INSTANCE, VariableProcessor, ResourceName);
	}

	public static final Resources newInstance(final IVariableProcessor VariableProcessor, final File XMLFile) {
		return newInstance(ResourceProcessorFactory.DEFAULT_INSTANCE, VariableProcessor, XMLFile);
	}

	public static final Resources newInstance(final IVariableProcessor VariableProcessor, final InputStream XMLData) {
		return newInstance(ResourceProcessorFactory.DEFAULT_INSTANCE, VariableProcessor, XMLData);
	}

	public static final Resources newInstance(final ResourceProcessorFactory ProcessorFactory, final IVariableProcessor VariableProcessor, final String ResourceName) {
		return newInstance(ProcessorFactory, VariableProcessor, Resources.class.getResourceAsStream(ResourceName));
	}

	public static final Resources newInstance(final ResourceProcessorFactory ProcessorFactory, final IVariableProcessor VariableProcessor, final File XMLFile) {
		FileInputStream fis = null;
		try {
			return newInstance(ProcessorFactory, VariableProcessor, (fis = new FileInputStream(XMLFile)));
		} catch(ResourceException t) {
			throw t;
		} catch(Throwable t) {
			throw new ResourceException("Unable to preprocess resource file", t);
		} finally {
			try {
				if (fis != null)
					fis.close();
			} catch(IOException ie) {
			}
		}
	}

	public static final Resources newInstance(final ResourceProcessorFactory ProcessorFactory, final IVariableProcessor VariableProcessor, final InputStream XMLData) {
		try {
			return new Resources(VariableProcessor, XMLData);
		} catch(ResourceException t) {
			throw t;
		} catch(Throwable t) {
			throw new ResourceException("Unable to preprocess resource file", t);
		}
	}
	//</editor-fold>
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="The Meat">
	protected IResourcePackage[] read(final ResourceProcessorFactory processorFactory, final IVariableProcessor variableProcessor, final XPath xpath, final Document document) throws XPathException  {
		Node node;
		NodeList lst;
		IResourcePackage pkg;
		List<IResourcePackage> pkgs = new ArrayList<IResourcePackage>(1);

		//Collapse whitespace nodes
		document.normalize();

		//Get the top-level document element, <Resources />
		final Element top = document.getDocumentElement();

		//<editor-fold defaultstate="collapsed" desc="Resource name">
		if ((node = (Node)xpath.evaluate("//Resources", top, XPathConstants.NODE)) != null) {
			this.name = stringAttributeValue(variableProcessor, StringUtil.empty, node, ATTRIBUTE_NAME);

			if (StringUtil.isNullOrEmpty(name))
				throw new ResourceException("Invalid resource name. It cannot be empty.");

			this.registryReference = Registry.add(name, this);
		}
		//</editor-fold>

		//<editor-fold defaultstate="collapsed" desc="References">
		//Locate <References><Add /></References> tags
		if ((lst = (NodeList)xpath.evaluate("//Resources/References/Add", top, XPathConstants.NODESET)) != null && lst.getLength() > 0) {
			final List<String> refs = new ArrayList<String>(lst.getLength());

			//Iterate over every <Add /> tag
			for(int i = 0; i < lst.getLength() && (node = lst.item(i)) != null; ++i) {
				final String refName = stringAttributeValue(variableProcessor, StringUtil.empty, node, ATTRIBUTE_NAME);
				if (!StringUtil.isNullOrEmpty(refName) && !this.name.equalsIgnoreCase(refName))
					refs.add(refName);
			}

			this.references = refs.toArray(new String[refs.size()]);
		}
		//</editor-fold>

		//<editor-fold defaultstate="collapsed" desc="Packages">
		//Locate <Extract /> tags
		if ((lst = (NodeList)xpath.evaluate("//Resources/Extract", top, XPathConstants.NODESET)) == null || lst.getLength() <= 0)
			return IResourcePackage.EMPTY;

		//Iterate over every <Extract /> tag
		for(int i = 0; i < lst.getLength() && (node = lst.item(i)) != null; ++i) {

			//Ask the package to read it
			if ((pkg = Package.newInstance(processorFactory, variableProcessor, node, xpath, document)) != null)
				pkgs.add(pkg);
		}
		//</editor-fold>

		//Create an array and return it
		return pkgs.toArray(new IResourcePackage[pkgs.size()]);
	}

	/*protected boolean processEnvVars(final IResourcePackage[] pkgs, final IResourceFilter filter) {
		//Please note that this is executed in whatever the caller thread is

		String resourceName;
		boolean success = true;

		try {
			for(IResourcePackage pkg : pkgs) {
				if (pkg == null)
					continue;

				for(IResourceProcessor p : pkg) {
					if (p == null || !(p instanceof EnvVarProcessor))
						continue;

					//Double check that we're allowed to process this resource
					resourceName = pkg.resourcePath(p.getName());
					if (filter != null && !filter.filter(pkg, p, resourceName))
						continue;

					//Here we go!
					if (!p.isProcessed())
						success = success && p.process(resourceName, pkg, IResourceProgressListener.None);
				}
			}
		} catch(Throwable t) {
			success = false;
			throw new ResourceException("Error while processing environment variables", t);
		} finally {
			return success;
		}
	} /**/

	protected void extractResources(final IResourcePackage[] pkgs, final IResourceFilter filter, final IResourceProgressListener progress) throws Exception {
		//Please note that this is executed in a separate thread.
		//It can be cancelled or interrupted at any time.

		int totalPkgs = 0;
		int totalResources = 0;
		long totalBytes = 0;
		String resourceName;
		long endTime;
		boolean success = true;
		Throwable exception = null;
		String title = null;
		long startTime = System.currentTimeMillis();

		try {
			//Notify begin
			notifyProgressBegin(progress, startTime);

			for(IResourcePackage pkg : pkgs) {
				if (pkg == null) {
					notifyProgressReportPackageCompleted(progress, pkg, totalBytes, totalResources, ++totalPkgs, startTime, "Skipped package");
					continue;
				}

				for(IResourceProcessor p : pkg) {
					if (p == null) {
						notifyProgressReportResourceCompleted(progress, p, pkg, totalBytes, ++totalResources, totalPkgs, startTime, "Skipped resource");
						continue;
					}

					//Double check that we're allowed to process this resource
					resourceName = pkg.resourcePath(p.getName());
					if (filter != null && !filter.filter(pkg, p, resourceName)) {
						notifyProgressReportResourceCompleted(progress, p, pkg, totalBytes, ++totalResources, totalPkgs, startTime, "Skipped resource");
						continue;
					}

					//Here we go!
					//If this was already processed, then skip the processing again
					if (p.isProcessed() || p.process(resourceName, pkg, progress)) {
						totalBytes += p.getSize();
						title = (p instanceof DefaultResourceProcessor ? ((DefaultResourceProcessor)p).getTitle() : p.getName());
						if (StringUtil.isNullOrEmpty(title))
							title = p.getName();
						notifyProgressReportResourceCompleted(progress, p, pkg, totalBytes, ++totalResources, totalPkgs, startTime, title);
					}
				}

				notifyProgressReportPackageCompleted(progress, pkg, totalBytes, totalResources, ++totalPkgs, startTime, "Completed package");
			}
			success = true;
		} catch(Throwable t) {
			success = false;
			exception = t;
		} finally {
			endTime = System.currentTimeMillis();
		}

		if (exception != null)
			notifyProgressError(progress, exception, !StringUtil.isNullOrEmpty(exception.getMessage()) ? exception.getMessage() : "Error loading resources");
		notifyProgressEnd(progress, success, totalBytes, totalResources, totalPkgs, startTime, endTime);
	}
	//</editor-fold>
}
