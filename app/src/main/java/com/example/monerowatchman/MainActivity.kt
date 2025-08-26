/*
 * This file is part of MoneroWatchman.
 *
 * MoneroWatchman is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MoneroWatchman is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MoneroWatchman.  If not, see https://www.gnu.org/licenses/gpl-3.0.en.html 
 */

package com.example.monerowatchman

// Standard Imports
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.monerowatchman.ui.theme.MoneroWatchmanTheme

// UI/json Imports
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.*
import org.json.JSONObject

// Foreground service
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import android.os.Build

// Get notification permissions
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat

// User Interface
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

class MainActivity : ComponentActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
	    super.onCreate(savedInstanceState)
		
		// Requests notification permissions
	    ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)

		setContent {
			MoneroWatchmanTheme {
				Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
					Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
						
						val default_node_url = "https://moneronode.org:18081"
						val default_reorg_threshold = 1
						val default_reorg_check_interval = 1
						val user_prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
						
						var node_url by remember {mutableStateOf(user_prefs.getString("node_url", default_node_url) ?: default_node_url)}
        				var reorg_threshold by remember {mutableStateOf(user_prefs.getInt("reorg_threshold", 1))}
						var reorg_check_interval = default_reorg_check_interval 


                    	OutlinedTextField(
                    	    value = node_url,
                    	    onValueChange = { node_url = it },
                    	    singleLine = true,
                    	    modifier = Modifier.fillMaxWidth(),
                    	    label = { Text("Node URL") }
                    	)
						
						var reorgText by remember {mutableStateOf(user_prefs.getInt("reorg_threshold", 1).toString())}
						
						OutlinedTextField(
						    value = reorgText,
						    onValueChange = { reorgText = it },
						    label = { Text("Reorg Threshold") },
						    singleLine = true,
						    modifier = Modifier.width(200.dp),
						    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
						)
						
						// Convert to Int when you need it:
						reorg_threshold = reorgText.toIntOrNull() ?: 1

					    Button(onClick = {
							user_prefs.edit().putString("node_url", "$default_node_url").apply()
							user_prefs.edit().putInt("reorg_threshold", default_reorg_threshold).apply()
							node_url = default_node_url
							reorgText = default_reorg_threshold.toString()
						}) {Text("Default Values")}

					    Button(onClick = {
						
							startReorgCheckService("$node_url",reorg_threshold,reorg_check_interval)
							user_prefs.edit().putString("node_url", "$node_url").apply()
							user_prefs.edit().putInt("reorg_threshold", reorg_threshold).apply()

						}) {Text("Run")}

					}
				}
			}
		}
	}

	private fun startReorgCheckService(node_url: String, reorg_threshold: Int, reorg_check_interval: Int) {
		// Get the ReorgCheckService
	    val intent = Intent(this, ReorgCheckService::class.java)
		// Append values to intent that we need to pass to the service
	    intent.putExtra("node_url", node_url)
	    intent.putExtra("reorg_check_interval", reorg_check_interval)
	    intent.putExtra("reorg_threshold", reorg_threshold)
		// Start Service
	    ContextCompat.startForegroundService(this, intent)
	}
}

data class BlockDataEntry(val height: Int, val hash: String)

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
				stopForeground(true)
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

// Regular Functions
fun checkForReorg(reorg_threshold: Int, old_block_data : MutableList<BlockDataEntry>, new_block_data : MutableList<BlockDataEntry>): String? {
	var has_reorg = false
	var reorg_length = 0
	var reorg_start_block = -1
	var	total_indices = new_block_data.indices

	var height: Int = -1 
	var old_height_index: Int = -1 
	var new_height_index: Int = -1 

	// Find starting indexes
	for (i in total_indices) {

		height = new_block_data[i].height
		old_height_index = old_block_data.indexOfFirst { it.height == height }

		if ( old_height_index != -1 ) {
			new_height_index = i 
			break
		} 
	}
	
	if (old_height_index != -1 && new_height_index != -1) {
	    val remaining_old_indices = old_block_data.size - old_height_index
	
	    for (offset in 0 until remaining_old_indices) {
	        val old_block = old_block_data[old_height_index + offset]
	        val new_block = new_block_data[new_height_index + offset]
	
	        // compare them
	        if (old_block.hash != new_block.hash) {
	            has_reorg = true
	            reorg_start_block = old_block.height
	            reorg_length++
	        }
	    }
	} else {
		return "Error calculating relative indicies"
	}

	if ( has_reorg && reorg_length >= reorg_threshold ) {
		return "Reorg Detected of length $reorg_length at $reorg_start_block "
	} else {
		return null
	}
}

fun sendMoneroRpcRequest(node_url: String, method: String, json_params: String = "{}"): String? {
    var response: String? = null

    try {
        val url = URL("$node_url/json_rpc")
        val connection = url.openConnection() as HttpURLConnection

        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true
        connection.connectTimeout = 5000

        val requestBody = """
            {
                "jsonrpc": "2.0",
                "id": "0",
                "method": "$method",
                "params": $json_params
            }
        """.trimIndent()

        OutputStreamWriter(connection.outputStream).use { writer ->
            writer.write(requestBody)
            writer.flush()
        }

        response = connection.inputStream.bufferedReader().use { it.readText() }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return response
}

fun getJsonValue(jsonString: String, keyPath: String): String? {
    var result: String? = null

    try {
        val keys = keyPath.split(".")
        var current: Any = JSONObject(jsonString)

        for (key in keys) {
            if (current is JSONObject && current.has(key)) {
                current = current.get(key)
            } else {
                return null
            }
        }

        result = current.toString()
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return result
}

fun parseBlockHeaders(json_string: String): MutableList<BlockDataEntry> {
    val entries = mutableListOf<BlockDataEntry>()

    try {
        val root = JSONObject(json_string)
        val result = root.getJSONObject("result")
        val headers = result.getJSONArray("headers")

        for (i in 0 until headers.length()) {
            val header = headers.getJSONObject(i)
            val height = header.getInt("height")
            val hash = header.getString("hash")
            entries.add(BlockDataEntry(height, hash))
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return entries
}

fun testReorgCheck() {
    val oldData = mutableListOf(
        BlockDataEntry(99,  "hash99"),
        BlockDataEntry(100, "hash100"),
        BlockDataEntry(101, "hash101"),
        BlockDataEntry(102, "hash102"),
        BlockDataEntry(103, "hash103")
    )

    val newDataNoReorg = mutableListOf(
        BlockDataEntry(100, "hash100"),
        BlockDataEntry(101, "hash101"),
        BlockDataEntry(102, "hash102"),
        BlockDataEntry(103, "hash103"),
        BlockDataEntry(104, "hash104")
    )

    val newDataWithReorg = mutableListOf(
        BlockDataEntry(100, "hash100"),
        BlockDataEntry(101, "DIFFERENT101"),
        BlockDataEntry(102, "DIFFERENT102"),
        BlockDataEntry(103, "hash103"),
        BlockDataEntry(104, "hash104")
    )

    println("Test 1 (No Reorg): " + checkForReorg(1, oldData, newDataNoReorg))
    println("Test 2 (Reorg threshold=1): " + checkForReorg(1, oldData, newDataWithReorg))
    println("Test 3 (Reorg threshold=3): " + checkForReorg(3, oldData, newDataWithReorg))
}

//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//	MoneroWatchmanTheme {
//		Greeting("Android")
//	}
//}
