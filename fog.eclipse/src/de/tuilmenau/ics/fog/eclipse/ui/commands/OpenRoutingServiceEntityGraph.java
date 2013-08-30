package de.tuilmenau.ics.fog.eclipse.ui.commands;

import org.eclipse.ui.handlers.IHandlerService;

import de.tuilmenau.ics.fog.eclipse.ui.editors.GraphEditor;
import de.tuilmenau.ics.fog.routing.RoutingServiceInstanceRegister;
import de.tuilmenau.ics.fog.topology.AutonomousSystem;
import de.tuilmenau.ics.fog.topology.Simulation;


public class OpenRoutingServiceEntityGraph extends EclipseCommand
{

	@Override
	public void execute(Object object) throws Exception
	{
		SelectionEvent event = null;
		
		if(object instanceof AutonomousSystem) {
			Simulation sim = ((AutonomousSystem) object).getSimulation();
			
			object = RoutingServiceInstanceRegister.getInstance(sim);
			
			event = new SelectionEvent(object, GraphEditor.ID, false);
		}
		
		try {
			IHandlerService handlerService = (IHandlerService) getSite().getService(IHandlerService.class);
			handlerService.executeCommand(CmdOpenEditor.ID, event);
		}
		catch(Exception exception) {
			throw new RuntimeException("Can not perform action " +this, exception);
		}
	}

}
