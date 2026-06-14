package com.carscanner.app.obd

data class ObdCommand(
    val name: String,
    val command: String,
    val unit: String,
    val formula: (String) -> Float?
)

object ObdPids {
    val supportedPids = listOf(
        ObdCommand("Engine RPM", "010C", "rpm", { raw ->
            raw.trim().split(" ").takeLast(2).let { parts ->
                if (parts.size == 2) {
                    val a = parts[0].toIntOrNull(16) ?: return@let null
                    val b = parts[1].toIntOrNull(16) ?: return@let null
                    ((a * 256) + b) / 4f
                } else null
            }
        }),
        ObdCommand("Vehicle Speed", "010D", "km/h", { raw ->
            raw.trim().split(" ").lastOrNull()?.toIntOrNull(16)?.toFloat()
        }),
        ObdCommand("Coolant Temp", "0105", "°C", { raw ->
            raw.trim().split(" ").lastOrNull()?.toIntOrNull(16)?.minus(40)?.toFloat()
        }),
        ObdCommand("Engine Load", "0104", "%", { raw ->
            raw.trim().split(" ").lastOrNull()?.toIntOrNull(16)?.let { it * 100 / 255f }
        }),
        ObdCommand("Intake Temp", "010F", "°C", { raw ->
            raw.trim().split(" ").lastOrNull()?.toIntOrNull(16)?.minus(40)?.toFloat()
        }),
        ObdCommand("MAF Air Flow", "0110", "g/s", { raw ->
            raw.trim().split(" ").takeLast(2).let { parts ->
                if (parts.size == 2) {
                    val a = parts[0].toIntOrNull(16) ?: return@let null
                    val b = parts[1].toIntOrNull(16) ?: return@let null
                    ((a * 256) + b) / 100f
                } else null
            }
        }),
        ObdCommand("Throttle Position", "0111", "%", { raw ->
            raw.trim().split(" ").lastOrNull()?.toIntOrNull(16)?.let { it * 100 / 255f }
        }),
        ObdCommand("Timing Advance", "010E", "°", { raw ->
            raw.trim().split(" ").lastOrNull()?.toIntOrNull(16)?.let { (it / 2f) - 64 }
        }),
        ObdCommand("Fuel Pressure", "010A", "kPa", { raw ->
            raw.trim().split(" ").lastOrNull()?.toIntOrNull(16)?.times(3)?.toFloat()
        }),
        ObdCommand("O2 Sensor Voltage", "0114", "V", { raw ->
            raw.trim().split(" ").takeLast(2).let { parts ->
                if (parts.size == 2) {
                    val a = parts[0].toIntOrNull(16) ?: return@let null
                    val b = parts[1].toIntOrNull(16) ?: return@let null
                    (a * 256 + b) * 8f / 65535f
                } else null
            }
        }),
        ObdCommand("Fuel Level", "012F", "%", { raw ->
            raw.trim().split(" ").lastOrNull()?.toIntOrNull(16)?.let { it * 100 / 255f }
        }),
        ObdCommand("Run Time", "011F", "s", { raw ->
            raw.trim().split(" ").takeLast(2).let { parts ->
                if (parts.size == 2) {
                    val a = parts[0].toIntOrNull(16) ?: return@let null
                    val b = parts[1].toIntOrNull(16) ?: return@let null
                    (a * 256 + b).toFloat()
                } else null
            }
        }),
    )

    val dtcCommand = "03"
    val clearDtcCommand = "04"
}
