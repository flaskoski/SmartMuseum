package flaskoski.rs.smartmuseum

import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import junit.framework.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getTargetContext()

        assertEquals("flaskoski.rs.smartmuseum", appContext.packageName)
    }
}
