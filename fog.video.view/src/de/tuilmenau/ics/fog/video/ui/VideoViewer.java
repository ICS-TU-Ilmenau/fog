/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Video and audio views
 * Copyright (c) 2015, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.video.ui;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;

import de.tuilmenau.ics.fog.audio.ui.AudioListener;
import de.tuilmenau.ics.fog.eclipse.ui.editors.EditorInput;
import de.tuilmenau.ics.fog.eclipse.utils.EditorUtils;
import de.tuilmenau.ics.fog.facade.Host;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.fog.video.VideoViewerApplication;

public class VideoViewer extends EditorPart
{
	private static final int VIDEO_UPDATE_TIME = 30; // in ms
	
	private Host mHost = null;
	private Display mDisplay = null;
	private Shell mShell = null;
	private Menu mMenu = null;
	private VideoListener mVideoWorker = null;
	private AudioListener mAudioWorker = null;
	private VideoPainter mVideoPainter = null; 
	private Composite mWidget = null;
	private VideoViewerApplication mVideoViewer = null;
	
	private Runnable mVideoRepaintTimer = new Runnable () {
		public void run () 
		{
			if (mShell.isDisposed()) 
				return;
			if (mWidget == null)
				return;
			if (mWidget.isDisposed())
				return;
			mWidget.redraw();
			//Logging.log(this, "RepaintTimer");
			mDisplay.timerExec (VIDEO_UPDATE_TIME, this);
		}
	};

	public VideoViewer()
	{
		Logging.log(this, "Created live video viewer");
		mDisplay = Display.getCurrent();
		mShell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
	}

	public void dispose()
	{
		if (mVideoViewer != null)
			mVideoViewer.exit();
		Logging.log(this, "Destroyed live video viewer");
		mVideoWorker.stopGrabbing();
		super.dispose();
	}

	@Override
	public void doSave(IProgressMonitor arg0)
	{
		
	}

	@Override
	public void doSaveAs()
	{
		
	}

	@Override
	public void init(IEditorSite pSite, IEditorInput pInput) throws PartInitException 
	{
		setSite(pSite);
		setInput(pInput);
		
		// get selected object to show in editor
		Object tSelection = null;
		if (pInput instanceof EditorInput) {
			tSelection = ((EditorInput) pInput).getObj();
		}
		
		Logging.log(this, "init video viewer for " +tSelection + " (class=" +tSelection.getClass() +")");

		// configure view
		if (tSelection instanceof Host) {
			mHost = (Host) tSelection; 
		}
		else {
			throw new PartInitException("No input for editor.");
		}
	}


	@Override
	public boolean isDirty() 
	{
		return false;
	}


	@Override
	public boolean isSaveAsAllowed()
	{
		return false;
	}


	@Override
	public void createPartControl(Composite pParent) 
	{
		mVideoWorker = new VideoListener(mHost);
		// if user has canceled the video server selection, we destroy this EditorPart
		if ((mVideoWorker.getServerName() == null) || (mVideoWorker.getServerName() == "")) {
			EditorUtils.closeEditor(getSite(), this);
			return;
		}
		Logging.log(this, "Creating video editor for " + mVideoWorker.getServerName());
		mWidget = new Composite(pParent, SWT.NO_BACKGROUND);
		mMenu = new Menu(mShell, SWT.POP_UP);
		mAudioWorker = new AudioListener(mHost);
		mVideoPainter = new VideoPainter(mDisplay, mShell, pParent, mVideoWorker, mAudioWorker); 

		MenuItem tMenuItem = new MenuItem(mMenu, SWT.PUSH);
		tMenuItem.setText("Save picture");
		tMenuItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent pEvent)
			{
				MenuItem tItem = (MenuItem)pEvent.widget;
				if (tItem.getSelection()) {
					Logging.log(this, "Going to save video picture");
					mVideoPainter.savePicture();
				}
			}
		});
		
		tMenuItem = new MenuItem(mMenu, SWT.CHECK);
		tMenuItem.setText("Double buffering");
		tMenuItem.setSelection(mVideoPainter.getDoubleBuffering());
		tMenuItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent pEvent) 
			{
				MenuItem tItem = (MenuItem)pEvent.widget;
				if (mVideoPainter.getDoubleBuffering()) {
					System.out.println(tItem + "unselected");
					tItem.setSelection(false);
					mVideoPainter.setDoubleBuffering(false);
				}
				else{
					System.out.println(tItem + "selected");
					tItem.setSelection(true);
					mVideoPainter.setDoubleBuffering(true);
				}
			}
		});

		tMenuItem = new MenuItem(mMenu, SWT.CHECK);
		tMenuItem.setText("Show Stats");
		tMenuItem.setSelection(mVideoPainter.getStatActivation());
		tMenuItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent pEvent) 
			{
				MenuItem tItem = (MenuItem)pEvent.widget;
				if (mVideoPainter.getStatActivation()) {
					System.out.println(tItem + "unselected");
					tItem.setSelection(false);
					mVideoPainter.setStatActivation(false);
				}
				else{
					System.out.println(tItem + "selected");
					tItem.setSelection(true);
					mVideoPainter.setStatActivation(true);
				}
			}
		});

		mWidget.setMenu(mMenu);
		//TODO: needed anymore? mWidget.setSize(ConfigVideo.RES_X, ConfigVideo.RES_Y);
		mWidget.addPaintListener(mVideoPainter);
		mDisplay.timerExec(100, mVideoRepaintTimer);
		mVideoViewer = new VideoViewerApplication(mHost, null);
	}

	@Override
	public void setFocus() 
	{
		
	}
}
