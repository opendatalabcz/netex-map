package cz.cvut.fit.gaierda1.data.orm.model

import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration
import org.rutebanken.netex.model.VehicleModeEnumeration

enum class TransportMode(val shortCode: String) {
    BUS("B"),
    TROLLEY_BUS("Y"),
    RAIL("R"),
    FUNICULAR("F"),
    TRAM("T"),
    METRO("M");

    companion object {
        fun fromShortCode(code: String): TransportMode = when (code.uppercase()) {
            "B" -> BUS
            "Y" -> TROLLEY_BUS
            "R" -> RAIL
            "F" -> FUNICULAR
            "T" -> TRAM
            "M" -> METRO
            else -> throw IllegalArgumentException("Invalid transport mode short code: $code")
        }

        fun fromNetexTransportMode(mode: AllVehicleModesOfTransportEnumeration): TransportMode = when (mode) {
            AllVehicleModesOfTransportEnumeration.BUS -> BUS
            AllVehicleModesOfTransportEnumeration.TROLLEY_BUS -> TROLLEY_BUS
            AllVehicleModesOfTransportEnumeration.RAIL -> RAIL
            AllVehicleModesOfTransportEnumeration.FUNICULAR -> FUNICULAR
            AllVehicleModesOfTransportEnumeration.TRAM -> TRAM
            AllVehicleModesOfTransportEnumeration.METRO -> METRO
            else -> throw IllegalArgumentException("Unsupported transport mode: $mode")
        }

        fun fromNetexVehicleMode(mode: VehicleModeEnumeration): TransportMode = when (mode) {
            VehicleModeEnumeration.BUS -> BUS
            VehicleModeEnumeration.TROLLEY_BUS -> TROLLEY_BUS
            VehicleModeEnumeration.RAIL -> RAIL
            VehicleModeEnumeration.FUNICULAR -> FUNICULAR
            VehicleModeEnumeration.TRAM -> TRAM
            VehicleModeEnumeration.METRO -> METRO
            else -> throw IllegalArgumentException("Unsupported vehicle mode: $mode")
        }
    }
}
