package flaskoski.rs.smartmuseum

import android.content.Intent
import flaskoski.rs.smartmuseum.model.Item
import flaskoski.rs.smartmuseum.model.Rating
import flaskoski.rs.smartmuseum.model.User
import flaskoski.rs.smartmuseum.util.ApplicationProperties
import flaskoski.rs.smartmuseum.viewmodel.JourneyManager
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.spy
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class JourneyManagerTest{
//    @get:Rule
//    val rule = InstantTaskExecutorRule()
//
//    companion object {
//        private lateinit var journeyManager: JourneyManager
//        private val itemsList = ArrayList<Item>()
//        private val ratingList = ArrayList<Rating>()
//        private val mockUser = User("Felipe", "Felipe", 120.0)
//
//
//        @BeforeClass @JvmStatic fun setup(){
//            journeyManager = spy(JourneyManager::class.java)
//            `when`(journeyManager.getItemsData()).thenAnswer {
//                itemsList.add(Item("1", "a1", lat = -23.651397, lng = -46.622152, isEntrance = true))
//                itemsList.add(Item("2", "b2", lat = -23.6514, lng = -46.6222, isEntrance = false))
//                itemsList.add(Item("3", "c3", lat = -23.6515, lng = -46.6223, isEntrance = false))
//                ratingList.add(Rating(mockUser.id, "1", 4F, recommendationSystem = ApplicationProperties.SYSTEM_USER_BASED))
//                ratingList.add(Rating(mockUser.id, "2", 5F, recommendationSystem = ApplicationProperties.SYSTEM_USER_BASED))
//            }
//            journeyManager.isItemsAndRatingsLoaded = true
//        }
//    }
//
//
//    @Test
//    fun itemRatingChangedButNotMovedToNextItem(){
//        Assert.assertTrue(ratingList.filter { it.user == mockUser.id && it.item == "1" }[0].rating == 4f)
//
//        val intent = Intent()
//        intent.putExtra(ApplicationProperties.TAG_ITEM_RATING, Rating(mockUser.id, "1", 3f, recommendationSystem = ApplicationProperties.SYSTEM_USER_BASED))
//        intent.putExtra(ApplicationProperties.TAG_GO_NEXT_ITEM, false)
//        journeyManager.itemRatingChangeResult(intent)
//
//        Assert.assertTrue(ratingList.filter { it.user == mockUser.id && it.item == "1" }[0].rating == 3f)
//    }
}