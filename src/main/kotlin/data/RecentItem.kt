package data

import kotlinx.serialization.Serializable

@Serializable
data class RecentItem(val time: String, val name: String, val path: String, val index: Int = 0) {
    override fun equals(other: Any?): Boolean {
        val otherItem = other as RecentItem
        return this.name == otherItem.name && this.path == otherItem.path
    }

    override fun hashCode(): Int {
        return name.hashCode() + path.hashCode()
    }
}