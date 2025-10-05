/*
 * Copyright (c) 2023-2025 tang shimin
 *
 * This file is part of MuJing.
 *
 * MuJing is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MuJing is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MuJing. If not, see <https://www.gnu.org/licenses/>.
 */

package data

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
/**
 * GitHub 文档地址：https://docs.github.com/cn/rest/releases/releases#get-the-latest-release
 * */
@ExperimentalSerializationApi
@Serializable
data class GitHubRelease(
    val url: String,
    val html_url: String,
    val assets_url: String,
    val upload_url: String,
    val tarball_url: String?,
    val zipball_url: String?,
    val id: Int,
    val node_id: String,
    val tag_name: String,
    val target_commitish: String,
    val name: String?,
    val body: String?,
    val draft: Boolean,
    val prerelease: Boolean,
    val created_at: String,
    val published_at: String?,
    val author: Author,
    val assets: List<Assert>,
)

@ExperimentalSerializationApi
@Serializable
data class Assert(
    val url: String,
    val id: Int,
    val node_id: String,
    val name: String,
    val label: String?,
    val uploader: Author?,
    val content_type: String,
    val state: String,
    val size: Int,
    val download_count: Int,
    val created_at: String,
    val updated_at: String,
    val browser_download_url: String
)

@ExperimentalSerializationApi
@Serializable
data class Author(
    val name:String? = null,
    val email:String? = null,
    val login: String,
    val id: Int,
    val node_id: String,
    val avatar_url: String,
    val gravatar_id: String?,
    val url: String,
    val html_url: String,
    val followers_url: String,
    val following_url: String,
    val gists_url: String,
    val starred_url: String,
    val subscriptions_url: String,
    val organizations_url: String,
    val repos_url: String,
    val events_url: String,
    val received_events_url: String,
    val type: String,
    val site_admin: Boolean,
)