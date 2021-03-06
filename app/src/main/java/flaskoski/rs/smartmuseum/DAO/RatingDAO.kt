package flaskoski.rs.smartmuseum.DAO

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import flaskoski.rs.smartmuseum.model.Rating
import flaskoski.rs.smartmuseum.model.UserRatings
import flaskoski.rs.smartmuseum.util.ParseTime
import java.util.*
import kotlin.collections.HashMap
/**
 * Copyright (c) 2019 Felipe Ferreira Laskoski
 * código fonte licenciado pela MIT License - https://opensource.org/licenses/MIT
 */
class RatingDAO(val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    private val TAG = "RatingDAO"

    companion object {
        const val COLLECTION_ALL_RATINGS = "ratings_by_user"
        const val COLLECTION_RATINGS_OF_USER = "ratings_of_user"
        const val COLLECTION_QUESTIONNAIRE = "questionnaire"
    }

    fun getAllFromUserByType(userId : String, type : String, isFeaturePreferences : Boolean, callback : (ratingList : List<Rating>) -> Unit) {
        //add items to grid from DB

        if (isFeaturePreferences)
            db.collection(COLLECTION_ALL_RATINGS)
                    .document(userId).get()
                    .addOnSuccessListener { document ->
                        val ratingsOfUser : UserRatings? = document.toObject(UserRatings::class.java)
                        val ratingList : java.util.ArrayList<Rating> = java.util.ArrayList()
                        ratingsOfUser?.ratings_of_user?.let {map ->
                            ratingList.addAll( map.filter { it.value.type == type}.values)
                        }
                        callback(ratingList)
                    }.addOnFailureListener { exception ->
                        Log.e(TAG, "Error getting documents.", exception)
                        //throw Exception("Error getting ratings")
                    }
        else
            db.collection(COLLECTION_QUESTIONNAIRE)
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
        db.collection(COLLECTION_ALL_RATINGS)
                .limit(250).get()
                .addOnSuccessListener { result ->
                    var ratingsOfUser : UserRatings
                    val ratingList : java.util.ArrayList<Rating> = java.util.ArrayList()
                    for (document in result) {
                        ratingsOfUser = document.toObject(UserRatings::class.java)
                        ratingList.addAll(ratingsOfUser.ratings_of_user.values)
                    }
                    callback(ratingList)
                }.addOnFailureListener { exception ->
                    Log.w(TAG, "Error getting documents.", exception)
                    throw Exception("Error getting ratings")
                }
    }
    fun add(rating: Rating){
        db.collection(COLLECTION_ALL_RATINGS)
                .document(rating.user)
                .get().addOnSuccessListener { document ->
                    //.collection(COLLECTION_RATINGS_OF_USER).whereEqualTo("item", rating.item)
                    //user's first rating
                    if(!document.exists() || document[COLLECTION_RATINGS_OF_USER] == null){
                        //user's first rating
                        val userRatings = UserRatings(mapOf(Pair(UUID.randomUUID().toString(), rating)) as HashMap<String, Rating>)
                        db.collection(COLLECTION_ALL_RATINGS).document(rating.user)
                                .set(userRatings)
                        Log.i(TAG, "Rating added on db: $rating")
                    }else{
                        //user has already rated an item
                        val ratingsOfUser = document.toObject(UserRatings::class.java)
                        ratingsOfUser?.ratings_of_user?.let {ratingsMap ->
                            val ratingToBeModified = ratingsMap.filter { it.value.item == rating.item }
                            if(ratingToBeModified.isEmpty())
                                ratingsMap[UUID.randomUUID().toString()] = rating
                            else {
                                val key = ratingToBeModified.keys.first()
                                ratingsMap[key] = rating
                            }
                            ratingsOfUser.updatedAt = ParseTime.getCurrentTime()
                            document.reference.set(ratingsOfUser)
                                    .addOnSuccessListener {
                                        Log.i(TAG, "Rating added/updated on db: $rating")
                                    }.addOnFailureListener {e->
                                        Log.w(TAG, "3.Error adding document", e)
                                    }

                        }?: Log.w(TAG, "2.Error adding document")


                    }
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "1.Error adding document", e)
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

    fun addAll(ratings: ArrayList<Rating>) {
        val userId = ratings.first().user
        db.collection(COLLECTION_ALL_RATINGS)
                .document(userId)
                .get().addOnSuccessListener { document ->
                    //.collection(COLLECTION_RATINGS_OF_USER).whereEqualTo("item", rating.item)
                    //user's first rating
                    if(!document.exists() || document[COLLECTION_RATINGS_OF_USER] == null){
                        //user's first rating
                        val userRatings = UserRatings(ratings.map{UUID.randomUUID().toString() to it}.toMap() as HashMap<String, Rating>)
                        db.collection(COLLECTION_ALL_RATINGS).document(userId).set(userRatings)

                        Log.i(TAG, "${ratings.size} ratings added on db for user $userId")
                    }else{
                        //user has already rated an item
                        val ratingsOfUser = document.toObject(UserRatings::class.java)
                        ratingsOfUser?.ratings_of_user?.let {ratingsMap ->
                            for(rating in ratings) {
                                val ratingToBeModified = ratingsMap.filter { it.value.item == rating.item }
                                if (ratingToBeModified.isEmpty())
                                    ratingsMap[UUID.randomUUID().toString()] = rating
                                else {
                                    val key = ratingToBeModified.keys.first()
                                    ratingsMap[key] = rating
                                }
                            }
                            ratingsOfUser.updatedAt = ParseTime.getCurrentTime()
                            document.reference.set(ratingsOfUser)
                                .addOnSuccessListener {
                                    Log.i(TAG, "${ratings.size} ratings added/modified on db for user $userId")
                                }.addOnFailureListener { e ->
                                    Log.w(TAG, "3.Error adding documents", e)
                                }
                        }?: Log.w(TAG, "2.Error adding document")


                    }
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "1.Error adding document", e)
                    //TODO offline mode with sync button
                }
    }
}
