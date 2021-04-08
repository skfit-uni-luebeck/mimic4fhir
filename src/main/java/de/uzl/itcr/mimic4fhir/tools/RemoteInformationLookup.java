/*******************************************************************************
 * Copyright (C) 2021 S. Ververs, P. Behrend, J. Wiedekopf, H.Ulrich - University of Lübeck
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package de.uzl.itcr.mimic4fhir.tools;

import de.uzl.itcr.mimic4fhir.work.Config;

/**
 * singleton wrapper around icd9/proc codes mapping and rxnorm lookup
 * this enables caching across multiple patients
 */
public class RemoteInformationLookup {

    private static RemoteInformationLookup _instance;

    public ICD9MapperLookup icd9MapperLookup;
    public RxNormLookup rxNormLookup;
    public ProcedureSNOMEDLookup procedureSNOMEDLookup;

    private RemoteInformationLookup(Config config) {
        icd9MapperLookup = new ICD9MapperLookup(config);
        rxNormLookup = new RxNormLookup();
        procedureSNOMEDLookup = new ProcedureSNOMEDLookup(config);
    }

    public static RemoteInformationLookup getInstance(Config config) {
        if (_instance == null) {
            _instance = new RemoteInformationLookup(config);
        }
        return _instance;
    }
}
