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
