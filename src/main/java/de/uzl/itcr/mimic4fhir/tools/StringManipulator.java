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

public class StringManipulator {

    public static String conformIcdString(String icdCode){
        //White space characters are not allowed in https query
        String icdString = icdCode.replaceAll("\\s", "");
        if(icdString.length() > 3){
            String firstComp = icdString.substring(0, 3).concat(".");
            String secComp = icdString.substring(3);
            return firstComp + secComp;
        }
        else{
            return icdString;
        }
    }

}
