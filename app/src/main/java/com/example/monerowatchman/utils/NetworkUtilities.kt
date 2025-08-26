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
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

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
