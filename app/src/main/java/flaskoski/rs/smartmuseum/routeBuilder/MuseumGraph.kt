package flaskoski.rs.smartmuseum.routeBuilder

import flaskoski.rs.smartmuseum.model.Arc
import flaskoski.rs.smartmuseum.model.Item
import flaskoski.rs.smartmuseum.model.Point

class MuseumGraph(val vertices : Set<Point>,
                  val arcs : Set<Arc>) {
    val entrances: Set<Item> = HashSet()
    val exits : Set<Item> = HashSet()

    init{
        for(vertex in vertices){
            if (vertex.isEntrance)
                entrances.plus(vertex)
            if (vertex.isExit)
                exits.plus(vertex)
        }
    }



}
