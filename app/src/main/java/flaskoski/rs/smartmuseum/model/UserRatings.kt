package flaskoski.rs.smartmuseum.model

import flaskoski.rs.smartmuseum.util.ParseTime
import java.util.*
import kotlin.collections.HashMap


class UserRatings(var ratings_of_user: HashMap<String, Rating> = HashMap(),
                  var updatedAt: Date = ParseTime.getCurrentTime() )