package de.tuilmenau.ics.fog.eclipse.ui.commands;

import de.tuilmenau.ics.fog.FoGEntity;


public class OpenFoGRoutingService extends OpenRoutingService
{
	@Override
	public void execute(Object object)
	{
		if(object instanceof FoGEntity) {
			object = ((FoGEntity) object).getRoutingService();
		}
		
		super.execute(object);
	}
}
