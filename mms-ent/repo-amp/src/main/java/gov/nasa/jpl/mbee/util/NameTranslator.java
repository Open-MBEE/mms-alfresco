/*******************************************************************************
 * Copyright (c) <2013>, California Institute of Technology ("Caltech").
 * U.S. Government sponsorship acknowledged.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice, this list
 *    of conditions and the following disclaimer in the documentation and/or other materials
 *    provided with the distribution.
 *  - Neither the name of Caltech nor its operating division, the Jet Propulsion Laboratory,
 *    nor the names of its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package gov.nasa.jpl.mbee.util;

//import gov.nasa.jpl.mbee.util.Random;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * This
 *
 */
public class NameTranslator {

  public interface Translates {
    String translate(String name, String fromDomain, String toDomain);
  }

  public static final String identifierPattern = "[^A-Za-z0-9_.]";
  public static final String identifierReplacement = "_";

  public static class TranslateIdentifier implements Translates {
    @Override
    public String translate( String name, String fromDomain, String toDomain ) {
      // Generate a new name.
      String newName =
          name.trim().replaceAll( identifierPattern, identifierReplacement );
      if ( !Character.isLetter( newName.charAt( 0 ) ) ) {
        newName = "g_" + newName;
      }
      return newName;
    }
  }

  public static TranslateIdentifier translateIdentifier =
      new TranslateIdentifier();

  protected long nextId = 0;
  // Map: name -> domain -> id
  protected Map< String, Map< String, Long > > domainsForName =
      new TreeMap< String, Map< String, Long > >();
  // Map: domain -> name -> id
  protected Map< String, Map< String, Long > > namesInDomain =
      new TreeMap< String, Map< String, Long > >();
  // Map: id -> name -> domain
  protected Map< Long, Map< String, String > > namesForId =
      new TreeMap< Long, Map< String, String > >();
  // Map: id -> domain -> name
  protected Map< Long, Map< String, String > > domainsForId =
      new TreeMap< Long, Map< String, String > >();
  // Map: domain1 -> domain2 -> name1 -> name2
  protected Map< String, Map< String, Map< String, String > > > domainTranslations =
      new TreeMap< String, Map< String, Map< String, String > > >();
  // Map: domain1 -> domain2 -> Translates
  protected Map< String, Map< String, Translates > > translatesMap =
      new TreeMap< String, Map< String, Translates > >();

  public NameTranslator() {}

  protected Long putInDomainsForName( String name, String domain, Long id ) {
    return Utils.put( domainsForName, name, domain, id );
  }
  Long putInNamesInDomain( String domain, String name, Long id ) {
    return Utils.put( namesInDomain, domain, name, id );
  }
  String putInNamesForId( Long id, String name, String domain ) {
    return Utils.put( namesForId, id, name, domain );
  }

  String putInDomainsForId( Long id, String domain, String name ) {
    return Utils.put( domainsForId, id, domain, name );
  }

  String putInDomainTranslations( String domain1, String domain2, String name1, String name2 ) {
    return Utils.put( domainTranslations, domain1, domain2, name1, name2 );
  }

  protected Long insert( String domain, String name, Long id ) {
    Long existingId1 = null, existingId2 = null;
    existingId1 = putInDomainsForName( name, domain, id );
    existingId2 = putInNamesInDomain( domain, name, id );
    putInNamesForId( id, name, domain );
    putInDomainsForId( id, domain, name );
    if ( existingId1 == null ) existingId1 = existingId2;
    return existingId1;
  }

  public Long getIdForNameAndDomain( String name, String domain ) {
    return Utils.get( domainsForName, name, domain );
  }

  public String getNameForIdAndDomain( Long id, String domain ) {
    return Utils.get( domainsForId, id, domain );
  }

  public String getDomainForNameAndId( String name, Long id ) {
    return Utils.get( namesForId, id, name );
  }

  public String getTranslation( String name, String fromDomain, String toDomain ) {
    return Utils.get( domainTranslations, fromDomain, toDomain, name );
  }

  public Translates getTranslates( String fromDomain, String toDomain ) {
    return Utils.get( translatesMap, fromDomain, toDomain );
  }

  // Uses an existing or new id to equate these names across their domains.
  // Returns true iff there is no existing and conflicting translation of the
  // names between domains. In other words, it returns true iff neither name
  // already has an id in the others domain tied to a different name.
  public boolean add( String name1, String domain1, String name2, String domain2 ) {
    // If there's already a translation, see if it's consistent.
    String existingName = getTranslation( name1, domain1, domain2 );
    if ( existingName != null ) {
      return existingName.equals( name2 );
    }
    existingName = getTranslation( name2, domain2, domain1 );
    if ( existingName != null ) {
      return existingName.equals( name1 );  // Is this always false?
    }

    // Get any existing id for the names and use it.
    Long id1 = getIdForNameAndDomain( name1, domain1 );
    Long id2 = getIdForNameAndDomain( name2, domain2 );
    // Set one id to the other if null.
    if ( id1 == null ) id1 = id2;
    if ( id1 == null ) id1 = nextId++;
    if ( id2 == null ) id2 = id1;
    // Now that both ids are assigned, they should match!
    if ( !id1.equals( id2 ) ) return false;

    // Add the names with the same id to the translation maps.
    insert( domain1, name1, id1 );
    insert( domain2, name2, id1 );
    putInDomainTranslations( domain1, domain2, name1, name2 );
    putInDomainTranslations( domain2, domain1, name2, name1 );

    return true;
  }

  // Suggest a unique name based on the input name for the domain.
  public String makeNameUnique( String nameToUniquify, String domain ) {
    Map< String, Long > names = namesInDomain.get( domain );
    if ( !Utils.isNullOrEmpty( names ) ) {
      return makeNameUnique( nameToUniquify, names.keySet(), 100 );
    }
    return nameToUniquify;
  }

  // Same as getTranslation() but creates a new translation if none exists.
  public String translate( String name, String fromDomain, String toDomain ) {

    // Return the translation if it already exists.
    String newName = getTranslation( name, fromDomain, toDomain );
    if ( newName != null ) return newName;

    // Get id for name in fromDomain.
    boolean needToInsertName = false;
    Long id = getIdForNameAndDomain( name, fromDomain );
    if ( id == null ) {
      needToInsertName = true;
      id = nextId++;
    }

    // Generate a new name.
    Translates t = getTranslates( fromDomain, toDomain );
    if ( t == null ) {
      t = translateIdentifier;
    }
    newName = t.translate( name, fromDomain, toDomain );

    // Try and ensure that the new name is unique to toDomain.
    newName = makeNameUnique( newName, toDomain );

    // Update the maps for the new translation of name.
    if ( !Utils.isNullOrEmpty( newName ) ) {
      if ( needToInsertName ) {
        insert( fromDomain, name, id );
      }
      insert( toDomain, newName, id );
      putInDomainTranslations( fromDomain, toDomain, name, newName );
      putInDomainTranslations( toDomain, fromDomain, newName, name );
    }

    return newName;
  }

  // Modify an input name to make it unique to the input set of names.
  public static String makeNameUnique( String newName, Set< String > names,
                                       int numTries ) {
    while ( names.contains( newName ) && numTries > 0 ) {
      newName = newName + ( (int)( Random.global.nextDouble() * 10 ) );
      --numTries;
    }
    // Fail if not unique.
    if ( names.contains( newName ) ) {
      try {
        throw new Exception();
      } catch ( Exception e ) {
        System.err.println( "NameTranslator.makeNameUnique() failed to generate a unique id : " + newName );
        e.printStackTrace();
      }
      return null;
    }

    return newName;
  }

  public String substitute( String value, String fromDomain, String toDomain ) {
    Map< String, String > subs = Utils.get( domainTranslations, fromDomain, toDomain );
    if ( subs == null ) return value;
    String newValue = value;
    for ( Map.Entry< String, String > e : subs.entrySet() ) {
      newValue = newValue.replace( e.getKey(), e.getValue() );
    }
    return newValue;
  }

}
