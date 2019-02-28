package flaskoski.rs.rs_cf_test.recommender

import android.content.Context
import net.librec.conf.Configuration
import flaskoski.rs.smartmuseum.recommender.RSCustomConvertor.NioFreeTextDataModel
import net.librec.common.LibrecException
import net.librec.recommender.Recommender
import net.librec.recommender.RecommenderContext
import net.librec.recommender.cf.UserKNNRecommender
import net.librec.similarity.CosineSimilarity

class Recommender(val type: String,
                  val source : String,
                  val context: Context,
                  val knn : Int =  4,
                  val useRanking : Boolean = false) {

    private lateinit var recommender : Recommender

    init {
        when(type){
            "CF" -> {
                var conf = setConfiguration();
                var dataModel = NioFreeTextDataModel(conf, context)
                try {
                    dataModel.buildDataModel()


                    val similarity = CosineSimilarity()
                    similarity.buildSimilarityMatrix(dataModel)


                    recommender = UserKNNRecommender()
                    (recommender as UserKNNRecommender).recommend(RecommenderContext(conf, dataModel, similarity))

                } catch (e: LibrecException) {
                    e.printStackTrace()
                }

            }
        }
    }

    private fun setConfiguration(): Configuration {
        val conf = Configuration()
        conf.set("dfs.data.dir", "")
        conf.set("data.input.path", source)
        conf.set("data.column.format", "UIR")
        conf.set("data.conver.binariza.threshold", "-1.0")

        conf.set("data.model.splitter", "kcv")
        conf.set("data.splitter.cv.number", "5")
        conf.set("data.splitter.cv.index", "1")
        conf.set("rec.recommender.similarities", "user")

        conf.set("rec.neighbors.knn.number", knn.toString())
        conf.set("rec.recommender.isranking", useRanking.toString())

        return conf;
    }

}