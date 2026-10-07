package com.wintercogs.beyonddimensions.client.ui.storage

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.mojang.logging.LogUtils
import com.mojang.serialization.JsonOps
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey
import net.minecraft.client.resources.language.I18n
import net.minecraft.core.HolderLookup
import net.minecraft.resources.RegistryOps
import net.neoforged.fml.loading.FMLPaths
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID

/**
 * 存储界面的一个分类标签。规则是搜索式与特定补充：符合 [search] 或在 [keys] 里的资源属于这个分类；
 * 两者都没有时包含全部资源，例如默认的"全部"。
 */
data class StorageCategory(
    val name: String,
    val icon: IStackKey<*>? = null,
    val search: String = "",
    val keys: List<IStackKey<*>> = emptyList(),
)

/**
 * 分类标签保存在客户端，跟随玩家：每个玩家一套，在任何网络里打开存储都能用。
 * 文件是 config/beyonddimensions/storage_categories.json，按玩家 UUID 分开；在游戏线程读写
 */
object StorageCategories {
    private const val FORMAT = 1
    private val logger = LogUtils.getLogger()
    private val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
    private val file
        get() = FMLPaths.CONFIGDIR.get().resolve("beyonddimensions/storage_categories.json")

    private val loaded = HashMap<UUID, List<StorageCategory>>()

    /** 本次游戏中各玩家最后打开的分类标签 */
    val selected = HashMap<UUID, Int>()

    /** 玩家的分类标签；还没有时只有一个"全部" */
    fun load(player: UUID, registries: HolderLookup.Provider): List<StorageCategory> =
        loaded.getOrPut(player) {
            val saved = read()?.getAsJsonObject("players")?.get(player.toString())
            if (saved is JsonArray) saved.mapNotNull { decode(it, registries) }
            else listOf(StorageCategory(I18n.get("ui.beyonddimensions.storage.all")))
        }

    fun save(player: UUID, categories: List<StorageCategory>, registries: HolderLookup.Provider) {
        loaded[player] = categories
        val document = read() ?: JsonObject().apply { addProperty("format", FORMAT) }
        val players = document.getAsJsonObject("players") ?: JsonObject().also { document.add("players", it) }
        players.add(player.toString(), JsonArray().apply { categories.forEach { add(encode(it, registries)) } })
        try {
            val path = file
            Files.createDirectories(path.parent)
            val temporary = path.resolveSibling("${path.fileName}.tmp")
            Files.writeString(temporary, gson.toJson(document))
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (error: Exception) {
            logger.warn("Cannot save storage categories", error)
        }
    }

    private fun read(): JsonObject? {
        val path = file
        if (!Files.exists(path)) return null
        return try {
            val root = Files.newBufferedReader(path).use(JsonParser::parseReader)
            root.asJsonObject.takeIf { it.get("format")?.asInt == FORMAT }
        } catch (error: Exception) {
            logger.warn("Cannot read storage categories", error)
            null
        }
    }

    private fun encode(category: StorageCategory, registries: HolderLookup.Provider) =
        JsonObject().apply {
            addProperty("name", category.name)
            category.icon?.let { icon -> encodeKey(icon, registries)?.let { add("icon", it) } }
            addProperty("search", category.search)
            add("keys", JsonArray().apply { category.keys.forEach { key -> encodeKey(key, registries)?.let(::add) } })
        }

    private fun decode(element: JsonElement, registries: HolderLookup.Provider): StorageCategory? {
        val json = element as? JsonObject ?: return null
        return StorageCategory(
            name = json.get("name")?.asString ?: return null,
            icon = json.get("icon")?.let { decodeKey(it, registries) },
            search = json.get("search")?.asString.orEmpty(),
            keys = (json.get("keys") as? JsonArray)?.mapNotNull { decodeKey(it, registries) }.orEmpty(),
        )
    }

    private fun encodeKey(key: IStackKey<*>, registries: HolderLookup.Provider): JsonElement? =
        IStackKey.CODEC.encodeStart(RegistryOps.create(JsonOps.INSTANCE, registries), key).result().orElse(null)

    // 资源所属的模组被移除后读不出来，跳过即可
    private fun decodeKey(json: JsonElement, registries: HolderLookup.Provider): IStackKey<*>? =
        IStackKey.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, registries), json).result().orElse(null)
            ?.takeUnless { it.isEmpty }
}
