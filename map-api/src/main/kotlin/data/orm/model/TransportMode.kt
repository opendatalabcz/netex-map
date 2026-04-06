package cz.cvut.fit.gaierda1.data.orm.model

import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration

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
    }
}
