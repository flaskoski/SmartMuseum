package flaskoski.rs.smartmuseum.activity

import android.content.Context
import androidx.appcompat.app.AlertDialog
import flaskoski.rs.smartmuseum.R

class AlertBuider {
    fun showNetworkDisconnected(context: Context){
        AlertDialog.Builder(context, R.style.Theme_AppCompat_Dialog_Alert).setTitle("Atenção")
                .setNeutralButton(android.R.string.ok){_,_->}
                .setMessage("Conexão com a internet não encontrada! Por favor verifique sua conexão.".trimMargin())
                .show()
    }
}