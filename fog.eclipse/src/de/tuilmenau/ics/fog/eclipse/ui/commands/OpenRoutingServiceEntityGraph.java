package de.tuilmenau.ics.fog.eclipse.ui.commands;

import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.handlers.IHandlerService;

import de.tuilmenau.ics.fog.eclipse.ui.editors.GraphEditor;
import de.tuilmenau.ics.fog.routing.RoutingServiceInstanceRegister;
import de.tuilmenau.ics.fog.topology.AutonomousSystem;
import de.tuilmenau.ics.fog.topology.Simulation;


public class OpenRoutingServiceEntityGraph extends Command
{
	@Override
	public void init(IWorkbenchPartSite site, Object object)
	{
		this.site = site;
		
		if(object instanceof AutonomousSystem) {
			Simulation sim = ((AutonomousSystem) object).getSimulation();
			
			object = RoutingServiceInstanceRegister.getInstance(sim);
			
			event = new SelectionEvent(object, GraphEditor.ID, false);
		}		
	}

	@Override
	public void main() throws Exception
	{
		try {
			IHandlerService handlerService = (IHandlerService) site.getService(IHandlerService.class);
			handlerService.executeCommand(CmdOpenEditor.ID, event);
		}
		catch(Exception exception) {
			throw new RuntimeException("Can not perform action " +this, exception);
		}
	}
	
	private IWorkbenchPartSite site;
	private SelectionEvent event;
}
