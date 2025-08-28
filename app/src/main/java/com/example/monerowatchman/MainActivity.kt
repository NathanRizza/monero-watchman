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

// Bring all the other files in this package
import com.example.monerowatchman.*

// Dependencies 
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.monerowatchman.ui.theme.MoneroWatchmanTheme
import androidx.compose.material3.Switch

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
						var use_proxy by remember { mutableStateOf(false) } 

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
						
							startReorgCheckService("$node_url",reorg_threshold,reorg_check_interval,use_proxy)
							user_prefs.edit().putString("node_url", "$node_url").apply()
							user_prefs.edit().putInt("reorg_threshold", reorg_threshold).apply()

						}) {Text("Run")}
					
						Switch(checked = use_proxy,onCheckedChange = { use_proxy = it })
				        Text(text = if (use_proxy) "Proxy ON" else "Proxy OFF")

						//Text("Until further updates, after pressing run wait for the notification to pop up to confirm service is running.")
					
					}
				}
			}
		}
	}

	private fun startReorgCheckService(node_url: String, reorg_threshold: Int, reorg_check_interval: Int, use_proxy: Boolean) {
		// Get the ReorgCheckService
	    val intent = Intent(this, ReorgCheckService::class.java)
		// Append values to intent that we need to pass to the service
	    intent.putExtra("node_url", node_url)
	    intent.putExtra("reorg_check_interval", reorg_check_interval)
	    intent.putExtra("reorg_threshold", reorg_threshold)
	    intent.putExtra("use_proxy", use_proxy)
		// Start Service
	    ContextCompat.startForegroundService(this, intent)
	}
}

