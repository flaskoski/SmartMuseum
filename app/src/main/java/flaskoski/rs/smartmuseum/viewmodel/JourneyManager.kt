package flaskoski.rs.smartmuseum.viewmodel
import android.app.Activity
import android.content.Intent
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import android.util.Log
import androidx.databinding.Observable
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.storage.FirebaseStorage
import flaskoski.rs.smartmuseum.recommender.RecommenderBuilder
import flaskoski.rs.smartmuseum.DAO.SharedPreferencesDAO
import flaskoski.rs.smartmuseum.activity.MainActivity
import flaskoski.rs.smartmuseum.location.MapManager
import flaskoski.rs.smartmuseum.location.UserLocationManager
import flaskoski.rs.smartmuseum.model.*
import flaskoski.rs.smartmuseum.routeBuilder.RecommendedRouteBuilder
import flaskoski.rs.smartmuseum.util.ApplicationProperties
import flaskoski.rs.smartmuseum.util.ParseTime
import java.lang.StringBuilder
import java.util.*
//import javax.inject.Inject
import kotlin.collections.ArrayList
import kotlin.math.roundToInt

class JourneyManager //@Inject constructor(itemRepository: ItemRepository)
    : ViewModel(), MapManager.OnUserArrivedToDestinationListener {

    companion object {
        val MESSAGE_FINISH: String = """Você já visitou todas as atrações recomendadas para você dentro do seu tempo disponível.
                        |""".trimMargin()
        val MESSAGE_QUESTIONNAIRE = "Por favor nos informe agora o que achou da visita com essa rápida pesquisa."
    }

    var checkedForUpdates: Boolean = false
    var recommendedRouteBuilder : RecommendedRouteBuilder? = null
//    var closestItem: Point? = null
    var lastItem : Point? = null
    private var nextItem: Item? = null
    private val TAG = "JourneyManager"

    var ratingsList  = HashSet<Rating>()
    var itemsList = ArrayList<Item>()
    private var subItemList = ArrayList<SubItem>()
//    var currentItem : Item? = null

    //--state flags
    var itemListChangedListener : (()->Unit)? = null
    var isItemsAndRatingsLoaded = MutableLiveData<Boolean>()
    var isPreferencesSet = MutableLiveData<Boolean>()

    var isGoToNextItem = MutableLiveData<Boolean>()
    var isJourneyFinishedFlag = MutableLiveData<Boolean>()
    var isJourneyBegan = MutableLiveData<Boolean>()
    var finishMessage = MESSAGE_FINISH + MESSAGE_QUESTIONNAIRE

    var showNextItem_okPressed = false
    var isCloseToItem = MutableLiveData<Boolean>()

    var isMapLoaded: Boolean = false
    private var whenMapLoadedSetDestination: Boolean = false
    //tell activity if the route was reconfigured due to the new ratings given
    var isRatingChanged: Boolean = false
    var finishButtonClicked: Boolean = false
    var isQuestionnaireAnswered : Boolean = false
    //--
    var isFirstTime : Boolean = true

    private var startTime: Date? = null

    var mapManager: MapManager? = null
    var userLocationManager : UserLocationManager? = null

    private var activity: Activity? = null
    private lateinit var sharedPreferences: SharedPreferencesDAO

    //GET ITEMS INFORMATION
    private val isItemAndRatingListLoadedListener = object : Observable.OnPropertyChangedCallback(){
        override fun onPropertyChanged(observable: Observable, i: Int) {
            initializeListsAndBuildRecommendations()
    }}

    private fun isPreferencesSetTrue(){
        isPreferencesSet.value = true
        initializeListsAndBuildRecommendations()
    }

    private fun initializeListsAndBuildRecommendations(){
        //if items and ratings were downloaded, preferences are set and lists weren't initialized yet
        if(ItemRepository.isItemListLoaded.get() && ItemRepository.isRatingListLoaded.get()
                && ItemRepository.itemList.isNotEmpty() && ItemRepository.ratingList.any { it.user != ApplicationProperties.user?.id }
                && isPreferencesSet.value!! && !isItemsAndRatingsLoaded.value!!){
            itemsList.addAll(ItemRepository.itemList)

            recommendedRouteBuilder = RecommendedRouteBuilder(ItemRepository.allElements)
            buildRecommender()
            isItemsAndRatingsLoaded.value = true
        }
    }


    private fun getFirstItem() : Point?{

        return userLocationManager?.userLatLng?.let {recommendedRouteBuilder?.getNearestPointFromUser(Point(it))}
                ?: userLocationManager?.userLastKnownLocation?.let{
                    recommendedRouteBuilder?.getNearestPointFromUser(Point(it))}
//                            ?: recommendedRouteBuilder?.getAllEntrances()?.first()}
    }

//    private val seeScheduledItemDetails: (item : Item) -> Unit = { item : Item ->
//        Log.w(TAG, "colocar como primeiro o item ${item.id}")
//        recommendedRouteBuilder?.addItemToRouteAsFirst(item)
//                ?: Log.e(TAG, "removeItemFromRoute - recommendedRouteBuilder is null")
//        sharedPreferences.removeItem(itemToBeRemoved)
////                ?: Log.e(TAG, "removeItemFromRoute - sharedPreferences is null")
//        sortItemList()
//        setNextRecommendedDestination()
//        itemRemovedConfirmationCallback.invoke()
//    }

    init {
        //live data vars initialization
        isCloseToItem.value = false
        isPreferencesSet.value = false
        isItemsAndRatingsLoaded.value = false
        isGoToNextItem.value = false
        isJourneyBegan.value = false
        isJourneyFinishedFlag.value = false

        ratingsList = ItemRepository.ratingList
        subItemList = ItemRepository.subItemList

        //Repository observers init
        ItemRepository.isRatingListLoaded.addOnPropertyChangedCallback(isItemAndRatingListLoadedListener)
        ItemRepository.isItemListLoaded.addOnPropertyChangedCallback(isItemAndRatingListLoadedListener)
        //maps setup
        userLocationManager = UserLocationManager(MainActivity.REQUEST_CHANGE_LOCATION_SETTINGS)
        mapManager = MapManager(this){
            isMapLoaded = true
            Log.i(TAG, "Map loaded")
            if(whenMapLoadedSetDestination) {
                whenMapLoadedSetDestination = false
                setNextRecommendedDestination()
            }
                //mapManager?.setDestination()
        }
        userLocationManager?.onUserLocationUpdateCallbacks = arrayListOf(mapManager?.updateUserLocationCallback,{
            if(lastItem == null) {
                recoverCurrentState()
            }
        })

        //If exited the app but didn't terminate it and then came back, the ItemRepository will already be loaded
        if(ItemRepository.itemList.isNotEmpty() && ItemRepository.ratingList.isNotEmpty())
            isItemAndRatingListLoadedListener.onPropertyChanged(ItemRepository.isItemListLoaded, 0)
    }


    fun updateActivity(activity : Activity) {
        this.activity = activity
        userLocationManager?.updateActivity(activity)
        sharedPreferences = SharedPreferencesDAO(activity)
        mapManager?.mapsActivity = activity
    }

    private fun updateRecommender() {
//        Toast.makeText(applicationContext, "Atualizando recomendações...", Toast.LENGTH_SHORT).show()
        buildRecommender()
//        Toast.makeText(applicationContext, "Atualizado!", Toast.LENGTH_SHORT).show()
    }

    private fun buildRecommender() {
        ItemRepository.recommenderManager.recommender = RecommenderBuilder().buildKNNRecommender(ratingsList)

        if(!ApplicationProperties.userNotDefinedYet()) {
            for(item in itemsList){
                val rating = ItemRepository.recommenderManager.getPrediction(ApplicationProperties.user!!.id, item.id)
                if (rating != null)
                    item.recommedationRating = rating
                else item.recommedationRating = 3F
            }
            for(item in subItemList){
                val rating = ItemRepository.recommenderManager.getPrediction(ApplicationProperties.user!!.id, item.id)
                if (rating != null)
                    item.recommedationRating = rating
                else item.recommedationRating = 3F
            }
        }
        itemListChangedListener?.invoke()
    }

    private fun sortItemList() {
        var i = 0
        itemsList.sortedWith(compareBy<Itemizable>{it.isVisited}
                .thenBy{ (it as RoutableItem).recommendedOrder}
                .thenBy{it.title}).forEach{
            itemsList[i++] = it
        }
        itemListChangedListener?.invoke()
    }

    override fun onUserArrivedToDestination() {
        isCloseToItem.value = true
    }

    private fun isNoItemsOnRoute(): Boolean {
        if(itemsList.isEmpty()) throw Exception("Manager was not built yet!")
        return itemsList.none { it.isRecommended() && !it.isVisited }
    }

    fun buildMap(mapFragment: SupportMapFragment) {
        mapFragment.getMapAsync(mapManager)
    }

    private fun beginJourney() {
        startTime = ParseTime.getCurrentTime()
        sharedPreferences.saveStartTime(startTime!!)
        isJourneyFinishedFlag.value = false
        if (!getRecommendedRoute()) {
            completeJourney()
            return
        }
        sortItemList()
        setNextRecommendedDestination()
        isJourneyBegan.value = true
        isGoToNextItem.value = true
    }

    fun recoverCurrentState() {
        if(isItemsAndRatingsLoaded.value!! && isPreferencesSet.value!!) {
            lastItem = getFirstItem()
            if(lastItem != null)
                if (isJourneyBegan.value!!) {
                    if (!sharedPreferences.isSavedItemsSynchronized) {
                        recoverSavedRecommendedItems()
                    }
                    sortItemList()
                    if(!isJourneyFinishedFlag.value!!)
                        setNextRecommendedDestination()
                } else
                    beginJourney()
        }
    }

    /***
     * set app state with the information saved from the last time it was used if the user didn't finish the journey.
     * @return User saved
     */
    fun recoverSavedPreferences(): User? {
        if(ApplicationProperties.userNotDefinedYet() || startTime == null) {
            ApplicationProperties.user = sharedPreferences.getUser()
            startTime = sharedPreferences.getStartTime()
            if (!ApplicationProperties.userNotDefinedYet()) {
                isQuestionnaireAnswered = sharedPreferences.getIsQuestionnaireAnswered()
                if (startTime != null){
                    isJourneyBegan.value = true
                    if(!ApplicationProperties.isThereTimeAvailableYet(startTime!!))
                        completeJourney("Acabou o tempo da visita que você começou anteriormente. Para começar uma nova visita, primeiro, " +
                                "por favor, responda a rápida pesquisa de satisfação a seguir, caso ainda não tenha feito, e então vá na opção \"Recomeçar\" no menu.")
                }
                isPreferencesSetTrue()
            }
        }else{
            isJourneyBegan.value = true
            isPreferencesSetTrue()
        }
        return ApplicationProperties.user
    }

    private fun recoverSavedRecommendedItems() {
        sharedPreferences.getAllRecommendedItemStatus().let {list->
            if (list.isNotEmpty()) {
                list.forEach{recommendedItem ->
                    val item : Itemizable?
                    if(recommendedItem is RoutableItem) {
                        item = itemsList.find{it.id == recommendedItem.id}
                        item?.recommendedOrder = recommendedItem.recommendedOrder
                    }else item = ItemRepository.subItemList.find{it.id == recommendedItem.id}
                    item?.isVisited= recommendedItem.isVisited

                }
                lastItem = getFirstItem()
                //?: itemsList.filter { it.isVisited }.sortedWith(compareByDescending<Itemizable>{ (it as RoutableItem).recommendedOrder}).get(0)
            }
        }
    }

    private fun getRecommendedRoute(): Boolean {
        if(recommendedRouteBuilder == null || lastItem == null) throw Exception("previous point is null. Did you instantiate RecommendedRouteBuilder?")

        ItemRepository.resetRecommendedOrder()
        val itemsRemaining = recommendedRouteBuilder?.getRecommendedRouteFrom(lastItem!!,
                120.0,
                0.0)
        if(itemsRemaining == null || itemsRemaining.isEmpty()) {
            Log.w(TAG, "No time available for visiting any item.")
            return false
        }
        sharedPreferences.setAllRecommendedItems(itemsList.filter { it.recommendedOrder != Int.MAX_VALUE }, subItemList.filter { it.isRecommended })
        return true
    }

    fun removeItemFromRoute(itemToBeRemoved: Item, itemRemovedConfirmationCallback: () -> Unit) {
        recommendedRouteBuilder?.removeItemFromRoute(itemToBeRemoved)
                ?: Log.e(TAG, "removeItemFromRoute - recommendedRouteBuilder is null!")
        sharedPreferences.removeItem(itemToBeRemoved)
//                ?: Log.e(TAG, "removeItemFromRoute - sharedPreferences is null")
        sortItemList()
        setNextRecommendedDestination()
        itemRemovedConfirmationCallback.invoke()
    }


    fun addItemToRoute(item: Item, itemAddedConfirmationCallback: () -> Unit) {
        if(recommendedRouteBuilder?.addItemToRoute(item, itemsList.filter { !it.isVisited && it.isRecommended()}) != true)
            Log.e(TAG, "addItemToRoute - recommendedRouteBuilder is null or function couldn't add item!")
        else {
            sharedPreferences.setAllRecommendedItems(itemsList.filter { it.recommendedOrder != Int.MAX_VALUE }, subItemList.filter { it.isRecommended })
            sortItemList()
            setNextRecommendedDestination()
            itemAddedConfirmationCallback.invoke()
        }
    }

    fun setNextRecommendedDestination() {
        if(isMapLoaded) {
            if (lastItem != null) {
                nextItem = null
                if (!itemsList[0].isVisited && itemsList[0].recommendedOrder != Int.MAX_VALUE)
                    nextItem = itemsList[0]

                if (nextItem != null) {
                    if (lastItem!!.isUserPoint())
                        recommendedRouteBuilder?.findAndSetShortestPathFromUserLocation(nextItem!!, lastItem!!)
                    else recommendedRouteBuilder?.findAndSetShortestPath(nextItem!!, lastItem!!)

                    try {
                        mapManager?.setDestination(nextItem!!, lastItem, userLocationManager?.userLastKnownLocation?.let {
                                LatLng(it.latitude, it.longitude) })
                        isGoToNextItem.value = true
                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                        //                Toast.makeText(applicationContext, "Erro ao carregar posição.", Toast.LENGTH_SHORT)
                    }


                } else {
                    if(isNoItemsOnRoute())
                        completeJourney()
                    Log.w(TAG, "Todos os itens já foram visitados e setNextRecommendedDestination foi chamado.")
                }
////            Toast.makeText(applicationContext, "Todos os itens já foram visitados.", Toast.LENGTH_LONG).show()
            }
            else
                Log.e(TAG, "Último ponto visitado não foi identificado!")
        }else whenMapLoadedSetDestination = true
    }

    fun createLocationRequest() {
        userLocationManager?.createLocationRequest()
    }

    fun getPreferencesResult(data: Intent) {
        (data.getSerializableExtra("featureRatings") as List<*>).forEach {
            ratingsList.add(it as Rating)
        }
        ApplicationProperties.user?.let { sharedPreferences.saveUser(it) }
        updateRecommender()
       // getRecommendedRoute()
        sortItemList()
        if (!isPreferencesSet.value!!) {
            isPreferencesSetTrue()
        }
    }

    //TODO time is up warning if trying to add new item to visit or just route to it
    fun itemRatingChangeResult(data: Intent) {
        val ratingChangedItemId : String? = data.getStringExtra(ApplicationProperties.TAG_RATING_CHANGED_ITEM_ID)
        val goToNextItem : Boolean = data.getBooleanExtra(ApplicationProperties.TAG_GO_NEXT_ITEM, false)
        val arrived : Boolean = data.getBooleanExtra(ApplicationProperties.TAG_ARRIVED, false)
        @Suppress("UNCHECKED_CAST") val visitedSubItems : List<String>? =  data.getSerializableExtra(ApplicationProperties.TAG_VISITED_SUBITEMS)?.let { it as List<String> }
        if(arrived)
            visitedSubItems?.let { sharedPreferences.setVisitedSubItems(it) }
        if(ratingChangedItemId != null) {
            updateRecommender()
            isRatingChanged = true
        }

        if(goToNextItem) {
            lastItem = nextItem
            isGoToNextItem.value = true
            isCloseToItem.value = false
            (lastItem as Item).isVisited = true

            sharedPreferences.setRecommendedItem(lastItem as Item)

            if (ratingChangedItemId != null)
                if(!getRecommendedRoute()){
                    completeJourney()
                    return
                }
            sortItemList()
            setNextRecommendedDestination()
        }
        else
            if(ratingChangedItemId != null) {
                if(isJourneyBegan.value!! && ratingChangedItemId != nextItem?.id){//rating changed from an item that is not the destination item
                    if(!getRecommendedRoute()){
                        completeJourney()
                        return
                    }
                    sortItemList()
                    setNextRecommendedDestination()
                }
            }
    }

    fun routeToItem(data: Intent) {
        //true when clicked on "Ir para esta atração" on scheduled item details
        val goToThis : Boolean = data.getBooleanExtra(MainActivity.TAG_GO_TO_THIS, false)
        val itemId : String? = data.getStringExtra(MainActivity.TAG_ITEM_ID)
        if(goToThis){
            itemsList.find {it.id == itemId}?.let {
                itemsList.remove(it)
                itemsList.add(0, it)
                it.recommendedOrder = RecommendedRouteBuilder.FIRST_ITEM_FROM_RECOMMENDED_ROUTE-1
                setNextRecommendedDestination()
            }
        }
    }

    //to go to satisfaction survey
    fun completeJourney(customMessage : String = MESSAGE_FINISH) {
        finishMessage = customMessage
        if(!isQuestionnaireAnswered && customMessage == MESSAGE_FINISH)
            finishMessage += MESSAGE_QUESTIONNAIRE
        itemsList.filter { it.isRecommended() }.forEach { it.setNotRecommended() }
        isJourneyFinishedFlag.value = true
    }

    //user finished using the app -> doesn't want to use the app to further check items. Erase all the configured data
    fun finishUserSession(){
        sharedPreferences.clear()
        ApplicationProperties.resetConfigurations()
        resetConfigurations()
        isPreferencesSet.value = false
    }

    fun restartJourney(){
        sharedPreferences.resetJourney()
        resetConfigurations()
    }

    private fun resetConfigurations(){
        ItemRepository.resetJourney()
        mapManager?.clearMap()

        isJourneyBegan.value = false
        isJourneyFinishedFlag.value = false
        isGoToNextItem.value = false
        isCloseToItem.value = false
        showNextItem_okPressed = false
    }

    fun focusOnUserPosition() {
        userLocationManager?.userLastKnownLocation?.let {
            mapManager?.goToLocation(it)
//            mapManager?.setDestination(nextItem!!, lastItem, LatLng(userLastKnowLocation.latitude, userLastKnowLocation.longitude))
            try {
                mapManager?.setDestination(nextItem!!, lastItem, userLocationManager?.userLastKnownLocation?.let {
                    LatLng(it.latitude, it.longitude) })
                if(isFirstTime) isFirstTime = false
                else isGoToNextItem.value = true
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                //                Toast.makeText(applicationContext, "Erro ao carregar posição.", Toast.LENGTH_SHORT)
            }
        }

    }

    fun setOnDestroyActivityState() {
        isMapLoaded = false
    }

    fun nextItemCardShowedWithRatingChangeWarning() {
        isRatingChanged = false
    }

    fun setQuestionnaireAnswered(){
        isQuestionnaireAnswered = true
        sharedPreferences.setIsQuestionnaireAnswered(true)
    }

    fun saveRatingsTrainingTimes(callback: (string : String) -> Unit) {
        ratingsList.forEach {
            it.recommendationRating = -1f
        }
        var auxRatingList = ArrayList<Rating>()
        auxRatingList.addAll(ratingsList)
        auxRatingList= auxRatingList.filter { it.user != "926fa314-8163-4a68-9b02-6cc2d3fad335"
                && it.user != "8c392241-a621-4080-9fbc-81d7665f4f04" } as ArrayList<Rating>
        sortRatingListByDate(auxRatingList)

        val originalRatingList = ArrayList<Rating>()
        originalRatingList.addAll(auxRatingList)

        val fileString = StringBuilder()
        fileString.append("i;User;Item;Rating;Recommendation System;App Version;User Lat;User Lng;Date;Type;" +
                                "User PCC 4 Time;Total Time to Execute;" +"\n")
        var userString = ""
        for(i in 1 until 400){
            userString = (i).toString()
            auxRatingList.add(Rating(userString, "planetaTerra", 3.0f))
            auxRatingList.add(Rating(userString, "planetaUrano", 3.0f))
            auxRatingList.add(Rating(userString, "planetaNetuno", 3.0f))
            auxRatingList.add(Rating(userString, "planetaSaturno", 3.0f))
            auxRatingList.add(Rating(userString, "planetaMarte", 3.0f))
            auxRatingList.add(Rating(userString, "planetaJupiter", 3.0f))
            auxRatingList.add(Rating(userString, "planetaMercurio", 3.0f))
            auxRatingList.add(Rating(userString, "planetaVenus", 3.0f))
            auxRatingList.add(Rating(userString, "sol", 3.0f))
            auxRatingList.add(Rating(userString, "111111", 3.0f))
            auxRatingList.add(Rating(userString, "222222", 3.0f))
        }

        for(i in 0 until 15) {
                ratingsList = auxRatingList.subList(0, i*370 + 2).toHashSet()
                val rating = ratingsList.last()
            for(j in 0 until 5) {

                fileString.append("${i*370+2};${rating.user};${rating.item};${rating.rating};${rating.recommendationSystem};" +
                        "${rating.appVersion};${rating.userLat};${rating.userLng};" +
                        "${ParseTime.toString(rating.date)};${rating.type};"
                        +
                        trainAndGetRoute()
                        + "\n")
            }
        }
        ratingsList = originalRatingList.toHashSet()
        saveCSVStringOnStorage("training_route_finding_ratings", fileString)
    }

    private fun trainAndGetRoute(): String {
        val start = SystemClock.elapsedRealtime()
        updateRecommender()
        val midTime = SystemClock.elapsedRealtime()
        if(!getRecommendedRoute()){
            completeJourney()
            return ";;"
        }
        sortItemList()
        setNextRecommendedDestination()
        val endTime = SystemClock.elapsedRealtime()
        return "${(midTime - start)/1000.0};${(endTime - start)/1000.0};"
     //   return "${rating.recommendationRating};${(1)/1000.0};"
    }

    private fun saveCSVStringOnStorage(recommenderMethod: String, fileString: StringBuilder) {
        val storage = FirebaseStorage.getInstance()
        val storageRef = storage.reference

        val folder = "CSV_TimeNeededForTrainingAndRouting"
        val fileToBeUploaded = storageRef.child(folder+"/"+ "$recommenderMethod-${ParseTime.toFileName(ParseTime.getCurrentTime())}.csv")

        val uploadTask = fileToBeUploaded.putBytes(fileString.toString().toByteArray())
        uploadTask.addOnFailureListener {
            // Handle unsuccessful uploads
        }.addOnSuccessListener {
            // taskSnapshot.metadata contains file metadata such as size, content-type, etc.
            // ...
            Log.w("INFO", "file with times created!")
        }
    }


    fun sortRatingListByDate(list : ArrayList<Rating>){
        var i=0
        list.sortedWith(compareBy<Rating>{it.date}.thenBy{ it.user}.thenBy{ it.item}).forEach{
            list[i++] = it
        }
    }

    fun saveItemsTrainingTimes(isSubitem : Boolean = false, callback: (string : String) -> Unit) {
        val originalItemsList = ArrayList<Item>()
        originalItemsList.addAll(itemsList)

        val originalSubItemsList = ArrayList<SubItem>()
        originalSubItemsList.addAll(subItemList)

        val originalRatingList = HashSet<Rating>()
        originalRatingList.addAll(ratingsList)

        val userString = "fulano"
        val auxItemsList = ArrayList<Itemizable>()
        var itemString = ""
        val planeta = originalItemsList.find { it.id == "planetaUrano" }
        for(i in 1..1000) {
            itemString = i.toString()
            val newItem : Itemizable
            if(isSubitem)
                newItem = SubItem(itemString,"laboratorioFisica", false, itemString,"")
            else{
                newItem = Item(itemString,"", itemString,"","",-23.0,-46.0)
                (newItem.adjacentPoints as HashMap)[planeta!!.id] = 2.0
            }
            auxItemsList.add(newItem)
        }

        val x = 40
        val fileString = StringBuilder()
        fileString.append("i;Item;User PCC 4 Time;Total Time to Execute;" +"\n")
        for(i in 0 until 15) {
            itemsList.clear()
            itemsList.addAll(originalItemsList)
            if(i>0) {
                if(!isSubitem) {
                    for (k in 1 until x+1) {
                        (planeta!!.adjacentPoints as HashMap)[((i - 1) * x + k).toString()] = 2.0
                        ratingsList.add(Rating(userString, ((i - 1) * x + k).toString(), 3.0f))
                    }
                    recommendedRouteBuilder!!.museumGraph.vertices.addAll(auxItemsList.subList((i-1)*x, i*x).toList() as List<Point>)
                    recommendedRouteBuilder!!.allItems.addAll(auxItemsList.subList((i-1)*x, i*x).toList())
                    itemsList.addAll(auxItemsList.subList(0, i * x).toList() as ArrayList<Item>)
                }
                else {
                    for (k in 1 until x+1) {
                        ratingsList.add(Rating(userString, ((i - 1) * x + k).toString(), 3.0f))
                    }
                    subItemList.addAll(auxItemsList.subList((i - 1) * x, i * x).toList() as List<SubItem>)
                    recommendedRouteBuilder!!.allItems.addAll(auxItemsList.subList((i - 1) * x, i * x).toList())
                }

            }
            var item : Itemizable =
                    if(isSubitem)
                        subItemList.last()
                    else itemsList.last()

            for(j in 0 until 5) {
                fileString.append("${i*x+originalItemsList.size};${item.id};" +
                        trainAndGetRoute()
                        + "\n")
//            if(i%30 == 0) {
//                val string : String = "Progress:" + "$i / ${auxRatingList.size}"
//                callback.invoke(string)
//            }
            }
        }
        itemsList = originalItemsList
        subItemList = originalSubItemsList
        ratingsList = originalRatingList
        recommendedRouteBuilder!!.allItems = ItemRepository.allElements.filter { it is Itemizable }.toHashSet() as HashSet<Itemizable>
        if(isSubitem)
            saveCSVStringOnStorage("training_route_finding_subitems", fileString)
        else
            saveCSVStringOnStorage("training_route_finding_items", fileString)
    }

}