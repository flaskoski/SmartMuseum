package flaskoski.rs.smartmuseum.location

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import flaskoski.rs.smartmuseum.model.Item
import flaskoski.rs.smartmuseum.util.ParseTime
import kotlin.math.roundToInt
import flaskoski.rs.smartmuseum.R
/**
 * Copyright (c) 2019 Felipe Ferreira Laskoski
 * código fonte licenciado pela MIT License - https://opensource.org/licenses/MIT
 */

class ScheduledItemMarker(map : GoogleMap, val item : Item){
    var marker : Marker? = setMarker(map)
    var isNextItem : Boolean = false

    init {
        updateLabel()
    }
//
    private fun setMarker(map : GoogleMap): Marker? {
        return item.getCoordinates()?.let {
            map.addMarker(
                    MarkerOptions().position(it)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.clock)))
        }
    }
    private fun updateLabel() {
        marker?.title = item.title

        marker?.snippet = "Clique aqui para ver horários"

//
//                item.getNextHour()?.let {
//                                """Próximo horário em: ${ParseTime.differenceInMinutesUntilNow(it).roundToInt()} min.
//                                |${if(!isNextItem)
//                                    "Clique aqui caso queira ir agora até esta atração."
//                                else
//                                    "Clique aqui para cancelar ida e retomar sua rota recomendada."}
//                                    """.trimMargin()}
//                            ?: "Nenhum Horário disponível. "
    }


}