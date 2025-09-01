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
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URI
import java.util.concurrent.TimeUnit
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

fun sendMoneroRpcRequest(use_proxy: Boolean, proxy_url: String, node_url: String, method: String, json_params: String = "{}"): String? {

    val node_url_json_rpc = "$node_url/json_rpc"
    var response: String? = null
	var client: OkHttpClient? = null

	val request_body = """
	    {
	        "jsonrpc": "2.0",
	        "id": "0",
	        "method": "$method",
	        "params": $json_params
	    }
	""".trimIndent()

	try {
		if (use_proxy) {
			val proxy_parts = proxy_url.split(":")
			val proxy_host = proxy_parts[0]
			val proxy_port = proxy_parts.getOrNull(1)?.toInt() ?: 9050 

        	val proxy_connection = Proxy(Proxy.Type.SOCKS, InetSocketAddress(proxy_host, proxy_port))

        	client = OkHttpClient.Builder().proxy(proxy_connection).build()

		} else {
			client = OkHttpClient.Builder().connectTimeout(3,TimeUnit.SECONDS).build()
		} 
	
	    val request = Request.Builder().url(node_url_json_rpc).post(request_body.toRequestBody("application/json".toMediaTypeOrNull())).build()

	    client.newCall(request).execute().use { response ->
	        if (!response.isSuccessful) {
				// Maybe add a log here
				return null
	        }
    	    return response.body?.string()
		}

	} catch (e: Exception) {
    	e.printStackTrace()
		return null
    }
}
