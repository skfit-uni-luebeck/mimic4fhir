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

import java.util.ArrayList;
import java.util.List;

public class Ingredient {

    private String description;
    private String rxCui;
    private List<String> atcCodes;
    private List<String> snomedCodes;
    private List<String> uniiCodes;

    public Ingredient(String description, String rxCui){
        this.description = description;
        this.rxCui = rxCui;
        this.atcCodes = new ArrayList<>();
        this.snomedCodes = new ArrayList<>();
        this.uniiCodes = new ArrayList<>();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRxCui() {
        return rxCui;
    }

    public void setRxCui(String rxCui) {
        this.rxCui = rxCui;
    }

    public List<String> getAtcCodes() {
        return atcCodes;
    }

    public void addAtcCode(String atcCode) {
        this.atcCodes.add(atcCode);
    }

    public void addAtcCodes(List<String> atcCodes) {
        this.atcCodes.addAll(atcCodes);
    }

    public List<String> getSnomedCodes() {
        return snomedCodes;
    }

    public void addSnomedCode(String snomedCode) {
        this.snomedCodes.add(snomedCode);
    }

    public void addSnomedCodes(List<String> snomedCodes) {
        this.snomedCodes.addAll(snomedCodes);
    }

    public List<String> getUniiCodes() {
        return uniiCodes;
    }

    public void addUniiCode(String uniiCode) {
        this.uniiCodes.add(uniiCode);
    }

    public void addUniiCodes(List<String> uniiCodes) {
        this.uniiCodes.addAll(uniiCodes);
    }

}
