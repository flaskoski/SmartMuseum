package flaskoski.rs.smartmuseum.model

import flaskoski.rs.smartmuseum.util.ParseTime
import java.util.*
import kotlin.collections.HashMap
/**
 * Copyright (c) 2019 Felipe Ferreira Laskoski
 * código fonte licenciado pela MIT License - https://opensource.org/licenses/MIT
 */

class UserRatings(var ratings_of_user: HashMap<String, Rating> = HashMap(),
                  var updatedAt: Date = ParseTime.getCurrentTime() )