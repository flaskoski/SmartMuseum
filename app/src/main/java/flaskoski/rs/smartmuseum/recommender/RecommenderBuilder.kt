package flaskoski.rs.smartmuseum.recommender

import android.util.Log
import flaskoski.rs.smartmuseum.model.Rating
import net.librec.conf.Configuration
import flaskoski.rs.smartmuseum.recommender.RSCustomConvertor.NioFreeTextDataModel
import net.librec.recommender.AbstractRecommender
import net.librec.recommender.RecommenderContext
import net.librec.recommender.cf.UserKNNRecommender
import net.librec.similarity.CosineSimilarity
import net.librec.similarity.PCCSimilarity

class RecommenderBuilder{

    private var recommender : AbstractRecommender? = null
    private lateinit var knn : String
    private var useRanking: Boolean = false
    private val TAG = "RecommenderBuilder"

    companion object {
        const val SIMILARITY_PCC = "PCC"
    }

    fun buildKNNRecommender(ratings: Set<Rating>,
                            knn: Int =  4,
                            useRanking: Boolean = false,
                            similarityMethod: String = SIMILARITY_PCC) : AbstractRecommender? {
        if(ratings.isEmpty()){
            Log.e(TAG, "buildKNNRecommender called but ratingList is empty!")
            return null
        }


        this.knn = knn.toString()
        this.useRanking = useRanking

        val conf = setConfiguration()
        val dataModel = NioFreeTextDataModel(conf, ratings.toMutableList())
        dataModel.buildDataModel()

        val similarity =
                if(similarityMethod == SIMILARITY_PCC)
                    PCCSimilarity()
                else
                    CosineSimilarity()
        similarity.buildSimilarityMatrix(dataModel)
//data.splitter.cv.number

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

        conf.set("rec.neighbors.knn.number", knn)
        conf.set("rec.recommenderManager.isranking", useRanking.toString())

        return conf
    }

}