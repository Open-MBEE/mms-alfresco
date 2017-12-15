package gov.nasa.jpl.sysml.impl;

import gov.nasa.jpl.mbee.util.HasId;
import gov.nasa.jpl.mbee.util.Pair;
import gov.nasa.jpl.mbee.util.Utils;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import gov.nasa.jpl.sysml.AbstractSystemModel;
import gov.nasa.jpl.sysml.Element;
import gov.nasa.jpl.sysml.BaseElement;
import gov.nasa.jpl.sysml.Version;
import gov.nasa.jpl.sysml.Workspace;

public class SystemModelImpl
        extends AbstractSystemModel< ElementImpl, BaseElementImpl, ElementImpl, PropertyImpl, String, String, ElementImpl, ElementImpl, VersionImpl, WorkspaceImpl, ElementImpl > {

    //protected static SystemModelImpl instance = new SystemModelImpl();

    Map< String, WorkspaceImpl > workspaces =
            new LinkedHashMap< String, WorkspaceImpl >();

    Map< String, ElementImpl > elements =
            new LinkedHashMap< String, ElementImpl >();

//    public SystemModelImpl getInstance() {
//        return instance;
//    }

    //Map< I, Element > elements;

    public SystemModelImpl() {
        super();
    }

    @Override
    public boolean isDirected( ElementImpl relationship ) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Collection< ElementImpl >
            getRelatedElements( ElementImpl relationship ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl >
            getElementForRole( ElementImpl relationship, String role ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl > getSource( ElementImpl relationship ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl > getTarget( ElementImpl relationship ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Class< ElementImpl > getElementClass() {
        return ElementImpl.class;
    }

    @Override
    public Class< BaseElementImpl > getContextClass() {
        return BaseElementImpl.class;
    }

    @Override
    public Class< ElementImpl > getTypeClass() {
        return ElementImpl.class;
    }

    @Override
    public Class< PropertyImpl > getPropertyClass() {
        return PropertyImpl.class;
    }

    @Override
    public Class< String > getNameClass() {
        return String.class;
    }

    @Override
    public Class< String > getIdentifierClass() {
        return String.class;
    }

    @Override
    public Class< ElementImpl > getValueClass() {
        return ElementImpl.class;
    }

    @Override
    public Class< ElementImpl > getRelationshipClass() {
        return ElementImpl.class;
    }

    @Override
    public Class< VersionImpl > getVersionClass() {
        return VersionImpl.class;
    }

    @Override
    public Class< WorkspaceImpl > getWorkspaceClass() {
        return WorkspaceImpl.class;
    }

    @Override
    public Class< ElementImpl > getConstraintClass() {
       return ElementImpl.class;
    }

    @Override
    public Class< ? extends ElementImpl > getViewClass() {
        return ElementImpl.class;
    }

    @Override
    public Class< ? extends ElementImpl > getViewpointClass() {
        return ElementImpl.class;
    }

    @Override
    public ElementImpl createConstraint( BaseElementImpl context ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ElementImpl createElement( BaseElementImpl context ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String createIdentifier( BaseElementImpl context ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String createName( BaseElementImpl context ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PropertyImpl createProperty( BaseElementImpl context ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ElementImpl createRelationship( BaseElementImpl context ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ElementImpl createType( BaseElementImpl context ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ElementImpl createValue( BaseElementImpl context ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public VersionImpl createVersion( BaseElementImpl context ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ElementImpl createView( BaseElementImpl context ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ElementImpl createViewpoint( BaseElementImpl context ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public WorkspaceImpl createWorkspace( BaseElementImpl context ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object delete( Object object ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl > getConstraint( BaseElementImpl context,
                                                    Object specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl >
            getConstraintWithElement( BaseElementImpl context, ElementImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl >
            getConstraintWithIdentifier( BaseElementImpl context, String specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl > getConstraintWithName( BaseElementImpl context,
                                                            String specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl >
            getConstraintWithProperty( BaseElementImpl context, PropertyImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public
            Collection< ElementImpl >
            getConstraintWithRelationship( BaseElementImpl context, ElementImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl >
            getConstraintWithType( BaseElementImpl context, ElementImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl >
            getConstraintWithValue( BaseElementImpl context, ElementImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl >
            getConstraintWithVersion( BaseElementImpl context, VersionImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl >
            getConstraintWithView( BaseElementImpl context, ElementImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl >
            getConstraintWithViewpoint( BaseElementImpl context, ElementImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public
            Collection< ElementImpl >
            getConstraintWithWorkspace( BaseElementImpl context, WorkspaceImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl >
            getElementWithConstraint( BaseElementImpl context, ElementImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl >
            getElementWithIdentifier( BaseElementImpl context, String specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl > getElementWithName( BaseElementImpl context,
                                                         String specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl > getElementWithQualifiedName( BaseElementImpl context,
                                                         String specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl >
            getElementWithProperty( BaseElementImpl context, PropertyImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl >
            getElementWithRelationship( BaseElementImpl context, ElementImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl > getElementWithType( BaseElementImpl context,
                                                         ElementImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl >
            getElementWithValue( BaseElementImpl context, ElementImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl >
            getElementWithVersion( BaseElementImpl context, VersionImpl version ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl > getElementWithView( BaseElementImpl context,
                                                         ElementImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl >
            getElementWithViewpoint( BaseElementImpl context, ElementImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl >
            getElementWithWorkspace( BaseElementImpl context, WorkspaceImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getName( BaseElementImpl context ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getIdentifier( BaseElementImpl context ) {
        if ( context instanceof HasId ) {
            Object id = ( (Element)context ).getId();
            if ( id == null )  return null;
            return id.toString();
        }
        return null;
    }

    @Override
    public Collection< PropertyImpl > getProperty( BaseElementImpl context,
                                                   Object specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< PropertyImpl >
            getPropertyWithConstraint( BaseElementImpl context, ElementImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< PropertyImpl >
            getPropertyWithElement( BaseElementImpl context, ElementImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< PropertyImpl >
            getPropertyWithIdentifier( BaseElementImpl context, String specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< PropertyImpl >
            getPropertyWithRelationship( BaseElementImpl context, ElementImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< PropertyImpl >
            getPropertyWithType( BaseElementImpl context, ElementImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< PropertyImpl >
            getPropertyWithValue( BaseElementImpl context, ElementImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< PropertyImpl >
            getPropertyWithVersion( BaseElementImpl context, VersionImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< PropertyImpl >
            getPropertyWithView( BaseElementImpl context, ElementImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< PropertyImpl >
            getPropertyWithViewpoint( BaseElementImpl context, ElementImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< PropertyImpl >
            getPropertyWithWorkspace( BaseElementImpl context, WorkspaceImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl > getRelationship( BaseElementImpl context,
                                                      Object specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public
            Collection< ElementImpl >
            getRelationshipWithConstraint( BaseElementImpl context, ElementImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl >
            getRelationshipWithElement( BaseElementImpl context, ElementImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl >
            getRelationshipWithIdentifier( BaseElementImpl context, String specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl > getRelationshipWithName( BaseElementImpl context,
                                                              String specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public
            Collection< ElementImpl >
            getRelationshipWithProperty( BaseElementImpl context, PropertyImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl >
            getRelationshipWithType( BaseElementImpl context, ElementImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl >
            getRelationshipWithValue( BaseElementImpl context, ElementImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl >
            getRelationshipWithVersion( BaseElementImpl context, VersionImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl >
            getRelationshipWithView( BaseElementImpl context, ElementImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public
            Collection< ElementImpl >
            getRelationshipWithViewpoint( BaseElementImpl context, ElementImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl >
            getRelationshipWithWorkspace( BaseElementImpl context,
                                          WorkspaceImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl > getType( BaseElementImpl context, Object specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getTypeString( BaseElementImpl context, Object specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl >
            getTypeWithConstraint( BaseElementImpl context, ElementImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl > getTypeWithElement( BaseElementImpl context,
                                                         ElementImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl > getTypeWithIdentifier( BaseElementImpl context,
                                                            String specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl > getTypeWithName( BaseElementImpl context,
                                                      String specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl >
            getTypeWithProperty( BaseElementImpl context, PropertyImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl >
            getTypeWithRelationship( BaseElementImpl context, ElementImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl > getTypeWithValue( BaseElementImpl context,
                                                       ElementImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl > getTypeWithVersion( BaseElementImpl context,
                                                         VersionImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl > getTypeWithView( BaseElementImpl context,
                                                      ElementImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl >
            getTypeWithViewpoint( BaseElementImpl context, ElementImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl >
            getTypeWithWorkspace( BaseElementImpl context, WorkspaceImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl >
            getValue( BaseElementImpl context, Object specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl >
            getValueWithConstraint( BaseElementImpl context, ElementImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl >
            getValueWithElement( BaseElementImpl context, ElementImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl > getValueWithIdentifier( BaseElementImpl context,
                                                             String specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl > getValueWithName( BaseElementImpl context,
                                                       String specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl >
            getValueWithProperty( BaseElementImpl context, PropertyImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl >
            getValueWithRelationship( BaseElementImpl context, ElementImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl > getValueWithType( BaseElementImpl context,
                                                       ElementImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl >
            getValueWithVersion( BaseElementImpl context, VersionImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl > getValueWithView( BaseElementImpl context,
                                                       ElementImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl >
            getValueWithViewpoint( BaseElementImpl context, ElementImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl >
            getValueWithWorkspace( BaseElementImpl context, WorkspaceImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< VersionImpl > getVersion( BaseElementImpl context ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl > getView( BaseElementImpl context, Object specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl > getViewpoint( BaseElementImpl context,
                                                   Object specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl >
            getViewpointWithConstraint( BaseElementImpl context, ElementImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl >
            getViewpointWithElement( BaseElementImpl context, ElementImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl >
            getViewpointWithIdentifier( BaseElementImpl context, String specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl > getViewpointWithName( BaseElementImpl context,
                                                           String specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl >
            getViewpointWithProperty( BaseElementImpl context, PropertyImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public
            Collection< ElementImpl >
            getViewpointWithRelationship( BaseElementImpl context, ElementImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl >
            getViewpointWithType( BaseElementImpl context, ElementImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl >
            getViewpointWithValue( BaseElementImpl context, ElementImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl >
            getViewpointWithVersion( BaseElementImpl context, VersionImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl >
            getViewpointWithView( BaseElementImpl context, ElementImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl >
            getViewpointWithWorkspace( BaseElementImpl context, WorkspaceImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl >
            getViewWithConstraint( BaseElementImpl context, ElementImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl > getViewWithElement( BaseElementImpl context,
                                                         ElementImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl > getViewWithIdentifier( BaseElementImpl context,
                                                            String specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl > getViewWithName( BaseElementImpl context,
                                                      String specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl >
            getViewWithProperty( BaseElementImpl context, PropertyImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl >
            getViewWithRelationship( BaseElementImpl context, ElementImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl > getViewWithType( BaseElementImpl context,
                                                      ElementImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl > getViewWithValue( BaseElementImpl context,
                                                       ElementImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl > getViewWithVersion( BaseElementImpl context,
                                                         VersionImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl >
            getViewWithViewpoint( BaseElementImpl context, ElementImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl >
            getViewWithWorkspace( BaseElementImpl context, WorkspaceImpl specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< WorkspaceImpl > getWorkspace( BaseElementImpl context ) {
        // TODO Auto-generated method stub
        return null;
    }

    // FIXME -- TODO -- This should be defined in the SystemModel interface!
    public WorkspaceImpl getWorkspaceWithId( String id ) {
        return workspaces.get( id );
    }

    @Override
    public Object set( Object object, Object specifier, ElementImpl value ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean fixConstraintViolations( ElementImpl element,
                                            VersionImpl version ) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean idsAreWritable() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean namesAreWritable() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean versionsAreWritable() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public ElementImpl getDomainConstraint( ElementImpl element,
                                            VersionImpl version,
                                            WorkspaceImpl workspace ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addConstraint( ElementImpl constraint, VersionImpl version,
                               WorkspaceImpl workspace ) {
        // TODO Auto-generated method stub

    }

    @Override
    public void addDomainConstraint( ElementImpl constraint,
                                     VersionImpl version,
                                     Set< ElementImpl > valueDomainSet,
                                     WorkspaceImpl workspace ) {
        // TODO Auto-generated method stub

    }

    @Override
    public
            void
            addDomainConstraint( ElementImpl constraint,
                                 VersionImpl version,
                                 Pair< ElementImpl, ElementImpl > valueDomainRange,
                                 WorkspaceImpl workspace ) {
        // TODO Auto-generated method stub

    }

    @Override
    public void relaxDomain( ElementImpl constraint, VersionImpl version,
                             Set< ElementImpl > valueDomainSet,
                             WorkspaceImpl workspace ) {
        // TODO Auto-generated method stub

    }

    @Override
    public void relaxDomain( ElementImpl constraint, VersionImpl version,
                             Pair< ElementImpl, ElementImpl > valueDomainRange,
                             WorkspaceImpl workspace ) {
        // TODO Auto-generated method stub

    }

    @Override
    public Collection< ElementImpl >
            getConstraintsOfElement( ElementImpl element, VersionImpl version,
                                     WorkspaceImpl workspace ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< ElementImpl >
            getViolatedConstraintsOfElement( ElementImpl element,
                                             VersionImpl version ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setOptimizationFunction( Method method, Object... arguments ) {
        // TODO Auto-generated method stub

    }

    @Override
    public Number getScore() {
        // TODO Auto-generated method stub
        return null;
    }

    public static LiteralString STRING = new LiteralString();
    public static class LiteralString extends ElementImpl {
        public LiteralString() {
            super((String)null);
        }
    }
    public static class Block extends ElementImpl {

        public Block( String id ) {
            super( id );
        }
        public Block( Workspace< String, String, Date > workspace,
                      String id,
                      String name,
                      Version< String, Date, BaseElement< String, String, Date >> version,
                      Map< String, PropertyImpl > properties ) {
            super( workspace, id, name, version, properties );
            // TODO Auto-generated constructor stub
        }

    }

    public static void main( String[] args ) {
        System.out.println("Hello, world.");
        WorkspaceImpl workspace = new WorkspaceImpl( "ws1" );
        VersionImpl version = new VersionImpl( "1.0", new Date(), null );
        Map< String, PropertyImpl > props = new LinkedHashMap< String, PropertyImpl >();
        ElementImpl literalString = new ElementImpl( workspace, "LiteralString", "LiteralString", null, null );
        props.put( "foo", new PropertyImpl( workspace, "foo", "foo", null, literalString , "bar" ) );
        ElementImpl e = new ElementImpl( workspace , "e_001", "e1", version , props  );


        Block cls1 = new Block("myClass");
        cls1.addProperty("foo", STRING, "bar");

        Block cls2 =
          new Block("myClass");
          cls2.addProperty("foo", STRING, "bar");


    }

}
