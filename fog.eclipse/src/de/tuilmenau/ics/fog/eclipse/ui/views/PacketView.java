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
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;

import de.tuilmenau.ics.fog.eclipse.ui.editors.SelectionProvider;
import de.tuilmenau.ics.fog.packets.Packet;
import de.tuilmenau.ics.fog.topology.Simulation;
import de.tuilmenau.ics.fog.ui.IPacketObserver;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.fog.ui.PacketLogger;
import de.tuilmenau.ics.fog.ui.PacketQueue.PacketQueueEntry;


/**
 * Class for showing a table with all packets received by a PacketQueue.
 * Packets can be selected by selecting a packet number.
 * 
 * Bases on the article:
 * http://www.vogella.de/articles/EclipseJFaceTable/article.html
 */
public class PacketView extends ViewPart implements IPacketObserver, ISelectionChangedListener, ISelectionListener
{
	public static final String ID = "de.tuilmenau.ics.fog.packetView";

	private static final String[] COLUMN_TITLES = { "Number", "Packet", "Object", "Time" };
	private static final int[]    COLUMN_SIZE = { 40, 200, 200, 100 };
	private static final int COLUMN_ID_NUMBER = 0;
	private static final int COLUMN_ID_PACKET = 1;
	private static final int COLUMN_ID_OBJECT = 2;
	private static final int COLUMN_ID_TIME   = 3;
	
	private enum Direction { ASCENDING, DESCENDING };
	
	// Refresh based on events from logger or by time?
	private static final boolean PACKET_VIEW_REFRESH_TIME_BASED = true;
	private static final int REFRESH_INTERVAL_IN_TIME_MODE_MSEC = 500;
	
	private static final String NO_SELECTED_LOGGER = "None";
	
	
	public class TableFilter extends ViewerFilter
	{
		public void setFilterText(String filter)
		{
			// Search must be a substring of the existing value
			this.filterString = ".*" + filter + ".*";
		}

		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element)
		{
			if(filterString == null) return true;
			if(filterString.length() <= 0) return true;
			
			if(element instanceof PacketQueueEntry) {
				PacketQueueEntry entry = (PacketQueueEntry) element;
				
				if(entry.packet.toString().matches(filterString)) return true;
				if(entry.object.toString().matches(filterString)) return true;
			} else {
				if(element.toString().matches(filterString)) return true;
			}

			return false;
		}
		
		private String filterString;
	}
	
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
			PacketQueueEntry p1 = (PacketQueueEntry) e1;
			PacketQueueEntry p2 = (PacketQueueEntry) e2;

			int rc = 0;
			switch(column) {
			case -1:
				rc = p2.number -p1.number;
				break;
			case COLUMN_ID_NUMBER:
				rc = (int) (p2.packet.getId() -p1.packet.getId());
				break;
			case COLUMN_ID_PACKET:
				rc = p1.packet.toString().compareTo(p2.packet.toString());
				break;
			case COLUMN_ID_OBJECT:
				rc = p1.object.toString().compareTo(p2.object.toString());
				break;
			case COLUMN_ID_TIME:
				rc = (int) (p2.lastSendTime -p1.lastSendTime);
				break;
			default:
				throw new RuntimeException(this +": Invalid column ID for packet view sorting.");
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
			if(parent == logger) {
				return logger.toArray();
			}
			
			return null;
		}
	}

	class ViewLabelProvider extends LabelProvider implements ITableLabelProvider
	{
		@Override
		public String getColumnText(Object obj, int index)
		{
			if(obj instanceof PacketQueueEntry) {
				PacketQueueEntry entry = (PacketQueueEntry) obj;
				
				switch(index) {
				case COLUMN_ID_NUMBER:
					return Long.toString(entry.packet.getId());
				case COLUMN_ID_PACKET:
					return entry.packet.toString();
				case COLUMN_ID_OBJECT:
					return entry.object.toString();
				case COLUMN_ID_TIME:
					return Double.toString(entry.lastSendTime);
				default:
					throw new RuntimeException(this +": Invalid column ID for packet view.");
				}
			}
			
			return null;
		}

		@Override
		public Image getColumnImage(Object obj, int index)
		{
			// no images at all
			return null;
		}
	}
	
	public PacketView()
	{
	}
	
	private GridData createLayoutFill(int colSpan)
	{
		GridData gd = new GridData();
		gd.horizontalAlignment = SWT.FILL;
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalSpan = colSpan;
		return gd;
	}

	/**
	 * Create GUI
	 */
	public void createPartControl(Composite parent)
	{
		parent.setLayout(new GridLayout(1, false));
		
		Composite textElements = new Composite(parent, SWT.FILL);
		GridLayout textElementsLayout = new GridLayout(3, false);
		textElements.setLayout(textElementsLayout);
		textElements.setLayoutData(createLayoutFill(1));
		
		Label selectedLogger = new Label(textElements, SWT.NONE);
		selectedLogger.setText("Selected logger: ");
		
		currentSelectedLoggerKey = new Label(textElements, SWT.NONE);
		currentSelectedLoggerKey.setText(NO_SELECTED_LOGGER);
		currentSelectedLoggerKey.setLayoutData(createLayoutFill(1));
		
		lockSelectedLogger = new Button(textElements, SWT.TOGGLE);
		lockSelectedLogger.setText("Lock selection");
		
		Label searchLabel = new Label(textElements, SWT.NONE);
		searchLabel.setText("Find: ");
		
		searchText = new Text(textElements, SWT.BORDER);
		searchText.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent keyEvent) {
				tableFilter.setFilterText(searchText.getText());
				updateView();
			}
		});
		searchText.setLayoutData(createLayoutFill(2));

		Composite tableComp = new Composite(parent, SWT.NONE);
		tableComp.setLayout(new FillLayout());
		tableComp.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		// store display
		display = parent.getDisplay();
		
		viewer = new TableViewer(tableComp, SWT.FULL_SELECTION | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);

		// important to create the columns before setting the providers
		createColumns();
		
		// setting additional sorter and filter
		tableSorter = new TableSorter();
		viewer.setSorter(tableSorter);
		tableFilter = new TableFilter();
		viewer.setFilters(new ViewerFilter[] { tableFilter });

		// TODO try it without content provider -> see updateView
		viewer.setContentProvider(new ViewContentProvider());
		viewer.setLabelProvider(new ViewLabelProvider());

		// get selection events from internal table
		viewer.addSelectionChangedListener(this);

		// announce ourself as selection source
		selectionProvider = new SelectionProvider(display);
		getSite().setSelectionProvider(selectionProvider);
		
		// register for selection from others
		getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(this);
		 
		// register at logger to start update; do that after view is setup!
		if(PACKET_VIEW_REFRESH_TIME_BASED) {
			updateViewRunnable.schedule();
		} else {
			logger.addObserver(this);
		}
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
					
					if(obj instanceof PacketQueueEntry) {
						Packet packet = ((PacketQueueEntry) obj).packet;
						
						selectionProvider.announceSelection(packet);
					}
				}
			}
		}
	}
	
	/**
	 * Called by the workbench if any other view/editor is reporting a selection.
	 * 
	 * @param sourcepart Part, which is announcing the selection
	 * @param selection Selected element(s)
	 */
	public void selectionChanged(IWorkbenchPart sourcepart, ISelection selection)
	{
		if(sourcepart != this) {
			if(selection instanceof IStructuredSelection) {
				// is the selection not locked?
				if(!lockSelectedLogger.getSelection()) {
					PacketLogger newLogger = null;
					IStructuredSelection strSelection = (IStructuredSelection) selection;
					
					// search for first available logger for selected elements
					for(Object selectedElement : strSelection.toList()) {
						newLogger = PacketLogger.getLogger(selectedElement);
						if(newLogger != null) {
							currentSelectedLoggerKey.setText(selectedElement.toString());
							break;
						} else {
							// TODO fully create packet logger hierarchy
							//      currently it ends at node
							// should we get global logger?
							if(selectedElement instanceof Simulation) {
								newLogger = PacketLogger.getLogger(null);
								if(newLogger != null) {
									currentSelectedLoggerKey.setText(selectedElement.toString());
									break;
								}
							}
						}
					}
				
					if(newLogger == null) {
						currentSelectedLoggerKey.setText(NO_SELECTED_LOGGER);
					}
					setLogger(newLogger);
				}
			}
		}
		// else: it is our stuff; ignore it
	}
	
	private void setLogger(PacketLogger newLogger)
	{
		if(newLogger != logger) {
			// do we have to disconnect from old one?
			if(logger != null) {
				logger.deleteObserver(this);
			}
			
			// connect to new one
			logger = newLogger;
			
			viewer.setInput(logger);
			if(PACKET_VIEW_REFRESH_TIME_BASED) {
				updateViewRunnable.schedule();
			} else {
				if(logger != null) {
					logger.addObserver(this);
				}
			}
		}
	}

	@Override
	public void notify(PacketLogger logger, EventType event, PacketQueueEntry packet)
	{
		if((logger != null) && (packet != null)) {
//			log.addFirst(new LogEntry(log.size() +1, logger.getKey(), packet));
			
			updateView();
		}
	}
	
	/**
	 * Internal class for running the update of a view. In special
	 * it is needed for tracking if an update is currently going on
	 * and for getting the right display/thread to call update for.
	 */
	private class UpdateViewRunnable implements Runnable
	{
		public void schedule()
		{
			if(!display.isDisposed()) {
				display.timerExec(REFRESH_INTERVAL_IN_TIME_MODE_MSEC, this);
			}
		}
		
		@Override
		public void run()
		{
			if(!display.isDisposed()) {
				if(Thread.currentThread() != display.getThread()) {
					//switches to different thread
					display.asyncExec(this);
				} else {
					if(!isRunning()) {
						viewer.refresh(); // TODO baut alles neu auf; performance?
					//	viewer.add(log.getFirst()); // TODO fügt am ende an, was nicht so schön ist
					}
					
					if(PACKET_VIEW_REFRESH_TIME_BASED) {
						schedule();
					}
				}
			}
		}
		
		public synchronized boolean isRunning()
		{
			return viewer.isBusy() || viewer.getTable().isDisposed();
		}
	};
	
	private void updateView()
	{
		// Do not do test and set because the worst thing, which might happening
		// are two view updates at the same time.
		// In general this check reduces the amount of repaints caused by changes
		// in the packet logger.
		if(!updateViewRunnable.isRunning()) {
			updateViewRunnable.run();
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
		setLogger(null);
		
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
	
	private Label currentSelectedLoggerKey;
	private Button lockSelectedLogger;
	private Text searchText;
	private PacketLogger logger;
	private TableViewer viewer;
	private UpdateViewRunnable updateViewRunnable = new UpdateViewRunnable();	
	private TableSorter tableSorter;
	private TableFilter tableFilter;
	private Display display;
	private SelectionProvider selectionProvider;
}
