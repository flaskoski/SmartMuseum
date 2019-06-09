package flaskoski.rs.smartmuseum.routeBuilder
import flaskoski.rs.smartmuseum.routeBuilder.JourneyStates.*

class JourneyManager(private val onStateChangeCallback: OnStateChangeCallback){

    var state : JourneyStates? = null
        set(newState : JourneyStates?){
        val lastState = field
        field = newState
        when (field) {
            PREFERENCES_NOT_SET -> onStateChangeCallback.onStatePreferencesNotSet(lastState)
            BEGIN -> onStateChangeCallback.onStateBegin(lastState)
            NEAR_ITEM -> onStateChangeCallback.onStateNearItem(lastState)
            NEW_ITEM -> onStateChangeCallback.onStateNewItem(lastState)
        }
    }

    interface OnStateChangeCallback{
        fun onStatePreferencesNotSet(lastState: JourneyStates?)
        fun onStateBegin(lastState: JourneyStates?)
        fun onStateNearItem(lastState: JourneyStates?)
        fun onStateNewItem(lastState: JourneyStates?)
    }
}