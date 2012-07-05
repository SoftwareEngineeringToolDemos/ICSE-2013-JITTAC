package net.sourceforge.actool.model.da;

import java.awt.image.ComponentSampleModel;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import net.sourceforge.actool.defaults;
import net.sourceforge.actool.db.DBManager;
import net.sourceforge.actool.db.DBManager.IResultSetDelegate;
//import net.sourceforge.actool.jdt.model.JavaXReference;
import net.sourceforge.actool.model.ResourceMap;
import net.sourceforge.actool.model.ResourceMapping;
import net.sourceforge.actool.model.ia.IElement;
import net.sourceforge.actool.model.ia.IXReference;
import net.sourceforge.actool.model.ia.IXReferenceStringFactory;
import net.sourceforge.actool.model.ia.ImplementationChangeDelta;
import net.sourceforge.actool.model.ia.ImplementationChangeListener;
import net.sourceforge.actool.model.ia.ImplementationModel;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.PropertyDescriptor;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;




/**
 * @since 0.1
 */
public class ArchitectureModel extends ArchitectureElement
							   implements ImplementationChangeListener, 
							   			  IResourceChangeListener,
							              PropertyChangeListener {

    public static final String COMPONENTS   = "components";
	public static final String _DELETION  = "_deletion";
	/**
	 * @since 0.1
	 */
	public static final String EMAIL      = "email";
//	public static final String NAME      = "name";
	public static IXReferenceStringFactory xrefStringFactory= null;
    public static final IPropertyDescriptor[] propertyDescriptors = new IPropertyDescriptor[]  {
         new TextPropertyDescriptor(EMAIL, "Email")
    };
    private static Map<String,String> emailAddresses = new HashMap<String, String>();
//    private String email=loadEmail();
//    private static Connection dbConn;  
//	private static Map<String, Component> components = new HashMap<String, Component>();
	private static Map<ArchitectureModel, HashMap<String, Component>> components = new HashMap<ArchitectureModel,HashMap<String, Component>>();
	private  ResourceMap map=null;
	private  LinkedList<Connector> connectorList = new LinkedList<Connector>();
	private final IResource resource;
	private final ModelProperties properties;
	private final String unresolvedTableName;
	private boolean initDb =true;
	public ArchitectureModel(IResource resource) {
		this.resource = resource;
//		email=loadEmail();
		if(map==null)map =  new ResourceMap(new QualifiedName("net.sourceforge.actool.map.", Integer.toString(hashCode())));
		components.put(this, new HashMap<String, Component>());
		this.properties = new ModelProperties(resource);
		unresolvedTableName = "unresolved_"+this.resource.getName().replace(".", "_");
		initDb();
	}
	
	public IResource getResource() {
		return resource;
	}

	public ArchitectureModel getModel() {
		return this;
	}
	
	public  ResourceMap getResourceMap() {
	    return map;
	}
	
	protected void onMappingAdded(final ResourceMapping mapping) {
	    ResourceMapping ancestor = this.map.getAncestorMapping(mapping);
	    
	    // If there is an 'ancestor' mapping which matches x-references
	    // matched by this mapping, all it's x-references must be re-processed.
	    if (ancestor != null) {
	        Iterator<Connector> conns;
	        
	        // We process source connections first.
	        conns = ancestor.getComponent().getSourceConnectors().iterator();
	        while (conns.hasNext()) {
	            final Connector connector = conns.next();
	 
	            // We need to go through all X references...
	            final Iterator<IXReference> xrefs = connector.getXReferences().iterator();
	            LinkedList<Thread> threads = new LinkedList<Thread>();//create list of threads
                while (xrefs.hasNext()) {
                	threads.clear();
                	while(threads.size()<defaults.MAX_THREADS){
                		
    					threads.add(new Thread(new Runnable() {
    						
    						@Override
    						public void run() {
    							IXReference xref=null;
    						
    							synchronized (xrefs) {
    								if(xrefs.hasNext())
    									xref = xrefs.next();
    							}
    							if(xref==null)return;                
    							if (mapping.matches(xref.getSource().getResource())) {              
    			                    connector.removeXReference(xref);
    			                    if (!mapping.getComponent().equals(connector.getTarget()))
    			                    	addXReference(xref, mapping.getComponent(), connector.getTarget());
    			                }
    							
    						}
    					}));
        			}
                	
    				
                	for(Thread t :threads)t.start();
                	for(Thread t :threads)
    					try {
    						t.join();
    					} catch (InterruptedException e) {
    						// TODO Auto-generated catch block
    						e.printStackTrace();
    					}
//	                IXReference xref = xrefs.next();
//	                
//	                if (mapping.matches(xref.getSource().getResource())) {              
//	                    connector.removeXReference(xref);
//	                    if (!mapping.getComponent().equals(connector.getTarget()))
//	                    	addXReference(xref, mapping.getComponent(), connector.getTarget());
//	                }
	            }
	        }
	        
	        // And target connections then.
            conns = ancestor.getComponent().getTargetConnectors().iterator();
            while (conns.hasNext()) {
                final Connector connector = conns.next();
     
                // We need to go through all X references...
                final Iterator<IXReference> xrefs = connector.getXReferences().iterator();
                LinkedList<Thread> threads = new LinkedList<Thread>();//create list of threads
                while (xrefs.hasNext()) {
                	threads.clear();
                	while(threads.size()<defaults.MAX_THREADS){
                		
    					threads.add(new Thread(new Runnable() {
    						
    						@Override
    						public void run() {
    							IXReference xref=null;
    						
    							synchronized (xrefs) {
    								if(xrefs.hasNext())
    									xref = xrefs.next();
    							}
    							if(xref==null)return;               
    		                    if (mapping.matches(xref.getTarget().getResource())) {              
    		                        connector.removeXReference(xref);
    		                        if (!connector.getSource().equals(mapping.getComponent()))
    		                        	addXReference(xref, connector.getSource(), mapping.getComponent());
    		                    }
    							
    						}
    					}));
        			}
                	
    				
                	for(Thread t :threads)t.start();
                	for(Thread t :threads)
    					try {
    						t.join();
    					} catch (InterruptedException e) {
    						// TODO Auto-generated catch block
    						e.printStackTrace();
    					}
//                    IXReference xref = xrefs.next();
//                    
//                    if (mapping.matches(xref.getTarget().getResource())) {              
//                        connector.removeXReference(xref);
//                        if (!connector.getSource().equals(mapping.getComponent()))
//                        	addXReference(xref, connector.getSource(), mapping.getComponent());
//                    }
                }
            }
	    }
	    
	    // Also the un-resolved x-references must be re-processed.
	    reprocessUnresolvedXReferences();
	}
	
	protected void onMappingRemoved(ResourceMapping mapping) {
	    // Re process all x-references potentially previously mapped by the removed mapping.
	    final Component component = mapping.getComponent();
	    Iterator<Connector> conns;
	    
	    // We process source connections first.
        conns = component.getSourceConnectors().iterator();
        while (conns.hasNext()) {
            final Connector connector = conns.next();
           
            // We need to go through all X references...
            final Iterator<IXReference> xrefs = connector.getXReferences().iterator();
            LinkedList<Thread> threads = new LinkedList<Thread>();
            while (xrefs.hasNext()) {
            	threads.clear();
            	while(threads.size()<defaults.MAX_THREADS){
            		
					threads.add(new Thread(new Runnable() {
						
						@Override
						public void run() {
							IXReference xref=null;
						
							synchronized (xrefs) {
								if(xrefs.hasNext())
									xref = xrefs.next();
							}
			                if(xref==null)return;
			                // If the x-reference does not resolve any more 
			                // that means it was matched by the removed mapping and is un-mapped now.
			                Component resolved = map.resolveMapping(xref.getSource().getResource());
			                if (resolved == null || resolved.equals(connector.getTarget())) {
			                    connector.removeXReference(xref);
			                    addUnresolvedXReference(xref);
			                } else if (!resolved.equals(component)) {
			                    connector.removeXReference(xref);
			                    
			                    // Get connection between the components, if none exists, create one.
			                    getConnector(resolved, connector.getTarget(), true).addXReference(xref);
			                }
							
						}
					}));
    			}
            	
				
            	for(Thread t :threads)t.start();
            	for(Thread t :threads)
					try {
						t.join();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
//                IXReference xref = xrefs.next();
//                
//                // If the x-reference does not resolve any more 
//                // that means it was matched by the removed mapping and is un-mapped now.
//                Component resolved = map.resolveMapping(xref.getSource().getResource());
//                if (resolved == null || resolved.equals(connector.getTarget())) {
//                    connector.removeXReference(xref);
//                    addUnresolvedXReference(xref);
//                } else if (!resolved.equals(component)) {
//                    connector.removeXReference(xref);
//                    
//                    // Get connection between the components, if none exists, create one.
//                    getConnector(resolved, connector.getTarget(), true).addXReference(xref);
//                }
            }
            
            if (!connector.isEnvisaged() && !connector.hasXReferences())
            	connector.disconnect();
        }
        
        // And then target connections!
        conns = component.getTargetConnectors().iterator();
        while (conns.hasNext()) {
            final Connector connector = conns.next();
            
            // We need to go through all X references...
            final Iterator<IXReference> xrefs = connector.getXReferences().iterator();
            LinkedList<Thread> threads = new LinkedList<Thread>();
            while (xrefs.hasNext()) {
            	threads.clear();
				while(threads.size()<defaults.MAX_THREADS){
            		
					threads.add(new Thread(new Runnable() {
						
						@Override
						public void run() {
							IXReference xref=null;
						
							synchronized (xrefs) {
								if(xrefs.hasNext())
									xref = xrefs.next();
							}
			                if(xref==null)return;
			                
			                // If the x-reference does not resolve any more 
			                // that means it was matched by the removed mapping and is un-mapped now.
			                Component resolved = map.resolveMapping(xref.getTarget().getResource());
			                if (resolved == null || resolved.equals(connector.getSource())) {
			                    connector.removeXReference(xref);
			                    addUnresolvedXReference(xref);
			                } else if (!resolved.equals(component)) {
			                    connector.removeXReference(xref);
			                    
			                    // Get connection between the components, if none exists, create one.
			                    getConnector(connector.getSource(), resolved, true).addXReference(xref);
			                }
							
						}
					}));
    			}
            	
				
            	for(Thread t :threads)t.start();
            	for(Thread t :threads)
					try {
						t.join();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
            	
            	
//                IXReference xref = xrefs.next();
//                
//                // If the x-reference does not resolve any more 
//                // that means it was matched by the removed mapping and is un-mapped now.
//                Component resolved = map.resolveMapping(xref.getTarget().getResource());
//                if (resolved == null || resolved.equals(connector.getSource())) {
//                    connector.removeXReference(xref);
//                    addUnresolvedXReference(xref);
//                } else if (!resolved.equals(component)) {
//                    connector.removeXReference(xref);
//                    
//                    // Get connection between the components, if none exists, create one.
//                    getConnector(connector.getSource(), resolved, true).addXReference(xref);
//                }
            }
            
            if (!connector.isEnvisaged() && !connector.hasXReferences())
            	connector.disconnect();
        } 
	}

	
	public void attachToImplementation(ImplementationModel implementation) {
		// TODO: Parse model to get existing cross references.
		if (implementation.addImplementationChangeListener(this)){
					reconnectAll();	
		}
	}

	public void detachFromImplementation(ImplementationModel implementation) {
		// TODO: Remove all cross references from this model.
		implementation.removeImplementationChangeListener(this);
	}
	
	public void implementationChangeEvent(ImplementationChangeDelta event) {

		Iterator<IXReference> iter = event.getRemovedXReferences().iterator();
		while (iter.hasNext())
			removeXReference(iter.next());
		
		iter = event.getAddedXReferences().iterator();
		while (iter.hasNext())
			addXReference(iter.next());
		
	}
	
	
	public void addModelListener(IArchitectureModelListener listener) {
		class AddListenerVisitor extends ArchitectureModelVisitor {
			protected IArchitectureModelListener listener;
			
			public AddListenerVisitor(IArchitectureModelListener listener) {
				this.listener = listener;
			}
			
			@Override
			public boolean visit(ArchitectureElement element) {
				element.addPropertyChangeListener(listener);
				return super.visit(element);
			}
		};
		
		accept(new AddListenerVisitor(listener));
	}
	
	public void removeModelListener(IArchitectureModelListener listener) {
		class RemoveListenerVisitor extends ArchitectureModelVisitor {
			protected IArchitectureModelListener listener;
			
			public RemoveListenerVisitor(IArchitectureModelListener listener) {
				this.listener = listener;
			}
			
			@Override
			public boolean visit(ArchitectureElement element) {
				element.removePropertyChangeListener(listener);
				return super.visit(element);
			}
		};
		
		accept(new RemoveListenerVisitor(listener));
	}
	
	   
    public boolean hasComponent(String id) {
        return components.get(this).containsKey(id);
    }

    public boolean hasComponent(Component component) {
        return hasComponent(component.getID()) && getComponent(component.getID()).equals(component);
    }
    
    public String comuteUniqueID() {
	    String id = "";
	    for (int i = 0; i < 999; ++i) {
	        id = hashCode() + "." + Integer.toString(i, 3);
	        if (!hasComponent(id))
	            break;
	    }
	    
	    return id;
    }
    
	public Component createComponent(String id, String name) {
	    if (hasComponent(id))
	        throw new IllegalStateException("Component with given ID already exists!");
		Component component = new Component(id, name);
	
		addComponent(component);
		return component;
	}

	public void addComponent(Component component) {
	    if (component.hasModel())
	        throw new IllegalArgumentException("Component already associated with a model!");
	    components.get(this).put(component.getID(), component);
//	    components.put(component.getID(), component);
	    component.setModel(this);
	    
	    component.addPropertyChangeListener(this);
	    firePropertyChange(COMPONENTS, null, component);
	}
	
	public void removeComponent(Component component) {
	    if (!hasComponent(component))
	        throw new IllegalArgumentException("Component is not a part of this model!");
	    
	    // Remove the mappings first.
	    component.removeAllMappings();

	    // Disconnect all componnent's connections.
	    component.disconnectAllConnectors();

	    // Remove component.
	    component.removePropertyChangeListener(this);
	    component.setModel(null);
	    components.get(this).remove(component.getID());
	    
	    // Re-process all the X references for given component.
	    reprocessXReferences(component);
	    
	    firePropertyChange(COMPONENTS, component, null);
	}
	
	public Component getComponent(String id) {
//		return components.get(id);
		return components.get(this).get(id);
	}
	
	public List<Component> getComponents() {
//		return new LinkedList<Component>(components.values());
		return new LinkedList<Component>(components.get(this).values());
	}
	
	public Connector connect(String sourceId, String targetId) {
	    Component source = getComponent(sourceId);
	    Component target = getComponent(targetId);
	    if ((source == null ) || (target == null) || target.equals(source))
	        throw new IllegalArgumentException();
	    return Connector.connect(source, target, true);
	}
	
	public static Connector getConnector(Component source, Component target, boolean create) {
	   if ((source == null ) || (target == null) || target.equals(source))
	        throw new IllegalArgumentException();

	    Connector connector = source.getConnectorForTarget(target);
	    if (connector == null) {
	        if (create) {
	            connector = Connector.connect(source, target, false);
	            // TODO: A violation has been added, handle this!
	        } else
	            return null;
	    }
	            	            
	    assert connector.getTarget().equals(target);
        return connector;
	}
	
	public Collection<Connector> getConnectors() {
		LinkedList<Connector> connectors = new LinkedList<Connector>();
	    
//	    Iterator<Component> iter = components.values().iterator();
		Iterator<Component> iter = components.get(this).values().iterator();
	    while (iter.hasNext())
	        connectors.addAll(iter.next().getSourceConnectors());
	    
	    return connectors;
	}
	
    
   
    protected void reprocessXReferences(Component component) {
        Iterator<Connector> iter;
        
        // We process source connections first.
        iter = component.getSourceConnectors().iterator();
        while (iter.hasNext()) {
            Connector connector = iter.next();
 
            // We need to go through all X references...
            Iterator<IXReference> xrefs = connector.getXReferences().iterator();
            while (xrefs.hasNext()) {
                IXReference xref = xrefs.next();
                connector.removeXReference(xref);
                addXReference(xref);
            }
           
           
        }
        
        // Then we need to process target connections.
        iter = component.getTargetConnectors().iterator();
        while (iter.hasNext()) {
            Connector connector = iter.next();
 
            // We need to go through all X references...
            Iterator<IXReference> xrefs = connector.getXReferences().iterator();
            while (xrefs.hasNext()) {
                IXReference xref = xrefs.next();
                connector.removeXReference(xref);
                addXReference(xref);
            }
        }
        System.gc();
    }
    
    private void  reconnectAll(){
    	LinkedList<IXReference> result= new LinkedList<IXReference>();
    	LinkedList<String> connectors = new LinkedList<String>();
		try {
			DBManager.preparedQuery("select distinct connector_id from "+Connector.TABLE_NAME/*, dbConn*/, new IResultSetDelegate(){
				@Override
				public int invoke(ResultSet rs, Object... args) throws SQLException {
					if(args.length!=1||!(args[0] instanceof LinkedList<?>)) return -1;
					LinkedList<String> result= (LinkedList<String>) args[0];
					while(rs.next())result.add(rs.getString("connector_id"));
					return 0;
				}

			},connectors);
			for(String connectorID: connectors){
				try {
					DBManager.preparedQuery("select TOP 1 xref from "+Connector.TABLE_NAME+" where connector_id= ? " , new Object[]{connectorID}/*, dbConn*/, new IResultSetDelegate(){
						@Override
						public int invoke(ResultSet rs, Object... args) throws SQLException {
							if(args.length!=1||!(args[0] instanceof LinkedList<?>)) return -1;
							LinkedList<IXReference> result= (LinkedList<IXReference>) args[0];
							while(rs.next())result.add(createXref(rs.getString("xref")));
							return 0;
						}

						private IXReference createXref(String xref){
							return xrefStringFactory.createXReference(xref);
						}
						
					},result);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
//			for(IXReference xref:result){
//				reconnect(xref);
//			}
			final Iterator<IXReference> it = result.iterator();
			LinkedList<Thread> threads = new LinkedList<Thread>();
			while(it.hasNext()){
				threads.clear();
            	while(threads.size()<defaults.MAX_THREADS){
            		
					threads.add(new Thread(new Runnable() {
						
						@Override
						public void run() {
							IXReference xref=null;
							synchronized (it) {
							if(it.hasNext())xref=it.next();
							}
							if(xref!=null)reconnect(xref);
						}
					}));
    			}
            	
				
            	for(Thread t :threads)t.start();
            	for(Thread t :threads)
					try {
						t.join();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	System.gc();
    }
    
    private void  reconnect(IXReference xref){
    	Component source = resolveMapping(xref.getSource());
		Component target = resolveMapping(xref.getTarget());
		if(source==null||target==null||target.equals(source))return;
		Connector conn = getConnector(source, target, true);
		if(!connectorList.contains(conn))connectorList.add(conn);
		
    }
    

    
   
    
	public void addXReference(IXReference xref) {
		
		// Map given java elements to model components.
		Component source = resolveMapping(xref.getSource());
		Component target = resolveMapping(xref.getTarget());
		if (source == null || target == null || source.equals(target)) {

		    	addUnresolvedXReference(xref);
			return;
		}
		
		addXReference(xref, source, target);
	}
	
	protected void addXReference(IXReference xref, Component source, Component target){
	    assert source != null && target != null && !source.equals(target);
	    
	    // Get connection between the components, if none exists, create one.
       Connector conn = getConnector(source, target, true);
       
       if(!conn.containsXref(xref)){// check if connection already exits before adding.
    	   conn.addXReference(xref);
    	   if(!connectorList.contains(conn))connectorList.add(conn);
       }
	}
	
	protected void removeXReference(IXReference xref) {
	    
	    // Removed unmapped x-references;
	    if (hasUnresolveddXReference(xref)) {
	        removeUnresolvedXReference(xref);
	        return;
	    }
	    
	    // Check if we have a mapping for the x-reference and remove it.
	    Connector connector =null;
	    String targetId = Connector.findConnectorId(xref);
	    for(Connector c: connectorList){
	    	if(c.toString().equals(targetId)){ 
	    		connector = c;
	    		break;
	    	}
	    }
		if (connector == null)
			// No connector, nothing to do then. This should not happen though...
			return; // TODO: Handle this!
		
	    // Remove x-reference from the connector
	    // and destroy the connector if it's a violation
		connector.removeXReference(xref);
		if (!connector.isEnvisaged() && connector.getNumXReferences() == 0){
			connectorList.remove(connector);
			connector.disconnect();
		}
	}
	
	protected void addUnresolvedXReference(IXReference xref) {
		if(!containsUndiclaredXref(xref)){
			try {
				DBManager.preparedUpdate("insert into "+unresolvedTableName+" values (?)",new Object[]{xrefStringFactory.toString(xref)} /*,dbConn*/);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	protected boolean hasUnresolveddXReference(IXReference xref) {
		boolean[] result= new boolean[]{ false};
	    	try {
	    		DBManager.preparedQuery("select count(xref)>0 as found from "+unresolvedTableName + " where xref=?",new Object[]{xrefStringFactory.toString(xref)} /*, dbConn*/, new IResultSetDelegate(){
	
					@Override
					public int invoke(ResultSet rs, Object... args) throws SQLException {
						if(args.length!=1) return -1;
						if(rs.next())
						((boolean[])args[0])[0]=rs.getBoolean("found");
						return 0;
					}
					
				},result);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	return result[0]; 
	}
	
	protected void removeUnresolvedXReference(IXReference xref) {
		removeUnresolvedXReference(xrefStringFactory.toString(xref));
	}
	/**
	 * @since 0.1
	 */
	protected void removeUnresolvedXReference(String xref) {
		try {
			DBManager.preparedUpdate("delete from "+unresolvedTableName+"  where xref=?",new Object[]{xref}/*,dbConn*/);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
    protected void reprocessUnresolvedXReferences() {
        Iterator<IXReference> iter = retriveUnresolvedXrefs().iterator();
        
        while (iter.hasNext()) {
            IXReference xref = iter.next();
            while(xref==null&& iter.hasNext())xref = iter.next();
            Component source = resolveMapping(xref.getSource());
            Component target = resolveMapping(xref.getTarget());
            if (source == null || target == null || source.equals(target))
                continue;

            addXReference(xref, source, target);
            removeUnresolvedXReference(xref);
        }
        iter=null;
        System.gc();
    }

	protected  Component resolveMapping(IElement element) {
	    return getResourceMap().resolveMapping(element.getResource());
	}
	
    public void propertyChange(PropertyChangeEvent event) {
        if (event.getSource() instanceof Component) {
            if (event.getPropertyName() == Component.ID) {
                String oldID = (String) event.getOldValue();
                String newID = (String) event.getNewValue();
    
//                Component comp = components.get(oldID);
                Component comp = components.get(this).get(oldID);
                if (comp == null || !comp.equals(event.getSource())) 
                    throw new IllegalStateException();
                components.get(this).remove(oldID);
//                components.put(newID, comp);
                components.get(this).put(newID, comp);
            } else if (event.getPropertyName() == Component.MAPPINGS) {
                if (event.getNewValue() == null && event.getOldValue() != null)
                    onMappingRemoved((ResourceMapping) event.getOldValue());
                if (event.getNewValue() != null && event.getOldValue() == null)
                    onMappingAdded((ResourceMapping) event.getNewValue());    
            }
        }
    }
    
    public void accept(IArchitectureModelVisitor visitor) {
    	if (!visitor.visit(this))
    		return;
    	
    	// Visit all the components...
//    	for (Component component: components.values())
    	for (Component component: components.get(this).values())
    		component.accept(visitor);
    }

    public IPropertyDescriptor[] getPropertyDescriptors() {
        return propertyDescriptors;
    }

    @Override
    public Object getPropertyValue(Object id) {
    	if (id.equals(EMAIL))  return getEmail();
        return null;
    }

    @Override
    public boolean isPropertySet(Object id) {
    	if (id.equals(EMAIL))  return getEmail() != null && !getEmail().isEmpty();
        return false;
    }

    @Override
    public void resetPropertyValue(Object id) {
//    	if (id.equals(EMAIL))  setEmail(loadEmail());
        
    }

    private String loadEmail() {
		// TODO Auto-generated method stub
		return "Architect@company.com";
	}
    /**
	 * @since 0.1
	 */
    public String getEmail(){
      	return getEmail(this.resource.getFullPath());
    }
    
    /**
	 * @since 0.1
	 */
    public static String getEmail(IPath fullPath){
    	String result = emailAddresses.get(fullPath.toString());
    	if(result==null||result.isEmpty())emailAddresses.put(fullPath.toString(),(result = "Architect@company.com"));
    	return result;
    }

	@Override
    public void setPropertyValue(Object id, Object value) {
        if (id.equals(EMAIL))  setEmail((String)value);
        
    }

	/**
	 * @since 0.1
	 */
	public void setEmail(String value) {
		emailAddresses.put(this.resource.getFullPath().toString(),value);
		
	}

	public void resourceChanged(IResourceChangeEvent event) {
		class Visitor implements IResourceDeltaVisitor {
			public boolean removed = false;

			public boolean visit(IResourceDelta delta) throws CoreException {
				if (delta.getKind() == IResourceDelta.REMOVED)
					removed = true;
				return true;
			}
			
		}
		
		// Analyse the resource delta!
		Visitor visitor = new Visitor();
		try {
			event.getDelta().accept(visitor);
		} catch (CoreException e) {
			e.printStackTrace();
		}
		
		// If some resources have been removed we may need to refresh the mappings.
		if (visitor.removed) {
			firePropertyChange(_DELETION, null, null);
		}
	}
	
	private Collection<IXReference> retriveUnresolvedXrefs() {
		LinkedList<IXReference> result= new LinkedList<IXReference>();
		try {
			DBManager.preparedQuery("select distinct xref from "+unresolvedTableName /*, dbConn*/, new IResultSetDelegate(){
				@Override
				public int invoke(ResultSet rs, Object... args) throws SQLException {
					if(args.length!=1||!(args[0] instanceof LinkedList<?>)) return -1;
					LinkedList<IXReference> result= (LinkedList<IXReference>) args[0];
					while(rs.next()){
						String current = rs.getString("xref");
						if(current.contains("\t"))
						result.add(createXref(current));
						else removeUnresolvedXReference(current);
					}
					return 0;
				}
				
				private IXReference createXref(String xref){
					return xrefStringFactory.createXReference(xref);
				}
				
			},result);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}
	
	

	public boolean containsUndiclaredXref(IXReference xref)
    { 
    	boolean[] result= new boolean[]{ false};
	    	try {
	    		DBManager.preparedQuery("select count(xref)>0 as found from "+unresolvedTableName +" where xref = ? " , new Object[]{"'"+xrefStringFactory.toString(xref)+"'"}/*, dbConn*/, new IResultSetDelegate(){
					@Override
					public int invoke(ResultSet rs, Object... args) throws SQLException {
						if(args.length!=1) return -1;
						if(rs.next())
						((boolean[])args[0])[0]=rs.getBoolean("found");
						return 0;
					}
					
				},result);
			} catch (SQLException e) {
				// TODO Auto-generated catch block 
				e.printStackTrace();
			}
    	
    	return result[0]; 
    }
	
	private void initDb()  {
    	if(!this.initDb)return; 
		try {
//			dbConn = DBManager.connect();
			DBManager.preparedUpdate("CREATE TABLE if not exists "+unresolvedTableName+" (xref VARCHAR(1024) NOT NULL)"/*,  dbConn*/);
			this.initDb =false;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	 /**
	 * @since 0.1
	 */
	public  static Component getComponentByIJavaElement(IJavaElement element){
	    	return getComponentByFQN(getFullname(element));
	    }

		private static String getFullname(IJavaElement element) {
//			element.getResource()
			Stack<String> temp = new Stack<String>();
	    	IJavaElement packageElement = element;
	    	while(packageElement.getElementType() !=4 ) {
	    		if(packageElement.getElementType()!=5){ 
	    			temp.push(packageElement.getElementName());
	    			temp.push(".");
	    		}
	    		packageElement=packageElement.getParent();
	    	}
	    	temp.push(packageElement.getElementName());
	    	String fullname = "";
	    	for(int i=0;i<temp.size();i++) fullname+=temp.pop();
			return fullname;
		}
	/**
	 * @since 0.1
	 */
	public static Component getComponentByFQN(String fullname) {
		Component result=null;
    	String bestMatch ="";
    	Iterator<ArchitectureModel> it= components.keySet().iterator();
    	LinkedList<Component> comps= new LinkedList<Component>();
    	while(it.hasNext())comps.addAll(components.get(it.next()).values());
    	for(Component c : comps){
//    	for(Component c : components.values()){
    		for(ResourceMapping rm : c.getMappings()){
	    		String current = rm.getName();
	    		
	    		if(fullname.contains(current)){ 
	    			if(isBetterMatch(fullname, bestMatch, current)) {
	    				bestMatch=current;
	    				if(result==null||!result.getName().contains(bestMatch)) result=c;
	    			}
	    		}
    		}
    	}
    	    	
    	return result;
	}

	private static boolean isBetterMatch(String fullname, String bestMatch,String current) {
		 boolean result = true;
		 int cLength = current.length();
		result&=bestMatch.length()<cLength;
		 int fLlength = fullname.length();
		result&=cLength<=fLlength;
		int cLastSegIndex = current.lastIndexOf(".");
		int fLastSegIndex = fullname.lastIndexOf(".");
		if(cLastSegIndex==fLastSegIndex){
			String cLastSeg = current.substring(cLastSegIndex,cLength);
			String fLastSeg = fullname.substring(fLastSegIndex,fLlength);
			result&=cLastSeg.equals(fLastSeg);
		}
		return result;
	}
}
