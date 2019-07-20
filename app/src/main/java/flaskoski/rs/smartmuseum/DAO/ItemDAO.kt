package flaskoski.rs.smartmuseum.DAO

import android.util.Log
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import flaskoski.rs.smartmuseum.model.*
//import javax.inject.Inject

class ItemDAO /*@Inject constructor*/(val db: FirebaseFirestore = FirebaseFirestore.getInstance()){
    private val TAG = "ItemDAO"
    fun getAllPoints(callback : (itemsList : Set<Element>)-> Unit) {
        //add items to grid from DB
        db.collection("items")
                .get()
                .addOnSuccessListener { result ->
                    val itemsList = HashSet<Element>()
                    for (document in result) {
                        Log.d(TAG, document.id + " => " + document.data)
                        var item: Element
                        item = when (document["type"].toString().toLowerCase()) {
                            "item" -> document.toObject(Item::class.java)
                            "subitem" -> document.toObject(SubItem::class.java)
                            "groupitem" -> document.toObject(GroupItem::class.java)
                            else -> document.toObject(Point::class.java)
                        }
                        item.id = document.id
                        itemsList.add(item)
                    }
                    callback(itemsList)
                }
                .addOnFailureListener { _ ->
                   // Log.w(TAG, "Error getting documents.", exception)
                    throw Exception("Error getting items")
                }
    }
}