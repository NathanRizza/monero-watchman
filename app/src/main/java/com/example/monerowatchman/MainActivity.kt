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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle
import android.provider.Settings;
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle 
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.monerowatchman.ui.theme.MoneroWatchmanTheme
import androidx.compose.ui.graphics.Color

class MainActivity : ComponentActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
	    super.onCreate(savedInstanceState)
		
		// Dark mode navigation bar
		window.navigationBarColor = android.graphics.Color.BLACK
		
		// Requests notification permissions
	    ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)

		// Reqest battery optimization exception
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
		    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
		    intent.data = Uri.parse("package:$packageName")
		    startActivity(intent)
		}

		setContent {
			MoneroWatchmanTheme {
				Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
					Column(modifier = Modifier.padding(innerPadding)) {
						
						val ew_modifier =  Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp)
						val ns_modifier =  Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp)

						val	about_text = "This app is designed to alert you when a reorganization occurs on the Monero node you are connected to. \n\n If you find any issues, want to recommended an additional feature or check the source code you can tap “Github Link”. \n\n If you want to help support the project you can send Monero to the “Moreno Donation Address”. \n\n Thank you for using my app!"
						val github_url = "https://github.com/NathanRizza/monero-watchman"
						val monero_donation_address = "86rBr8eqGFbLNgR9VTm6XbdPBFc5hGqMrGjQh1Pv8UVuQRd5oTMRYZHUdQqpJDRRukc3R2EcTWTHq1cjVGiLdSm9EdtVFTu"

						val default_node_url = "https://moneronode.org:18081"
						val default_proxy_url = "127.0.0.1:9050"
						val default_use_proxy = false
						val default_reorg_threshold = 4
						val default_reorg_check_interval = 1
						val user_prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
						
						var node_url by remember {mutableStateOf(user_prefs.getString("node_url", default_node_url) ?: default_node_url)}
        				var reorg_threshold by remember {mutableStateOf(user_prefs.getInt("reorg_threshold", 4))}
						var reorg_check_interval = default_reorg_check_interval 
						var proxy_url by remember {mutableStateOf(user_prefs.getString("proxy_url", default_proxy_url) ?: default_proxy_url)}
						var use_proxy by remember {mutableStateOf(user_prefs.getBoolean("use_proxy", default_use_proxy) ?: default_use_proxy)} 

						var send_launch_alert by remember { mutableStateOf(false) } 
						val alert_text = "Launched Monero Reorg Checker Service"
						
						// Title Section
						Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center,verticalAlignment = Alignment.CenterVertically) {
							Text(text = "Monero Watchman  ",style = TextStyle(fontSize = 24.sp))
							Image(
    						    painter = painterResource(id = R.drawable.ic_launcher_playstore), // Reference your image here
    						    contentDescription = "App Icon",
								modifier = Modifier.size(40.dp)
    						)
						}

						// Reorg Checker Settings
						HorizontalDivider(modifier = ew_modifier,thickness = 2.dp)
						Text("Reorg Checker Settings:",modifier = ns_modifier)

                    	OutlinedTextField(
                    	    value = node_url,
                    	    onValueChange = { node_url = it },
                    	    singleLine = true,
                    	    modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp),
                    	    label = { Text("Node Address") }
                    	)
						
						var reorg_threshold_text by remember {mutableStateOf(user_prefs.getInt("reorg_threshold", 1).toString())}
						
						OutlinedTextField(
						    value = reorg_threshold_text,
						    onValueChange = { reorg_threshold_text = it },
						    label = { Text("Reorg Threshold") },
						    singleLine = true,
						    modifier = Modifier.width(200.dp).padding(start = 8.dp),
						    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
						)
						
						// Convert to int because we can only get strings
						reorg_threshold = reorg_threshold_text.toIntOrNull() ?: 1

						if (reorg_threshold > 50) {
							reorg_threshold = 50
							reorg_threshold_text = "50"
						}
					    
                    	OutlinedTextField(
                    	    value = proxy_url,
                    	    onValueChange = { proxy_url = it },
                    	    singleLine = true,
                    	    modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp),
                    	    label = { Text("Proxy Address") }
                    	)

						// Proxy Section
						Row(modifier = ns_modifier,verticalAlignment = Alignment.CenterVertically) {
							Switch(checked = use_proxy,onCheckedChange = { use_proxy = it }) 
							Text(text = if (use_proxy) "Proxy ON" else "Proxy OFF", modifier = Modifier.padding(start = 16.dp))
						}

						HorizontalDivider(modifier = ew_modifier,thickness = 2.dp)
				        
						// Button Section
						Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
					    	Button(onClick = {

								user_prefs.edit().putString("node_url", "$default_node_url").apply()
								user_prefs.edit().putInt("reorg_threshold", default_reorg_threshold).apply()
								user_prefs.edit().putString("proxy_url", "$default_proxy_url").apply()
								user_prefs.edit().putBoolean("use_proxy", default_use_proxy).apply()

								node_url = default_node_url
								reorg_threshold_text = default_reorg_threshold.toString()
								reorg_threshold = default_reorg_threshold
								proxy_url = default_proxy_url
								use_proxy = default_use_proxy

							}) {Text("Default Values")}

					    	Button(onClick = {
							
								startReorgCheckService(node_url, proxy_url, reorg_threshold, reorg_check_interval, use_proxy)
								user_prefs.edit().putString("node_url", "$node_url").apply()
								user_prefs.edit().putInt("reorg_threshold", reorg_threshold).apply()
								user_prefs.edit().putString("proxy_url", "$proxy_url").apply()
								user_prefs.edit().putBoolean("use_proxy", use_proxy).apply()
								send_launch_alert = true 

							},modifier = Modifier.padding(start = 16.dp)) {Text("Launch")}

						}

						// About / Other Section 
						HorizontalDivider(modifier = ew_modifier,thickness = 2.dp)

						Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
							textAlertDialogBox("About", about_text)
						}
						Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
							textAlertDialogBox("Github Link", github_url)
						}
						Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
							textAlertDialogBox("Monero Donation Address", monero_donation_address)
						}

						HorizontalDivider(modifier = ew_modifier,thickness = 2.dp)
                    	
						// Launch Alert
    					if (send_launch_alert) {
    					    AlertDialog(
    					        onDismissRequest = { send_launch_alert = false },
    					        confirmButton = { TextButton(onClick = { send_launch_alert = false }) {Text("OK")}},
    					        text = {Column {Text(alert_text)}}
    					    )
    					}
					}
				}
			}
		}
	}

	private fun startReorgCheckService(node_url: String, proxy_url: String, reorg_threshold: Int, reorg_check_interval: Int, use_proxy: Boolean) {

	    val intent = Intent(this, ReorgCheckService::class.java)

    	stopService(intent)

	    intent.putExtra("node_url", node_url)
	    intent.putExtra("reorg_threshold", reorg_threshold)
	    intent.putExtra("use_proxy", use_proxy)
	    intent.putExtra("proxy_url", proxy_url)

	    ContextCompat.startForegroundService(this, intent)
	}

}

@Composable
fun textAlertDialogBox(box_text: String, alert_text: String) {

	var show_alert_dialog by remember { mutableStateOf(false) }
	
	Text(
	    text = box_text,
	    modifier = Modifier
	        .clickable { show_alert_dialog = true }
	        .padding(16.dp)
	)
	
	if (show_alert_dialog) {
	    AlertDialog(
	        onDismissRequest = { show_alert_dialog = false },
	        text = {
	            Column {
	                OutlinedTextField(
	                    value = alert_text,
	                    onValueChange = {},
	                    modifier = Modifier.fillMaxWidth(),
	                    readOnly = true
	                )
	            }
	        },
	        confirmButton = {TextButton(onClick = { show_alert_dialog = false }) {Text("Close")}}
	    )
	}
}
