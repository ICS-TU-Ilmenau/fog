/*******************************************************************************
 * Middleware
 * Copyright (C) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * This program and the accompanying materials are dual-licensed under either
 * the terms of the Eclipse Public License v1.0 as published by the Eclipse
 * Foundation
 *  
 *   or (per the licensee's choosing)
 *  
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 ******************************************************************************/
package de.tuilmenau.ics.middleware;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.Remote;
import java.util.HashMap;
import java.util.LinkedList;

import net.jini.config.Configuration;
import net.jini.config.ConfigurationNotFoundException;
import net.jini.config.ConfigurationProvider;
import net.jini.core.discovery.LookupLocator;
import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceMatches;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.lookup.ServiceRegistration;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.export.Exporter;
import net.jini.jeri.BasicILFactory;
import net.jini.jeri.BasicJeriExporter;
import net.jini.jeri.tcp.TcpServerEndpoint;
import net.jini.lease.LeaseRenewalManager;
import net.jini.lookup.entry.Name;


/**
 * Helper functions to use Jini. These functions shield the source code from the
 * dependencies to Jini and provides a simpler interface.
 */
public class JiniHelper
{
	// Name of the configuration file for the export configuration of Jini.
	private static final String CONFIG_FILE = "jeri.config";
	
	// indicates if configuration file MUST be used or if alternative configurations can be used
	private static final boolean CONFIG_FILE_ENFORCE_USAGE = false;
	
	private static final String CONFIG_NAME_DEFAULT = "Default";
	
	// Address of the lookup service
	private static final String LOOKUP_SERVICE_ENV_KEY = "jini.lookup";
	private static final String LOOKUP_SERVICE_STD = "localhost";
	private static final String LOOKUP_SERVICE_BASE = "jini://";
	private static String LOOKUP_SERVICE_ADR = null;
	
	// stores all exporters for exported objects
	private static HashMap<Remote, Exporter> sExporters = new HashMap<Remote, Exporter>();
	
	// registered services
	private static HashMap<Class<?>, HashMap<Object, ServiceEntry>> sServices = new HashMap<Class<?>, HashMap<Object, ServiceEntry>>();
	
	// stores all service registrations
	private static HashMap<Object, ServiceRegistration> sRegistrations = new HashMap<Object, ServiceRegistration>();
	
	// manages all leases from registered services
	private static LeaseRenewalManager sLeaseManager = new LeaseRenewalManager();
	
	// intermediate lifetime for the registrations
	private static final long REGISTRATION_LIFETIME_MSEC = 1000 * 60 * 1;
	
	// flag, if Jini should be used
	private static boolean sJiniEnabled = true;
	
	// flag indicating that a warning should be shown if configuration file can not be loaded
	private static boolean sShowConfigurationFileWarning = true;

	// main instance created with class loader of plug-in 
	private static JiniHelper sInstance = null;
	
	/**
	 * @return JiniHelper singleton instance (!= null)
	 */
	public static JiniHelper getInstance()
	{
		if(sInstance == null) sInstance = new JiniHelper();
		
		return sInstance; 
	}
	
	/**
	 * Queries look-up service for service matching a pattern.
	 * 
	 * @param pServiceClass class of the required service (required)
	 * @param pName name of the required service (optional)
	 * @return List of service objects matching the parameters of the function (!= null)
	 */
	static public LinkedList<Object> getServices(Class<?> pServiceClass, String pName)
	{
		ServiceMatches jiniServices = getJiniServices(pServiceClass, pName);
		LinkedList<Object> foundServices = new LinkedList<Object>();
		ServiceItem[] jiniItems = null;
		int removedJiniItems = 0;
		
		if(jiniServices != null) jiniItems = jiniServices.items;
		if(jiniItems == null) jiniItems = new ServiceItem[0];
		
		HashMap<Object, ServiceEntry> entrys = sServices.get(pServiceClass);
		if(entrys != null) {
			// prevent concurrent modifications in the hash maps during for loop
			synchronized(entrys) {
				for(ServiceEntry service : entrys.values()) {
					boolean add = false;
					
					if(pName == null) add = true;
					else {
						if(service.name != null) {
							add = pName.equals(service.name);
						}
					}
						
					if(add) {
						foundServices.add(service.service);
						
						// Check if the local service appears in the Jini
						// list. If so, remove it from the list, because
						// we would like to use the local one directly.
						removedJiniItems += removeProxyFromList(jiniItems, service.proxy);
					}
				}
			}
		}
		
		// combine local and Jini list to one result list
		for(int i = 0; i < jiniItems.length; i++) {
			if(jiniItems[i] != null) {
				foundServices.addLast(jiniItems[i].service);
			}
		}
		
		return foundServices;
	}


	private static int removeProxyFromList(ServiceItem[] jiniItems, Object proxy)
	{
		int removed = 0;
		
		if(proxy != null) {
			for(int i = 0; i < jiniItems.length; i++) {
				if(jiniItems[i] != null) {
					if(proxy.equals(jiniItems[i].service)) {
						jiniItems[i] = null;
						removed++;
					}
				}
			}
		}

		return removed;
	}


	/**
	 * Queries Jini look-up service for a single service matching a pattern.
	 * 
	 * @param pServiceClass class of the required service (required)
	 * @param pName name of the required service (optional)
	 * @return service object from Jini matching the parameters of the function (null if no available)
	 */
	static public Object getService(Class<?> pServiceClass, String pName)
	{
		LinkedList<Object> services = getServices(pServiceClass, pName);
		
		if(services.size() > 0) {
			return services.getFirst();
		}
		
		return null;
	}
	
	/**
	 * Queries Jini look-up service for services matching a pattern.
	 * 
	 * @param pServiceClass class of the required service (required)
	 * @param pName name of the required service (optional)
	 * @return service objects from Jini matching the parameters of the function (null if no available)
	 */
	static private ServiceMatches getJiniServices(Class<?> pServiceClass, String pName)
	{
		LookupLocator tLookup = null;
		ServiceRegistrar tRegistrar = null;
		ServiceMatches tFoundServices = null;
		
		if(sJiniEnabled) {
			try {    
				// versuche Lookup-Dienst zu kontaktieren
				tLookup = getLookupService();
				tRegistrar = tLookup.getRegistrar();
				
				// Suchmuster spezifizieren
				// => hier wird nur eine unterstuetzte Basisklasse/Interface gefordert
				Class<?>[] tClasses = new Class[] {pServiceClass};
				Entry tDescription[] = null;
				
				if(pName != null) {
					tDescription = new Entry[] { new Name(pName) };
				}
				
				ServiceTemplate template = new ServiceTemplate(null, tClasses, tDescription);
				
				// Suche starten
				tFoundServices = tRegistrar.lookup(template, 50);
				
				return tFoundServices;
			}
			catch(Exception tExc) {
				// do not print full stack trace here, because error is (hopefully) well known
				warn("Disable Jini due to error \"" + tExc.getLocalizedMessage() + "\".", null);
				sJiniEnabled = false;
			}
		}
		
		return null;
	}

	static public void registerService(Class<?> pClass, Object pService, String pName)
	{
		if(pService != null) {
			// default: class of service
			if(pClass == null) pClass = pService.getClass();
			
			HashMap<Object, ServiceEntry> entrys = sServices.get(pClass);
			
			if(entrys == null) {
				entrys = new HashMap<Object, ServiceEntry>();
				sServices.put(pClass, entrys);
			}
			
			Object proxy;
			
			if(pService instanceof Remote) {
				// TODO suitable config name management
				//Remote proxy = export(pClass.getCanonicalName(), pService);
				proxy = getInstance().export(null, (Remote) pService);
			}
			else if(pService instanceof Serializable) {
				// Register object itself
				proxy = pService;
			}
			else {
				throw new RuntimeException(JiniHelper.class +" - Can not register service " +pService +" since it is neither Remote nor Serializable.");
			}
			
			ServiceEntry entry = new ServiceEntry(pService, pName, proxy);
			synchronized (entrys) {
				entrys.put(pService, entry);
			}
			
			registerService(proxy, pName);
		}
	}
	
	/**
	 * Register a proxy at the look-up service of Jini.
	 * In most cases, the proxy will be generated by the export function. 
	 * 
	 * @param pProxy Serialisable proxy object
	 * @return true if registration successful; false on error
	 */
	static private boolean registerService(Object pProxy, String pName)
	{
		LookupLocator lookup = null;
		ServiceRegistrar registrar = null;
		
		if(sJiniEnabled) {
			try {
				// versuche Lookup-Dienst zu kontaktieren
				lookup = getLookupService();
				registrar = lookup.getRegistrar();
	
				Entry tDescription[] = null;
				
				if(pName != null) {
					tDescription = new Entry[] {
						new Name(pName)
						//new Location("localhost", "homenetwork", "earch"),
						//new Comment("service")
						};
				}
				
				// Registriere Dienst
				// - mit Kommentaren
				// - und einer Laufzeit
				ServiceItem item = new ServiceItem(null, pProxy, tDescription);
	
				ServiceRegistration reg = registrar.register(item, REGISTRATION_LIFETIME_MSEC);
				sRegistrations.put(pProxy, reg);
	
				sLeaseManager.renewFor(reg.getLease(), Lease.FOREVER, null);
				return true;
			} catch(Exception tExc) {
				// do not print full stack trace here, because error is (hopefully) well known
				warn("Disable Jini due to error \"" + tExc.getLocalizedMessage() + "\".", null);
				sJiniEnabled = false;
			}
		}
		
		return false;
	}

	/**
	 * Generates a LookupLocator based on the VM parameters or
	 * the hard coded default parameters.
	 * 
	 * @return Lookup Reference to lookup service for Jini services
	 * @throws MalformedURLException if name is not a URL
	 */
	private static LookupLocator getLookupService() throws MalformedURLException
	{
		if(LOOKUP_SERVICE_ADR == null) {
			String envValue = System.getProperty(LOOKUP_SERVICE_ENV_KEY);
			// Is environment variable defined?
			// If not, use hard coded default.
			if(envValue != null) {
				LOOKUP_SERVICE_ADR = LOOKUP_SERVICE_BASE +envValue;
				
				log("Using lookup service at " +LOOKUP_SERVICE_ADR);
			} else {
				LOOKUP_SERVICE_ADR = LOOKUP_SERVICE_BASE +LOOKUP_SERVICE_STD;
				
				log("Using default lookup service at " +LOOKUP_SERVICE_ADR);
			}
		}
		
		LookupLocator lookup = new LookupLocator(LOOKUP_SERVICE_ADR);
		return lookup;
	}
	
	/**
	 * Remove registered proxy from the Jini look-up service
	 * 
	 * @param pProxy which should be removed
	 * @return true if successful; false on error
	 */
	static private boolean unregisterService(Object pProxy)
	{
		ServiceRegistration tReg = sRegistrations.get(pProxy);
		
		if(tReg != null) {
			try {
				sLeaseManager.cancel(tReg.getLease());
			} catch (Exception tExc) {
				// Ignore exception if lease is not defined anyway
				tExc.printStackTrace();
			}
			
			sRegistrations.remove(pProxy);
			return true;
		}
		
		return false;
	}
	
	static public boolean unregisterService(Class<?> pClass, Object pService)
	{
		if(pService != null) {
			if(pClass == null) pClass = pService.getClass();
			HashMap<Object, ServiceEntry> entrys = sServices.get(pClass);
			ServiceEntry entry = null;
			
			// check if it can be found directly
			if(entrys != null) {
				synchronized (entrys) {
					entry = entrys.remove(pService);
				}
			}
			// not found? -> search for it in all categories
			if(entry == null) {
				entry = removeServiceBySearch(pService);
			}
			
			if(entry != null) {
				if(entry.proxy != null) {
					unregisterService(entry.proxy);
					unexport(entry.proxy);
				}
				// else: Jini not used; nothing to do.
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}
	
	static private ServiceEntry removeServiceBySearch(Object pService)
	{
		for(HashMap<Object, ServiceEntry> entrys : sServices.values()) {
			synchronized (entrys) {
				ServiceEntry result = entrys.remove(pService);
				
				if(result != null) return result;
			}
		}
		
		return null;
	}
	
	
	/**
	 * Function exports an object. Afterwards, object is available for
	 * external access via Jini.
	 *  
	 * @param pConfigName Key name in configuration file for the export configuration 
	 * @param pServerObject Object, which should be available via Jini
	 * @return Proxy object, with represents the pServerObject within Jini
	 */
	public Remote export(String pConfigName, Remote pServerObject)
	{
		if(sJiniEnabled) {
			if(pConfigName == null) pConfigName = CONFIG_NAME_DEFAULT;
			
			try {
				//
				// Try to read type of exporter generation from configuration file
				//
				Configuration tConfig = null;
				
				try {
					// Try to load config file via the class loader.
					// That is especially important for the Eclipse environment.
					URL fileUrl = pServerObject.getClass().getClassLoader().getResource("/" +CONFIG_FILE);
					
					if(fileUrl != null) {
						tConfig = ConfigurationProvider.getInstance(new String[] { fileUrl.toExternalForm() });
					} else {
						tConfig = ConfigurationProvider.getInstance(new String[] { CONFIG_FILE }); 
					}
				}
				catch(ConfigurationNotFoundException tExc) {
					// are we allowed to switch do some other configuration?
					if(CONFIG_FILE_ENFORCE_USAGE) {
						throw tExc;
					} else {
						// show warning just once
						if(sShowConfigurationFileWarning) {
							sShowConfigurationFileWarning = false;
							
							warn("Can not open configuration file '" +CONFIG_FILE +"'. Fallback to default configuration.", null);
						}
					}
				}
				
				//
				// Create exporter
				//
				Exporter tExporter;
				if(tConfig != null) {
					// setup from configuration file
					tExporter = (Exporter) tConfig.getEntry(pConfigName, 
											"Exporter", 
											Exporter.class);
				} else {
					// setup with default configuration
					tExporter = new BasicJeriExporter(TcpServerEndpoint.getInstance(0), new BasicILFactory());
				}
				
				// save exporter in cache
				sExporters.put(pServerObject, tExporter);
		
				//
				// Export object for remote access
				//
				Remote tProxy = tExporter.export(pServerObject);
				return tProxy;
			}
			catch(Exception tExc) {
				err("Can not generate proxy for '" +pServerObject +"'", tExc);
			}
		}
		
		return null;
	}
	
	/**
	 * Shuts down remote call features of an object.
	 * 
	 * @param pServerObject which should be disconnected
	 * @return true=successfull; false=error or object not exported
	 */
	static private boolean unexport(Object pServerObject)
	{
		Exporter tExporter = sExporters.get(pServerObject);
		
		if(tExporter != null) {
			if(tExporter.unexport(true)) {
				sExporters.remove(pServerObject);
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Closes all exported objects which shut down their remote call reachability.
	 * Unregisters all services.
	 */
	static public void cleanUp()
	{
		while(sExporters.size() > 0) {
			unexport(sExporters.keySet().iterator().next());
		}
		
		while(sRegistrations.size() > 0) {
			unregisterService(sRegistrations.keySet().iterator().next());
		}
		
		sLeaseManager.clear();		
	}

	public static boolean isEnabled()
	{
		return sJiniEnabled;
	}
	
	private static void log(String pMessage)
	{
		System.out.println(JiniHelper.class +" - LOG: " +pMessage);
	}
	
	private static void warn(String pMessage, Throwable pExc)
	{
		System.err.println(JiniHelper.class +" - WARN: " +pMessage);
		if(pExc != null) {
			pExc.printStackTrace(System.err);
		}
	}
	
	private static void err(String pMessage, Throwable pExc)
	{
		System.err.println(JiniHelper.class +" - ERR: " +pMessage);
		if(pExc != null) {
			pExc.printStackTrace(System.err);
		}
	}
}

