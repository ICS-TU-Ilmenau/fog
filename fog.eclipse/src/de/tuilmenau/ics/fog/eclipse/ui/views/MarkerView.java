/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Eclipse
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.eclipse.ui.views;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.part.ViewPart;

import de.tuilmenau.ics.fog.eclipse.ui.editors.SelectionProvider;
import de.tuilmenau.ics.fog.ui.IMarkerContainerObserver;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.fog.ui.Marker;
import de.tuilmenau.ics.fog.ui.MarkerContainer;


/**
 * Class for showing all markers.
 */
public class MarkerView extends ViewPart implements IMarkerContainerObserver, ISelectionChangedListener
{
	private static final String[] COLUMN_TITLES = { "Marker", "Color" };
	private static final int[]    COLUMN_SIZE = { 200, 150 };
	
	private static final int COLUMN_ID_NAME   = 0;
	private static final int COLUMN_ID_COLOR  = 1;
	
	private enum Direction { ASCENDING, DESCENDING };
	
	
	class TableSorter extends ViewerSorter
	{
		private Direction direction;
		private int column;

		public TableSorter()
		{
			column = -1;
			direction = Direction.ASCENDING;
		}

		public void setSorting(TableColumn column, int direction)
		{
			// Find column via title text
			this.column = -1;
			for(int i = 0; i < COLUMN_TITLES.length; i++) {
				if(COLUMN_TITLES[i].equals(column.getText())) {
					this.column = i;
					break;
				}
			}
			
			// Store order
			if(direction == SWT.DOWN) this.direction = Direction.DESCENDING;
			else this.direction = Direction.ASCENDING;
		}

		@Override
		public int compare(Viewer viewer, Object e1, Object e2)
		{
			Marker p1 = (Marker) e1;
			Marker p2 = (Marker) e2;

			int rc = 0;
			switch(column) {
			case -1:		
			case COLUMN_ID_NAME:
				rc = p2.getName().compareTo(p1.getName());
				break;
			case COLUMN_ID_COLOR:
				rc = p2.getColor().getRGB() -p1.getColor().getRGB();
				break;
			default:
				throw new RuntimeException(this +": Invalid column ID for marker view sorting.");
			}
			
			// If descending order, flip the direction
			if(direction == Direction.DESCENDING) {
				rc = -rc;
			}
			
			return rc;
		}
	}
	
	class ViewContentProvider implements IStructuredContentProvider
	{
		public void inputChanged(Viewer v, Object oldInput, Object newInput)
		{
		}

		public void dispose()
		{
		}

		public Object[] getElements(Object parent)
		{
			if(parent instanceof MarkerContainer) {
				return ((MarkerContainer) parent).getMarkers();
			}
			
			return null;
		}
	}

	class ViewLabelExtProvider extends StyledCellLabelProvider
	{
		@Override
		public void update(ViewerCell cell)
		{
			Marker element = (Marker) cell.getElement();
			int index = cell.getColumnIndex();
			String columnText = getColumnText(element, index);
			cell.setText(columnText);
			
			if(index == COLUMN_ID_COLOR) {
				java.awt.Color awtColor = element.getColor();
				Color color = new Color(cell.getControl().getDisplay(), awtColor.getRed(), awtColor.getGreen(), awtColor.getBlue());
				cell.setBackground(color);
			}
		}

		public String getColumnText(Object obj, int index)
		{
			if(obj instanceof Marker) {
				Marker entry = (Marker) obj;
				
				switch(index) {
				case COLUMN_ID_NAME:
					return entry.getName();
				case COLUMN_ID_COLOR:
					return entry.getColor().toString();
				default:
					throw new RuntimeException(this +": Invalid column ID for packet view.");
				}
			}
			
			return null;
		}
	}
	
	public MarkerView()
	{
	}

	/**
	 * Create GUI
	 */
	public void createPartControl(Composite parent)
	{
		GridLayout layout = new GridLayout(1, false);
		parent.setLayout(layout);
		
		Composite tableComp = new Composite(parent, SWT.NONE);
		tableComp.setLayout(new FillLayout());
		tableComp.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		viewer = new TableViewer(tableComp, SWT.FULL_SELECTION | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);

		// important to create the columns before setting the providers
		createColumns();
		
		viewer.setContentProvider(new ViewContentProvider());
		viewer.setLabelProvider(new ViewLabelExtProvider());
		viewer.setInput(MarkerContainer.getInstance());
		
		Composite buttonComp = new Composite(parent, SWT.NONE);
		buttonComp.setLayout(new RowLayout());
		
		Button deleteButton = new Button(buttonComp, SWT.NONE);
		deleteButton.setText("Delete marker");
		deleteButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				ISelection selection = viewer.getSelection();
			
				if(!selection.isEmpty()) {
					if(selection instanceof StructuredSelection) {
						Object obj = ((StructuredSelection) selection).getFirstElement();
						
						if(obj instanceof Marker) {
							Logging.info(this, "Deleting marker: " +obj);
							MarkerContainer.getInstance().removeMarker((Marker) obj);
						}
					}
				}
			}
		});

		Button refreshButton = new Button(buttonComp, SWT.NONE);
		refreshButton.setText("Refresh view");
		refreshButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				updateView();
			}
		});

		// store display
		display = parent.getDisplay();
		
		// get selection events from internal table
		viewer.addSelectionChangedListener(this);

		// announce ourself as selection source
		selectionProvider = new SelectionProvider(display);
		getSite().setSelectionProvider(selectionProvider);
		
		MarkerContainer.getInstance().addObserver(this);
	}
	
	/**
	 * Called by the internal table displaying the packets, if the user
	 * selects one of them. Now, we have to convert the PacketQueueEntry
	 * to a Packet and announce the selection of the Packet object. That
	 * transition is needed for the PropertyView, which is able to display
	 * Packets, only.
	 */
	@Override
	public void selectionChanged(SelectionChangedEvent event)
	{
		if(event != null) {
			ISelection selection = event.getSelection();
			
			if(!selection.isEmpty()) {
				if(selection instanceof StructuredSelection) {
					Object obj = ((StructuredSelection) selection).getFirstElement();
					
					Logging.log(this, "Selected object: " +obj);
					selectionProvider.announceSelection(obj);
				}
			}
		}
	}

	@Override
	public void notify(MarkerContainer pContainer, Object pChangesObject)
	{
		updateView();	
	}
	
	private void updateView()
	{
		if(Thread.currentThread() != display.getThread()) {
			display.asyncExec(new Runnable() {
				@Override
				public void run() {
					updateView();
				}
			});
		} else {
			viewer.refresh(); // TODO: refreshes all, performance?
			//viewer.add(log.getFirst()); // TODO adds at the end, not the best solution
		}
	}
	
	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus()
	{
		viewer.getControl().setFocus();
	}
	
	@Override
	public void dispose()
	{
		MarkerContainer.getInstance().deleteObserver(this);
		
		super.dispose();
	}
	
	private void createColumns()
	{
		for (int i = 0; i < Math.min(COLUMN_TITLES.length, COLUMN_SIZE.length); i++) {
			TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
			column.getColumn().setText(COLUMN_TITLES[i]);
			column.getColumn().setWidth(COLUMN_SIZE[i]);
			column.getColumn().setResizable(true);
			column.getColumn().setMoveable(true);
			
			// Setting the right sorter
			column.getColumn().addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent event) {
					if(event.widget instanceof TableColumn) {
						TableColumn column = (TableColumn) event.widget;
						
						int dir = viewer.getTable().getSortDirection();

						if(viewer.getTable().getSortColumn() == column) {
							dir = (dir == SWT.UP) ? SWT.DOWN : SWT.UP;
						} else {
							dir = SWT.DOWN;
						}
						
						tableSorter.setSorting(column, dir);
						viewer.getTable().setSortColumn(column);
						viewer.getTable().setSortDirection(dir);
						updateView();
					}
				}
			});

		}
		
		Table table = viewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
	}
	
	private TableViewer viewer;
	private TableSorter tableSorter;
	private Display display;
	private SelectionProvider selectionProvider;
}
