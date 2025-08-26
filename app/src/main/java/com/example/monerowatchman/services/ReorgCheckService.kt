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
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class ReorgCheckService : Service() {

    private val channel_id = "ReorgCheckServiceChannel"
	private val block_window = 10
    private var job: Job? = null
    private var reorg_message: String? = null
	
	// block_data[0] will always be the newest block in their respective lists
	private var old_block_data = mutableListOf<BlockDataEntry>()
	private var new_block_data = mutableListOf<BlockDataEntry>()

    override fun onCreate() {
		super.onCreate()
		createNotificationChannel()

		val notification = sendForegroundNotification("Monitoring for chain reorganizations")
		startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

		val node_url = intent?.getStringExtra("node_url") ?: "https://moneronode.org:18081"
        val reorg_check_interval = intent?.getIntExtra("reorg_check_interval", 5) ?: 5
        val reorg_threshold = intent?.getIntExtra("reorg_threshold", 5) ?: 5

        job?.cancel()
        job = CoroutineScope(Dispatchers.IO).launch {

			var get_info_json = sendMoneroRpcRequest(node_url,"get_info")
			val server_status = getJsonValue(get_info_json ?: "fail", "result.status")

			if ( server_status != "OK" ) {
				Log.d("ReorgCheckService", "Failed to connect to server exiting service")
        		sendNotification("Failed to connect to server",1001)
				stopForeground(STOP_FOREGROUND_REMOVE)
				stopSelf()
			} 
            
			while (isActive) {
				// Loops every reorg_check_interval
				
				//TESTING
				//testReorgCheck()

				var get_info_json = sendMoneroRpcRequest(node_url,"get_info")
				val block_height = get_info_json?.let { json -> getJsonValue(json, "result.height")?.toInt()} ?: -1
				val end_block_height = block_height - 1 
				var start_block_height = end_block_height - block_window
				
				val get_block_headers_range_params = """{"start_height": $start_block_height, "end_height": $end_block_height}"""
				var get_block_headers_range_json = sendMoneroRpcRequest(node_url,"get_block_headers_range",get_block_headers_range_params )
				
				new_block_data = parseBlockHeaders("$get_block_headers_range_json")
				
				// Logging
                Log.d("ReorgCheckService", "node_url : $node_url")
                Log.d("ReorgCheckService", "block_height : $block_height")
                Log.d("ReorgCheckService", "reorg_threshold : $reorg_threshold")
                Log.d("ReorgCheckService", new_block_data.toString())
				

				if (old_block_data.isEmpty()) {
					Log.d("ReorgCheckService", "old_block_data is empty not running Reorg check yet")
				} else if (new_block_data.isEmpty())  {
					Log.d("ReorgCheckService", "new_block_data is empty, maybe server is down")
				} else {
					Log.d("ReorgCheckService", "Checking for reorgs")
					reorg_message = checkForReorg(reorg_threshold, old_block_data, new_block_data)
					if (reorg_message != null) {
						Log.d("ReorgCheckService", "$reorg_message")
        				sendNotification("$reorg_message",1002)
					} else {
						Log.d("ReorgCheckService", "No reorg detected")
					}
				}

				old_block_data = new_block_data

                delay(reorg_check_interval * 60_000L)
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channel_id,"Reorg Check Service",NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun sendForegroundNotification(content: String): Notification {
        return NotificationCompat.Builder(this, channel_id)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground) 
            .setOngoing(true)
            .build()
    }

	private fun sendNotification(content: String, notification_id: Int) {
	    val notification = NotificationCompat.Builder(this, channel_id)
	        .setContentText(content)
	        .setSmallIcon(R.drawable.ic_launcher_foreground)
	        .setAutoCancel(true) 
	        .build()
	
	    val manager = getSystemService(NotificationManager::class.java)
	    manager.notify(notification_id, notification) 
	}
}
