package de.tuilmenau.ics.fog.eclipse.ui.commands;

import org.eclipse.ui.IWorkbenchPartSite;

import de.tuilmenau.ics.fog.FoGEntity;


public class OpenFoGRoutingService extends OpenRoutingService
{
	@Override
	public void init(IWorkbenchPartSite site, Object object)
	{
		if(object instanceof FoGEntity) {
			object = ((FoGEntity) object).getRoutingService();
		}
		
		super.init(site, object);
	}
}
