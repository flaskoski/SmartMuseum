package flaskoski.rs.smartmuseum.util

import flaskoski.rs.smartmuseum.model.User

object ApplicationProperties {
    var user : User? = null

    fun userNotDefinedYet(): Boolean {
        //TODO check db if there is a current user
        return user == null
    }
}