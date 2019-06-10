package flaskoski.rs.smartmuseum.DAO

import android.util.Log
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import flaskoski.rs.smartmuseum.model.Item
import flaskoski.rs.smartmuseum.model.Point

class ItemDAO(val db: FirebaseFirestore = FirebaseFirestore.getInstance()){
    private val TAG = "ItemDAO"
    fun getAllPoints(callback : (itemsList : List<Point>)-> Unit) {
        //add items to grid from DB
        db.collection("items")
                .get()
                .addOnSuccessListener { result ->
                    val itemsList : ArrayList<Point> = ArrayList()
                    for (document in result) {
                        Log.d(TAG, document.id + " => " + document.data)
                        var item : Point
                        if(document["type"] == "item")
                            item = document.toObject(Item::class.java)
                        else  item = document.toObject(Point::class.java)
                        item.id = document.id
                        itemsList.add(item)
                    }
                    callback(itemsList)
                }
                .addOnFailureListener { exception ->
                   // Log.w(TAG, "Error getting documents.", exception)
                    throw Exception("Error getting items")
                }
    }
}