package com.wintercogs.beyonddimensions.client.ui.storage

import com.google.gson.*
import com.mojang.logging.LogUtils
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey
import net.minecraft.client.resources.language.I18n
import net.minecraft.nbt.TagParser
import net.minecraftforge.fml.loading.FMLPaths
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*

/** 存储界面的一个分类标签：符合 [search] 或在 [keys] 里的资源属于这个分类，两者都没有时包含全部资源 */
data class StorageCategory(
    val name: String,
    val icon: IStackKey<*>? = null,
    val search: String = "",
    val keys: List<IStackKey<*>> = emptyList(),
)

/**
 * 分类标签保存在客户端，每个玩家一套，在任何网络里打开存储都能用。
 * 文件是 config/beyonddimensions/storage_categories.json；在游戏线程读写
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
    fun load(player: UUID): List<StorageCategory> =
        loaded.getOrPut(player) {
            val saved = read()?.getAsJsonObject("players")?.get(player.toString())
            if (saved is JsonArray) saved.mapNotNull { decode(it) }
            else listOf(StorageCategory(I18n.get("ui.beyonddimensions.storage.all")))
        }

    /** 改动玩家的分类；写进文件要再调用 [save] */
    fun update(player: UUID, categories: List<StorageCategory>) {
        loaded[player] = categories
    }

    /** 把玩家当前的分类写进文件，其他玩家的部分保持不变 */
    fun save(player: UUID) {
        val categories = loaded[player] ?: return
        val document = read() ?: JsonObject().apply { addProperty("format", FORMAT) }
        val players = document.getAsJsonObject("players") ?: JsonObject().also { document.add("players", it) }
        players.add(player.toString(), JsonArray().apply { categories.forEach { add(encode(it)) } })
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

    private fun encode(category: StorageCategory) =
        JsonObject().apply {
            addProperty("name", category.name)
            category.icon?.let { icon -> encodeKey(icon)?.let { add("icon", it) } }
            addProperty("search", category.search)
            add("keys", JsonArray().apply { category.keys.forEach { key -> encodeKey(key)?.let(::add) } })
        }

    private fun decode(element: JsonElement): StorageCategory? {
        val json = element as? JsonObject ?: return null
        return StorageCategory(
            name = json.get("name")?.asString ?: return null,
            icon = json.get("icon")?.let { decodeKey(it) },
            search = json.get("search")?.asString.orEmpty(),
            keys = (json.get("keys") as? JsonArray)?.mapNotNull { decodeKey(it) }.orEmpty(),
        )
    }

    // 资源以 SNBT 文本保存
    private fun encodeKey(key: IStackKey<*>): JsonElement? = JsonPrimitive(IStackKey.serializeNBTCommon(key).toString())

    // 资源所属的模组被移除后读不出来，跳过即可
    private fun decodeKey(json: JsonElement): IStackKey<*>? =
        try {
            IStackKey.deserializeNBTStrict(TagParser.parseTag(json.asString)).takeUnless { it.isEmpty }
        } catch (error: Exception) {
            null
        }
}
