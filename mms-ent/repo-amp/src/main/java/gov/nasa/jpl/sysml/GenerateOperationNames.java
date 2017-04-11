/*******************************************************************************
 * Copyright (c) <2014>, California Institute of Technology ("Caltech").
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

package gov.nasa.jpl.sysml;

import gov.nasa.jpl.mbee.util.Debug;
import gov.nasa.jpl.mbee.util.MoreToString;

import java.util.TreeSet;

import gov.nasa.jpl.sysml.SystemModel.ModelItem;

/**
 * Generate possible method names from {@link SystemModel.Operation} and {@link SystemModel.ModelItem}.
 */
public class GenerateOperationNames {

    public static TreeSet<String> allLegalOperations() {
        //SystemModel.ModelItem itemType;
        //SystemModel.Operation operation;
        //AbstractSystemModel asm = new AbstractSystemModel< O, C, T, P, N, I, U, R, V, W, CT >();
        TreeSet<String> set = new TreeSet<String>();
        for ( SystemModel.Operation operation : SystemModel.Operation.values() ) {
            for ( boolean nullItem : new boolean[] { false, true } )
            for ( SystemModel.ModelItem itemType : SystemModel.ModelItem.values() ) {
                for ( boolean nullContext : new boolean[] { false, true } )
                for ( SystemModel.ModelItem contextType : SystemModel.ModelItem.values() ) {
                    for ( boolean nullSpec : new boolean[] { false, true } )
                    for ( SystemModel.ModelItem specifierType : SystemModel.ModelItem.values() ) {
                        if ( nullItem && nullContext && nullSpec ) {
                            Debug.breakpoint();
                        }
                        Object newValue = new Integer(4);
                        if ( !AbstractSystemModel.isAllowed((AbstractSystemModel)null, operation,
                                        nullItem ? null : itemType,
                                        nullContext ? null : contextType, //new SystemModel.Item(null, contextType),
                                        nullSpec ? null : specifierType, //new SystemModel.Item(null, specifierType),
                                        ModelItem.VALUE, //newValue,
                                        false) ) {
                            continue;
                        }
                        String name =
                                AbstractSystemModel.getMethodName( operation,
                                                                   nullItem ? null : itemType,
                                                                   nullContext ? null : new SystemModel.Item(null, contextType),
                                                                   nullSpec ? null : new SystemModel.Item(null, specifierType),
                                                                   newValue,
                                                                   false );
                        if ( name != null && !set.contains( name ) ) {
                            set.add( name );
                        }
                        if ( nullSpec ) break;
                    }
                    if ( nullContext ) break;
                }
                if ( nullItem ) break;
            }
        }
        return set;
    }

    public static TreeSet<String> minimalNecessaryAPI() {
        Integer four = new Integer(4);
        TreeSet<String> set = new TreeSet<String>();
        for ( SystemModel.Operation operation : SystemModel.Operation.values() ) {
            for ( boolean nullItem : new boolean[] { false, true } )
            for ( SystemModel.ModelItem itemType : SystemModel.ModelItem.values() ) {
                for ( boolean nullContext : new boolean[] { false, true } )
                for ( SystemModel.ModelItem contextType : SystemModel.ModelItem.values() ) {
                    for ( boolean nullSpec : new boolean[] { false, true } )
                    for ( SystemModel.ModelItem specifierType : SystemModel.ModelItem.values() ) {
                        for ( boolean nullValue : new boolean[] { false, true } ) {
                            if ( nullItem && nullContext && nullSpec ) {
                                Debug.breakpoint();
                            }
                            Object newValue = nullValue ? null : four;
                            if ( !AbstractSystemModel.isNecessaryInAPI(
                                            operation,
                                            nullItem ? null : itemType,
                                            nullContext ? null : contextType, //new SystemModel.Item(null, contextType),
                                            nullSpec ? null : specifierType, //new SystemModel.Item(null, specifierType),
                                            nullValue ? null : ModelItem.VALUE, //newValue,
                                            false) ) {
                                continue;
                            }
                            String name =
                                    AbstractSystemModel.getMethodName( operation,
                                                                       nullItem ? null : itemType,
                                                                       nullContext ? null : new SystemModel.Item(null, contextType),
                                                                       nullSpec ? null : new SystemModel.Item(null, specifierType),
                                                                       newValue, false );
                            if ( name != null && !set.contains( name ) ) {
                                set.add( name );
                            }
                        }
                        if ( nullSpec ) break;
                    }
                    if ( nullContext ) break;
                }
                if ( nullItem ) break;
            }
        }
        return set;
    }

    /**
     * @param args
     */
    public static void main( String[] args ) {
        //Debug.turnOn();
        TreeSet<String> minSet = minimalNecessaryAPI();
        System.out.println( "minimal API:\n" +
                            MoreToString.Helper.toString( minSet, false, false,
                                                          null, null, "", "\n",
                                                          "", false ) );
        TreeSet<String> allowedSet = allLegalOperations();
        System.out.println( "\n\n\nlegal operations\n" +
                            MoreToString.Helper.toString( allowedSet, false, false,
                                                          null, null, "", "\n",
                                                          "", false ) );
    }
}
