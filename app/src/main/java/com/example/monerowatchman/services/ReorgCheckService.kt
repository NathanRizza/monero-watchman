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
    private var job: Job? = null
	
	private var baseline_block_data = mutableListOf<BlockDataEntry>()
	private var comparison_block_data = mutableListOf<BlockDataEntry>()

    override fun onCreate() {
		
		super.onCreate()
		createNotificationChannel()
		val notification = sendForegroundNotification("Monitoring for chain reorganizations")
		startForeground(1, notification)

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        job?.cancel()
        job = CoroutineScope(Dispatchers.IO).launch {

			val node_url = intent?.getStringExtra("node_url") ?: "https://moneronode.org:18081"
        	val reorg_check_interval = intent?.getIntExtra("reorg_check_interval", 5) ?: 5
        	val reorg_threshold = intent?.getIntExtra("reorg_threshold", 5) ?: 5
			val block_window = reorg_threshold + 10
    		var reorg_message: String? = null

			var next_reorg_check_height = -1
			var reorg_check_height = -1

			// TODO replace with some other way to communitcate that you're not connected to the server 
			var get_info_json = sendMoneroRpcRequest(node_url,"get_info")
			val server_status = getJsonValue(get_info_json ?: "fail", "result.status")

			if ( server_status != "OK" ) {
				Log.d("ReorgCheckService", "Failed to connect to server exiting service")
        		sendNotification("Failed to connect to server",1001)
				stopForeground(STOP_FOREGROUND_REMOVE)
				stopSelf()
			} 
            
			while (isActive) {

	            Log.d("ReorgCheckService", "ReorgCheckServiceLoop")

				reorg_check_height = next_reorg_check_height
				
				//TODO add some additonal checks like if the old data has values in it
				if (reorg_check_height != -1){

					val reorg_check_end_block_height = reorg_check_height - 1 
					val reorg_check_start_block_height = reorg_check_height - block_window

					val comparison_get_block_headers_range_params = """{"start_height": $reorg_check_start_block_height, "end_height": $reorg_check_end_block_height}"""
					val comparison_get_block_headers_range_json = sendMoneroRpcRequest(node_url,"get_block_headers_range",comparison_get_block_headers_range_params)
					comparison_block_data = parseBlockHeaders("$comparison_get_block_headers_range_json")

	                Log.d("ReorgCheckService", "node_url : $node_url")
	                Log.d("ReorgCheckService", "reorg_check_height : $reorg_check_height")
	                Log.d("ReorgCheckService", "reorg_threshold : $reorg_threshold")
	                Log.d("ReorgCheckService", "block_window : $block_window")
	                Log.d("ReorgCheckService", "baseline_block_data: $baseline_block_data")
	                Log.d("ReorgCheckService", "comparison_block_data: $comparison_block_data")

					//Check for a reorg
					val reorg_message = checkForReorg(reorg_threshold,baseline_block_data,comparison_block_data)
					if (reorg_message != null) {
						Log.d("ReorgCheckService", "$reorg_message")
		      			sendNotification("$reorg_message",1002)
					} else {
						Log.d("ReorgCheckService", "No reorg detected")
					}
				}
				
				val get_info_json = sendMoneroRpcRequest(node_url,"get_info")

				if (getJsonValue(get_info_json ?: "", "result.status") == "OK") {

					next_reorg_check_height = getJsonValue(get_info_json ?: "", "result.height")?.toIntOrNull() ?: -1
					
					if (next_reorg_check_height != -1) {
						val next_reorg_check_end_block_height = next_reorg_check_height - 1 
						val next_reorg_check_start_block_height = next_reorg_check_height - block_window
						
						val baseline_get_block_headers_range_params = """{"start_height": $next_reorg_check_start_block_height, "end_height": $next_reorg_check_end_block_height}"""
						val baseline_get_block_headers_range_json = sendMoneroRpcRequest(node_url,"get_block_headers_range",baseline_get_block_headers_range_params)
						
						baseline_block_data = parseBlockHeaders("$baseline_get_block_headers_range_json")
	            		Log.d("ReorgCheckService", "Retrieved baseline_block_data for next loop")
					}else {
	                	Log.d("ReorgCheckService", "Failed to get valid block height from server")
					}
				} else {
	                Log.d("ReorgCheckService", "Failed to connect to server")
				}

                delay(reorg_check_interval * 60_000L)
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

	private fun checkForReorg(reorg_threshold: Int, baseline_block_data : MutableList<BlockDataEntry>, comparison_block_data : MutableList<BlockDataEntry>): String? {

		var has_reorg = false
		var reorg_length = 0
		var fork_point = -1
			
		var	total_indices = baseline_block_data.indices

		for (i in total_indices) {
        	if (baseline_block_data[i].hash != comparison_block_data[i].hash) {
				has_reorg = true
				reorg_length++
				fork_point = baseline_block_data[i].height
			}
		}

		if ( has_reorg && reorg_length >= reorg_threshold ) {
			return "Reorg Detected - Length: $reorg_length Fork Point: $fork_point"
		} else {
			return null
		}
	} 

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
