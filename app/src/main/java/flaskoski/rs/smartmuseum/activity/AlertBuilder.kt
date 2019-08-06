package flaskoski.rs.smartmuseum.activity

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import flaskoski.rs.smartmuseum.R
import android.view.Gravity
import android.R.attr.gravity
import android.graphics.Color
import android.widget.FrameLayout
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout


class AlertBuilder {
    fun showNetworkDisconnected(context: Context){
        AlertDialog.Builder(context, R.style.Theme_AppCompat_Dialog_Alert).setTitle("Atenção")
                .setNeutralButton(android.R.string.ok){_,_->}
                .setMessage("Conexão com a internet não encontrada! Por favor verifique sua conexão.".trimMargin())
                .show()
    }

    fun showUpdateRequired(context : Context, function: () -> Unit) {
        AlertDialog.Builder(context, R.style.Theme_AppCompat_Dialog_Alert).setTitle("Atenção")
                .setNeutralButton(android.R.string.ok){_,_-> }
                .setMessage("Uma nova versão do aplicativo está disponível. Para continuar, por favor, baixa a nova versão na Play Store.".trimMargin())
                .setOnDismissListener { function() }
                .show()
    }

    fun showUpdateAvailable(context: Context) {
        AlertDialog.Builder(context, R.style.Theme_AppCompat_Dialog_Alert).setTitle("Atenção")
                .setNeutralButton(android.R.string.ok){_,_->}
                .setMessage("Uma nova versão do aplicativo está disponível.".trimMargin())
                .show()
    }

    fun buildNetworkUnavailableWarning(anchor : View, forever : Boolean, atTop : Boolean): Snackbar {
        val internetConnectionWarning = Snackbar.make(anchor, "Conexão com a internet não encontrada! Por favor, verifique sua conexão antes de continuar.",
                if(forever) Snackbar.LENGTH_INDEFINITE
                else Snackbar.LENGTH_LONG)
        val view = internetConnectionWarning.view
        view.setBackgroundColor(Color.parseColor("#F14242"))
        if(atTop) {
            val params = view.layoutParams as CoordinatorLayout.LayoutParams
            params.gravity = Gravity.TOP
            view.layoutParams = params
        }
        return internetConnectionWarning
    }
}