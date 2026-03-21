package com.example.tabidachi.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.Subway
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Tram
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.tabidachi.ui.theme.TransportBus
import com.example.tabidachi.ui.theme.TransportCar
import com.example.tabidachi.ui.theme.TransportFerry
import com.example.tabidachi.ui.theme.TransportFlight
import com.example.tabidachi.ui.theme.TransportSubway
import com.example.tabidachi.ui.theme.TransportTaxi
import com.example.tabidachi.ui.theme.TransportTrain
import com.example.tabidachi.ui.theme.TransportTram
import com.example.tabidachi.ui.theme.TransportWalk

data class TransportInfo(val icon: ImageVector, val color: Color)

fun transportInfo(mode: String?): TransportInfo = when (mode) {
    "flight" -> TransportInfo(Icons.Default.Flight, TransportFlight)
    "train", "shinkansen" -> TransportInfo(Icons.Default.Train, TransportTrain)
    "subway" -> TransportInfo(Icons.Default.Subway, TransportSubway)
    "bus" -> TransportInfo(Icons.Default.DirectionsBus, TransportBus)
    "car" -> TransportInfo(Icons.Default.DirectionsCar, TransportCar)
    "ferry" -> TransportInfo(Icons.Default.DirectionsBoat, TransportFerry)
    "walk" -> TransportInfo(Icons.Default.DirectionsWalk, TransportWalk)
    "taxi" -> TransportInfo(Icons.Default.LocalTaxi, TransportTaxi)
    "tram" -> TransportInfo(Icons.Default.Tram, TransportTram)
    else -> TransportInfo(Icons.Default.ArrowForward, TransportFlight)
}
