package flaskoski.rs.smartmuseum.propertiesAdmin

import android.content.Context
import java.io.Serializable
import java.util.*

/**
 * /**
 * Copyright (c) 2019 Felipe Ferreira Laskoski
 * código fonte licenciado pela MIT License - https://opensource.org/licenses/MIT
*/
 * Created by Felipe on 10/8/2018.
 * Uses application.properties
 */

class ToggleRouter(context: Context) : Serializable {
    internal var featureConfig = Hashtable<String, Boolean>()
    private val applicationProperties : Properties

    init {
        val assetPropertiesReader = AssetPropertiesReader(context)
        this.applicationProperties = assetPropertiesReader.getProperties("application.properties")
    }

    fun setAllFeatures() {
        val e = applicationProperties.propertyNames()
        while (e.hasMoreElements()) {
            val key = e.nextElement() as String
            if (key.startsWith("feature.")) {
                val value = java.lang.Boolean.valueOf(applicationProperties.getProperty(key))
                featureConfig[key] = value
            }
        }
    }

    fun setFeature(feature: String, isEnabled: Boolean?) {
        featureConfig[feature] = isEnabled!!
    }

    fun featureIsEnabled(feature: String): Boolean? {
        return featureConfig[feature]
    }
}
