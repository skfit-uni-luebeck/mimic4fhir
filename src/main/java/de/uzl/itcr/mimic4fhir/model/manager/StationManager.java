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
package de.uzl.itcr.mimic4fhir.model.manager;

import ca.uhn.fhir.model.primitive.IdDt;
import de.uzl.itcr.mimic4fhir.model.MStation;

import org.hl7.fhir.r4.model.*;

import java.util.HashMap;
import java.util.List;

public class StationManager {

    private final HashMap<String, MStation> stationMap;
    private final HashMap<MStation, Organization> orgaMap;
    private final HashMap<MStation, Location> locationMap;

    public StationManager(List<String> stationNames){
        this.stationMap = new HashMap<>();
        this.orgaMap = new HashMap<>();
        this.locationMap = new HashMap<>();

        for(String stationName : stationNames) {
            if (stationName != null) {
                MStation station = new MStation(stationName);
                this.stationMap.put(stationName, station);
            }
        }
    }

    public MStation getStation(String stationName) {
        if (stationName != null) {
            return this.stationMap.get(stationName);
        } else {
            return this.stationMap.get("Unknown");
        }
    }

    public Organization getOrganization(MStation station){
        if(this.orgaMap.containsKey(station)){
            return this.orgaMap.get(station);
        }
        else{
            Organization orga = new Organization();

            //Set station ID
            orga.setId(String.valueOf(IdDt.newRandomUuid()));

            //Add identifier
            orga.addIdentifier().setSystem("http://www.imi-mimic.de/organisation/" + station.getStationName().trim().replaceAll("\\s+", "_"))
                    .setValue(station.getStationName().trim())
                    .setType(new CodeableConcept(new Coding().setSystem("http://terminology.hl7.org/CodeSystem/v2-0203")
                            .setCode("XX").setDisplay("Organization identifier")));

            //Add organization type; In this care it's always a hospital department
            orga.addType().addCoding(new Coding().setSystem("http://terminology.hl7.org/CodeSystem/organization-type")
                    .setCode("dept").setDisplay("Hospital Department"));

            //Set the name of the station
            orga.setName(station.getStationName());

            this.orgaMap.put(station, orga);
            return orga;
        }
    }

    public Location getLocation(MStation station){
        if(this.locationMap.containsKey(station)) {
            return this.locationMap.get(station);
        }
        else{
            Location location = new Location();

            //Set location ID
            location.setId(String.valueOf(IdDt.newRandomUuid()));

            //Add identifier
            location.addIdentifier().setSystem("http://www.imi-mimic.de/location/" + station.getStationName().trim().replaceAll("\\s+", "_"))
                    .setValue(station.getStationName().trim());

            //Add type
            location.addType(new CodeableConcept(new Coding().setSystem("http://terminology.hl7.org/ValueSet/v3-ServiceDeliveryLocationRoleType")
                    .setCode("_ServiceDeliveryLocationRoleType").setDisplay("ServiceDeliveryLocationRoleType")));

            //Add physical type; Of course, all locations are part of a medical facility, i.e. a ward
            location.setPhysicalType(new CodeableConcept(new Coding().setSystem("http://terminology.hl7.org/CodeSystem/location-physical-type")
                    .setCode("wa").setDisplay("Ward")));

            //Add name (will be the same as the station name)
            location.setName(station.getStationName());

            //Add the station as the managing organization
            location.setManagingOrganization(new Reference(this.getOrganization(station).getId()));

            this.locationMap.put(station, location);
            return location;
        }
    }
}
