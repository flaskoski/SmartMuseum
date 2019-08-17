package flaskoski.rs.smartmuseum.DAO

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import flaskoski.rs.smartmuseum.model.Rating

class RatingDAO(val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    private val TAG = "RatingDAO"

    fun getAllFromUserByType(userId : String, type : String, isFeaturePreferences : Boolean, callback : (ratingList : List<Rating>) -> Unit) {
        //add items to grid from DB
        val collectionId =
                if (isFeaturePreferences) "ratings"
                else "questionnaire"
        db.collection(collectionId)
                .whereEqualTo("type", type)
                .whereEqualTo("user", userId)
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
                .limit(4000).get()
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
                    if(ratingsList.isEmpty){
                        db.collection("ratings").add(rating)
                        Log.i(TAG, "Rating added on db: $rating")
                    }
                    else
                        for(ratingItem in ratingsList){
                            ratingItem.reference.set(rating)
                            Log.i(TAG, "Rating changed on db: $rating")
                        }
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error adding document", e)
                    //TODO offline mode with sync button
                }
    }
    fun addQuestionnaireAnswer(rating: Rating){
        db.collection("questionnaire")
                .whereEqualTo("user", rating.user)
                .whereEqualTo("item", rating.item)
                .get().addOnSuccessListener { answerList ->
                    if(answerList.isEmpty){
                        db.collection("questionnaire").add(rating)
                        Log.i(TAG, "Answer added on db: $rating")
                    }

                    else
                        for(answer in answerList){
                            answer.reference.set(rating)
                            Log.i(TAG, "Answer changed on db: $rating")
                        }
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error adding document", e)
                    //TODO offline mode with sync button
                }
    }
}
