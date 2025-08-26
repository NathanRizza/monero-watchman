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
import org.json.JSONObject

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
