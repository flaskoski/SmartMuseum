package flaskoski.rs.smartmuseum.DAO

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import flaskoski.rs.smartmuseum.model.Rating

class RatingDAO(val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    private val TAG = "RatingDAO"

    fun getAllByType(type : String, callback : (ratingList : List<Rating>) -> Unit) {
        //add items to grid from DB
        db.collection("ratings")
                .whereEqualTo("type", type)
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

    fun getAllItems(callback : (ratingList : List<Rating>) -> Unit) {
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
    fun add(rating: Rating){
        db.collection("ratings")
                .whereEqualTo("user", rating.user)
                .whereEqualTo("item", rating.item)
                .get().addOnSuccessListener { ratingsList ->
                    if(ratingsList.isEmpty)
                        db.collection("ratings").add(rating)
                    else
                        for(ratingItem in ratingsList){
                            ratingItem.reference.set(rating)
                        }
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error adding document", e)
                    //TODO offline mode with sync button
                }
    }
}
