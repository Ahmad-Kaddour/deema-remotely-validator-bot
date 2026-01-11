package org.example

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import dev.kord.rest.builder.message.create.stickerId
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale

val keywords = listOf("ريموت", "ريموتلي", "remotely", "remote", "+1", "و انا", "remotly", "remot")

@Serializable
data class UserWeekData(
    val userId: String,
    val week: Int
)

@Serializable
data class StorageData(
    val userWeeklyMessages: List<UserWeekData>
)

val storageFile = File("bot_data.json")
val userWeeklyMessages = mutableMapOf<Snowflake, Int>()

fun loadData() {
    if (storageFile.exists()) {
        try {
            val json = storageFile.readText()
            val data = Json.decodeFromString<StorageData>(json)
            data.userWeeklyMessages.forEach {
                userWeeklyMessages[Snowflake(it.userId)] = it.week
            }
        } catch (e: Exception) {
            println("Error loading data: ${e.message}")
        }
    }
}

fun saveData() {
    try {
        val data = StorageData(
            userWeeklyMessages = userWeeklyMessages.map {
                UserWeekData(it.key.toString(), it.value)
            }
        )
        val json = Json.encodeToString(data)
        storageFile.writeText(json)
    } catch (e: Exception) {
        println("Error saving data: ${e.message}")
    }
}

fun currentWeek(): Int {
    val now = LocalDate.now()
    return now.get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear())
}

@OptIn(PrivilegedIntent::class)
fun main() = runBlocking {
    loadData()
    val token = System.getenv("DISCORD_BOT_TOKEN")
        ?: error("DISCORD_BOT_TOKEN is not set")
    val kord = Kord(token)

    kord.on<MessageCreateEvent> {
        val message = message
        val author = message.author ?: return@on
        if (author.isBot) return@on

        val content = message.content.lowercase()
        val userId = author.id
        val week = currentWeek()

        if (keywords.any { it.lowercase() in content }) {
            val alreadyThisWeek = userWeeklyMessages[userId] == week
            userWeeklyMessages[userId] = week
            saveData()

            if (alreadyThisWeek) {
                message.channel.createMessage {
                    stickerId(Snowflake("1459956550260097300"))
                    messageReference = message.id
                }
            } else {
                message.channel.createMessage {
                    stickerId(Snowflake("1454915613846798548"))
                    messageReference = message.id
                }
            }
        }
    }

    kord.login() {
        intents += Intent.GuildMessages
        intents += Intent.MessageContent
    }
}