package br.gov.interpretaai.platform

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class InterpretaDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        Toast.makeText(context, "Gestão escolar do InterpretaAI ativada", Toast.LENGTH_SHORT).show()
    }
}
