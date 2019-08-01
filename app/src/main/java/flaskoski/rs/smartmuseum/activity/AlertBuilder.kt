package flaskoski.rs.smartmuseum.activity

import android.content.Context
import androidx.appcompat.app.AlertDialog
import flaskoski.rs.smartmuseum.R

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
}