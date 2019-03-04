package flaskoski.rs.smartmuseum.propertiesAdmin

import android.content.Context
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.util.*

/**
 * Modified by Felipe on 9/19/2018.
 */
/**
 * @author Khurram
 */

class AssetPropertiesReader(private val context: Context) {
    private val properties: Properties

    init {
        /**
         * Constructs a new Properties object.
         */
        properties = Properties()
    }

    fun getProperties(FileName: String): Properties {
        var inputStream: InputStream? = null
        try {
            /**
             * getAssets() Return an AssetManager instance for your
             * application's package. AssetManager Provides access to an
             * application's raw asset files;
             */
            val assetManager = context.assets
            /**
             * Open an asset using ACCESS_STREAMING mode. This
             */
            inputStream = assetManager.open(FileName)
            /**
             * Loads properties from the specified InputStream,
             */
            properties.load(inputStream)

        } catch (e: IOException) {
            // TODO Auto-generated catch block
            Log.e("AssetsPropertyReader", e.toString())
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        return properties
    }
}
