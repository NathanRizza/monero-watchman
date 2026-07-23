/*
This file is apart of Monerowatch, a Monero blockchain observing tool.
Copyright (C) 2025 Nathaniel Rizza

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

// Include this file in this package
package com.example.monerowatchman

// Dependencies
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {

        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return

        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

        if (!prefs.getBoolean("start_on_boot", false)) {
            Log.d("BootReceiver", "start_on_boot disabled, not starting service")
            return
        }

        val service_intent = Intent(context, ReorgCheckService::class.java).apply {
            putExtra("node_url", prefs.getString("node_url", "https://xmrnode.shork.ch"))
            putExtra("reorg_threshold", prefs.getInt("reorg_threshold", 3))
            putExtra("use_proxy", prefs.getBoolean("use_proxy", false))
            putExtra("proxy_url", prefs.getString("proxy_url", "127.0.0.1:9050"))
        }

        try {
            ContextCompat.startForegroundService(context, service_intent)
            Log.d("BootReceiver", "Started ReorgCheckService on boot")
        } catch (e: Exception) {
            Log.e("BootReceiver", "Failed to start service on boot", e)
        }
    }
}
