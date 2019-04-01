package flaskoski.rs.smartmuseum.DAO

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import flaskoski.rs.smartmuseum.model.Rating

class RatingDAO(val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    private val TAG = "RatingDAO"
    fun getAllItems(callback : (itemsList : List<Rating>) -> Unit) {
        //add items to grid from DB
        db.collection("ratings")
                .get()
                .addOnSuccessListener { result ->
                    val ratingsList : ArrayList<Rating> = ArrayList()
                    for (document in result) {
                        ratingsList.add(document.toObject(Rating::class.java))
                    }
                    callback(ratingsList)
                }.addOnFailureListener { exception ->
                    Log.w(TAG, "Error getting documents.", exception)
                    throw Exception("Error getting ratings")
                }
    }
}
