package com.example.monerowatchman

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

// Custom imports
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MoneroWatchmanTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

					val default_url = "https://moneronode.org:18081"
           			var node_url by remember { mutableStateOf("$default_url")}

                    Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
                        StatusTextBox(node_url = node_url)
                        Spacer(modifier = Modifier.height(16.dp))
                        NodeUrlBox(onSubmit = { submittedUrl -> node_url = "$submittedUrl"})
                    }
                }
            }
        }
    }
}

// UI functions
@Composable
fun StatusTextBox(node_url: String, modifier: Modifier = Modifier) {

	var get_info_json by remember { mutableStateOf<String?>("") }
	var server_status by remember { mutableStateOf<String?>("") }
	var block_height by remember { mutableStateOf<String?>("") }
	
	LaunchedEffect(node_url) {
		get_info_json = withContext(Dispatchers.IO) {
			sendMoneroRpcRequest(node_url,"get_info")
		}
		server_status = getJsonValue("$get_info_json","result.status") 
		block_height = getJsonValue("$get_info_json","result.height") 
    }

    
    Text(text = "node_url: $node_url", modifier = modifier)
	Text(text = "server_status: $server_status", modifier = modifier)
    Text(text = "block_height: $block_height", modifier = modifier)
}

@Composable
fun NodeUrlBox( onSubmit: (String) -> Unit, modifier: Modifier = Modifier) {
	
	var nodeUrl by remember { mutableStateOf("") }

    Column(modifier = modifier) {
        TextField(
            value = nodeUrl,
            onValueChange = { nodeUrl = it },
            label = { Text("Monero Node URL") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { onSubmit(nodeUrl) }) {
            Text("Submit")
        }
    }
}

// Regular Functions
fun sendMoneroRpcRequest(rpcUrl: String, method: String, paramsJson: String = "{}"): String? {
    return try {
        val url = URL("$rpcUrl/json_rpc")
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
                "params": $paramsJson
            }
        """.trimIndent()

        OutputStreamWriter(connection.outputStream).use { writer ->
            writer.write(requestBody)
            writer.flush()
        }

        connection.inputStream.bufferedReader().use { it.readText() }

    } catch (e: Exception) {
		null
	}

}

fun getJsonValue(jsonString: String, keyPath: String): String? {
    return try {
        val keys = keyPath.split(".")
        var current: Any = JSONObject(jsonString)

        for (key in keys) {
            current = if (current is JSONObject && current.has(key)) {
                current.get(key)
            } else {
                return null
            }
        }

        current.toString()
    } catch (e: Exception) {
        null
    }
}

//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    MoneroWatchmanTheme {
//        Greeting("Android")
//    }
//}
