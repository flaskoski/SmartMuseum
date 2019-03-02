package flaskoski.rs.smartmuseum.recommender

import android.content.Context
import java.io.FileOutputStream
import java.io.OutputStreamWriter

class DatabaseIORequests(val context : Context,
                         var ratings : String = "",
                         private val filename : String = "ratings.txt"){

    init{
        if(ratings != "") {
            val outputStream: FileOutputStream

            try {
                outputStream = context.openFileOutput(filename, Context.MODE_PRIVATE)
                outputStream.write(ratings.toByteArray())
                outputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun write(userId : String, itemId : String, rating : Float){
        this.ratings = ratings.plus("$userId $itemId $rating")

        val outputStream: FileOutputStream
        val writer : OutputStreamWriter
        try {
            outputStream = context.openFileOutput(filename, Context.MODE_PRIVATE)
            writer = OutputStreamWriter(outputStream)
            writer.append(ratings)
            writer.close()
            outputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    fun write(ratings : String){
        this.ratings = this.ratings.plus(ratings)

        val outputStream: FileOutputStream
        val writer : OutputStreamWriter
        try {
            outputStream = context.openFileOutput(filename, Context.MODE_PRIVATE)
            writer = OutputStreamWriter(outputStream)
            writer.append(ratings)
            writer.close()
            outputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}