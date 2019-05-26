package flaskoski.rs.rs_cf_test.recommender

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import flaskoski.rs.smartmuseum.DAO.ItemDAO
import flaskoski.rs.smartmuseum.DAO.RatingDAO
import flaskoski.rs.smartmuseum.model.Item
import flaskoski.rs.smartmuseum.model.Rating
import net.librec.conf.Configuration
import flaskoski.rs.smartmuseum.recommender.RSCustomConvertor.NioFreeTextDataModel
import net.librec.common.LibrecException
import net.librec.recommender.Recommender
import net.librec.recommender.RecommenderContext
import net.librec.recommender.cf.UserKNNRecommender
import net.librec.similarity.CosineSimilarity

class RecommenderBuilder{

    private var recommender : Recommender? = null
    private lateinit var knn : String
    private var useRanking: Boolean = false
    private val TAG = "RecommenderBuilder"

    fun buildKNNRecommender(ratings : List<Rating>,
                            context: Context,
                            knn : Int =  4,
                            useRanking : Boolean = false) : Recommender{
        this.knn = knn.toString()
        this.useRanking = useRanking

        var conf = setConfiguration();
        var dataModel = NioFreeTextDataModel(conf, ratings, context)
        dataModel.buildDataModel()


        val similarity = CosineSimilarity()
        similarity.buildSimilarityMatrix(dataModel)


        recommender = UserKNNRecommender()
        (recommender as UserKNNRecommender).recommend(RecommenderContext(conf, dataModel, similarity))

        return recommender as UserKNNRecommender
    }

    private fun setConfiguration(): Configuration {
        val conf = Configuration()
        conf.set("dfs.data.dir", "")
        conf.set("data.input.path", "")
        conf.set("data.column.format", "UIR")
        conf.set("data.conver.binariza.threshold", "-1.0")

        conf.set("data.model.splitter", "kcv")
        conf.set("data.splitter.cv.number", "5")
        conf.set("data.splitter.cv.index", "1")
        conf.set("rec.recommenderManager.similarities", "user")

        conf.set("rec.neighbors.knn.number", knn.toString())
        conf.set("rec.recommenderManager.isranking", useRanking.toString())

        return conf;
    }

}