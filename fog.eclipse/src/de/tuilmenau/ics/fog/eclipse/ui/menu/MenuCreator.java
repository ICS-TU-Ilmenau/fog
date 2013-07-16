/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Eclipse
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.eclipse.ui.menu;

import java.awt.Font;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.handlers.IHandlerService;

import de.tuilmenau.ics.fog.eclipse.ui.commands.CmdOpenEditor;
import de.tuilmenau.ics.fog.eclipse.ui.commands.Command;
import de.tuilmenau.ics.fog.eclipse.ui.commands.SelectionEvent;
import de.tuilmenau.ics.fog.ui.Logging;




public class MenuCreator implements IMenuCreator
{
	private static boolean LOG_INHERITANCE_PROBLEMS = false;
	
	public static final String applicationID = "de.tuilmenau.ics.fog.hostApplications";	
	
	private static final String ENTRY_NAME = "name";
	private static final String ENTRY_CLASS = "class";
	private static final String ENTRY_EDITOR_ID = "editorID";
	private static final String ENTRY_FILTER = "filter";
	private static final String ENTRY_ON_CREATION = "onCreation";
	private static final String ENTRY_DEFAULT = "default";
	private static final String ENTRY_ALLOW_MULTIPLE = "allowMultiple";
	
	private static final String ENTRY_FILTER_NULL = "null";
	
	private static final String EXTENSION_TYPE_EDITOR   = "editor"; 
	private static final String EXTENSION_TYPE_PLUGIN   = "plugin";
	private static final String EXTENSION_TYPE_SUBENTRY = "subentry";
	
	private static final int HIGHLIGHTED_ACTION_FONT_SIZE = 12;
	
	/**
	 * Normally the check requires the extension plug-in to be
	 * loaded an be available by the class loader. This is normally
	 * not the case if the extension is defined in a separate plug-in.
	 * Nevertheless, the extensions for the default base classes are
	 * defined in the same plug-in and should be available. Therefore,
	 * we report such errors only once and ignore them afterwards. 
	 */
	private static HashMap<String, Boolean> sInheritanceErrorReports = null;
	
	public MenuCreator()
	{
		mSite = null;
	}

	public MenuCreator(IWorkbenchPartSite pSite)
	{
		mSite = pSite;
	}
	
	public void setSite(IWorkbenchPartSite pSite)
	{
		mSite = pSite;
	}
	
	public String toString()
	{
		return getClass().getSimpleName();
	}

	protected boolean checkFilter(String filterClassName, Object pContext)
	{
		boolean accept = true;
		
		if(filterClassName != null) {
			if(pContext != null) {
				accept = filterClassName.equals(pContext.getClass().getName());
				
				// do we need to check for inheritance or are class names identical?
				if(!accept) {
					if(!ENTRY_FILTER_NULL.equalsIgnoreCase(filterClassName)) {
						// check for inheritance
						try {
							Class<?> filterClass = Class.forName(filterClassName);

							accept = filterClass.isInstance(pContext);
						}
						catch(ClassNotFoundException exception) {
							// error is well known since the extension might be defined in another
							// plug-in, which is not accessible by the class loader of this plug-in
							if(sInheritanceErrorReports == null) {
								sInheritanceErrorReports = new HashMap<String, Boolean>();
							}

							if(!sInheritanceErrorReports.containsKey(filterClassName)) {
								sInheritanceErrorReports.put(filterClassName, true);
								
								if (LOG_INHERITANCE_PROBLEMS){
									Logging.err(this, "Can not check for inheritance because class " +filterClassName +" not found. Maybe extension class defined in not-loaded plug-in. This message appears only once and will be suppressed in the future.");
								}
							}
						}
					}
					// else: filter for null context but context available
				}
			} else {
				// there is a filter but no context
				// -> check if the filter refers to "null" context
				accept = ENTRY_FILTER_NULL.equalsIgnoreCase(filterClassName);
			}
		}
		
		return accept;
	}
	
	private boolean isDefault(IConfigurationElement element)
	{
		String defaultStr = element.getAttribute(ENTRY_DEFAULT);
		
		if(defaultStr != null) {
			return "true".equalsIgnoreCase(defaultStr);
		} else {
			return false;
		}
	}
	
	private boolean allowsMultiple(IConfigurationElement element)
	{
		String allowMultiple = element.getAttribute(ENTRY_ALLOW_MULTIPLE);
		
		return "true".equalsIgnoreCase(allowMultiple);
	}

	private ActionListener createAction(Object pContext, IConfigurationElement element)
	{
		if(EXTENSION_TYPE_EDITOR.equals(element.getName())) {
			return new ActionEventSWTCmd(CmdOpenEditor.ID, pContext, element.getAttribute(ENTRY_EDITOR_ID), allowsMultiple(element));
		}
		else if(EXTENSION_TYPE_PLUGIN.equals(element.getName())) {
			return new ActionEventLoadPlugin(element, pContext);
		}
		else {
			Logging.warn(this, "Unknown extension type " +element.getName());
		}
		
		return null;
	}
	
	/**
	 * Recursive version for creating the context menu.
	 * Required by special case where a Node is cased to a Host.
	 * 
	 * @param selection Selected object for which the context menu should be created (null, if no object is selected)
	 * @param popup Menu, which had to be filled
	 */
	@Override
	public void fillMenu(Object pContext, Menu pMenu)
	{
		Logging.debug(this, "Context menu for: " +pContext);

		try {
			if(pMenu != null) {
				IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(applicationID);
	
				for(IConfigurationElement element : config) {
					// Check if element is accepted by the filter
					boolean accept = checkFilter(element.getAttribute(ENTRY_FILTER), pContext);
	
					if(accept) {
						MenuItem item;
	
						if(EXTENSION_TYPE_SUBENTRY.equals(element.getName())) {
							//
							// Construct sub entry
							//
							Menu submenu = new Menu(element.getAttribute(ENTRY_NAME));
	
							final Object subEntryCreator = element.createExecutableExtension(ENTRY_CLASS);
							if(subEntryCreator instanceof IMenuCreator) {
								((IMenuCreator) subEntryCreator).setSite(mSite);
								((IMenuCreator) subEntryCreator).fillMenu(pContext, submenu);
							} else {
								// output error message and display entry in menu without subentries 
								Logging.err(this, "Can not cast " +subEntryCreator +" to class IMenuCreator. Error in extension.");
							}

							item = submenu;
						} else {
							//
							// Construct menu item
							//
							item = new MenuItem(element.getAttribute(ENTRY_NAME));

							ActionListener action = createAction(pContext, element);	
							item.addActionListener(action);
							
							if(isDefault(element)) {
								Font defaultOne = item.getFont();
								Font highlightFont;
								if(defaultOne != null) {
									highlightFont = new Font(defaultOne.getFontName(), Font.BOLD, defaultOne.getSize());
								} else {
									highlightFont = new Font(null, Font.BOLD, HIGHLIGHTED_ACTION_FONT_SIZE);
								}
								item.setFont(highlightFont);
							}
						}
	
						pMenu.add(item);
					}
					// else: filter does not match -> do not include entry
				} // rof
			}
			// else: no menu -> do nothing
		}
		catch(Exception exc) {
			Logging.err(this, "Error while creating menu for " +pContext, exc);
		}
	}

	public ActionListener getDefaultAction(Object pObj)
	{
		Logging.debug(this, "Getting default action for: " +pObj);

		try {
			IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(applicationID);

			for(IConfigurationElement element : config) {
				if(isDefault(element)) {
					// Check if element is accepted by the filter.
					// It is the second check, because it is more
					// performance intense than the default check.
					if(checkFilter(element.getAttribute(ENTRY_FILTER), pObj)) {
						return createAction(pObj, element);
					}
				}
			} // rof
		}
		catch(Exception exc) {
			Logging.err(this, "Error while creating default action for " +pObj, exc);
		}
		
		return null;
	}

	public ActionListener getCreationAction(Object pNewElement)
	{
		Logging.debug(this, "Create creation action for: " +pNewElement);

		try {
			IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(applicationID);

			for(IConfigurationElement element : config) {
				String onCreation = element.getAttribute(ENTRY_ON_CREATION);
				
				// is it an "on creation" action?
				if("true".equalsIgnoreCase(onCreation)) {
					// currently we are supporting editors only, because
					// the creation is used for starting up a GUI
					if(EXTENSION_TYPE_EDITOR.equals(element.getName())) {
						// Check if element is accepted by the filter
						if(checkFilter(element.getAttribute(ENTRY_FILTER), pNewElement)) {
							return new ActionEventSWTCmd(CmdOpenEditor.ID, pNewElement, element.getAttribute(ENTRY_EDITOR_ID), allowsMultiple(element));
						}
					}
				}
			} // rof
		}
		catch(Exception exc) {
			Logging.err(this, "Error while creating creation action for " +pNewElement, exc);
		}
		
		return null;
	}
	
	/**
	 * Eclipse command extension executed by an AWT event.
	 * The Eclipse IHandlerServer extension must be registered under <code>cmdID</code>. 
	 */
	private class ActionEventSWTCmd implements ActionListener
	{
		public ActionEventSWTCmd(String cmdID, Object host, String editorID, boolean allowMultiple)
		{
			this.cmdID = cmdID;
			event = new SelectionEvent(host, editorID, allowMultiple);
		}
		
		@Override
		public void actionPerformed(ActionEvent eventAWT)
		{
			try {
				IHandlerService handlerService = (IHandlerService) mSite.getService(IHandlerService.class);
				handlerService.executeCommand(cmdID, event);
			}
			catch(Exception exception) {
				throw new RuntimeException("Can not perform action " +this, exception);
			}
		}
		
		private String cmdID;
		private SelectionEvent event;
	}
	
	/**
	 * Simulator command executed based on an AWT event.
	 * The command is loaded via its class name specified by the extension.
	 */
	private class ActionEventLoadPlugin implements ActionListener
	{
		public ActionEventLoadPlugin(IConfigurationElement element, Object selection)
		{
			this.element = element;
			this.selection = selection;
		}
		
		@Override
		public void actionPerformed(ActionEvent eventAWT)
		{
			try {
				final Object obj = element.createExecutableExtension("class");
				
				if(obj instanceof Command) {
					Thread commandThread = new Thread() {
						@Override
						public void run()
						{
							try {
								((Command) obj).init(mSite, selection);
								((Command) obj).main();
							}
							catch(Exception exception) {
								Logging.err(this, "Exception '" +exception +"' while running command '" +obj +"'.", exception);
							}
						}
					};
					
					commandThread.start();
				} else {
					Logging.err(this, "Invalid base class for extension class " +obj);
				}
			}
			catch(CoreException exception) {
				Logging.err(this, "Can not create executable extension for " +element);
			}
		}
		
		private IConfigurationElement element;
		private Object selection;
	}

	
	private IWorkbenchPartSite mSite;
}
