import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.UpdatesListener
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.model.User
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import com.pengrad.telegrambot.request.EditMessageText
import com.pengrad.telegrambot.request.SendMessage
import fm.force.training.AI
import fm.force.training.BoardGraph
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*


abstract class UpdateHandler(
    private val bot: TelegramBot
) {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    abstract fun handle(update: Update): Boolean

    fun sendResponse(update: Update, text: String? = "", keyboard: InlineKeyboardMarkup? = null, edit: Boolean = false) {
        val msg = if (edit) {
            EditMessageText(update.chatId(), update.messageId(), text).replyMarkup(keyboard)
        } else {
            SendMessage(update.chatId(), text).replyMarkup(keyboard)
        }

        val result = bot.execute(msg)
        logger.info("{} {} <- {} ({})", Date(), update.from().verbose(), text, result.description()?: "OK")
    }
}


class Bot(
    token: String,
    trainingDataDir: String
) {
    private val aiList: List<AI> = loadTrainingData(trainingDataDir)
    private val bot = TelegramBot(token)
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val handlers: List<UpdateHandler> = listOf(
        LoggingHandler(bot),
        NewGameHandler(bot, aiList),
        NormalMoveHandler(bot, aiList),
        WrongMoveHandler(bot),
    )

    private fun loadTrainingData(dir: String): List<AI> {
        return File(dir)
            .listFiles()
            ?.map { it.path }
            ?.filter { it.endsWith(".json") }
            ?.map {
                val (_, dimensions, player) = it.removeSuffix(".json").split('_')
                val playerNum = player.last().digitToInt()
                val boardSize = dimensions.first().digitToInt()
                AI(
                    boardGraph = BoardGraph(boardSize, boardSize),
                    player1 = playerNum == 1
                ).apply {
                    loadTrainingResults(it)
                }
            }!!
    }

    fun listen() {
        bot.setUpdatesListener(UpdatesListener { updates ->
            updates.forEach { update -> handlers.takeWhile { !it.handle(update) } }
            return@UpdatesListener UpdatesListener.CONFIRMED_UPDATES_ALL
        })

        logger.info("start listening...")
    }
}

class LoggingHandler(bot: TelegramBot) : UpdateHandler(bot) {

    override fun handle(update: Update): Boolean {
        logger.info("{} {} -> {}", Date(), update.from().verbose(), update.verbose())
        return false
    }
}

fun User.verbose() = "${firstName()} ${lastName()} (${username()}, id=${id()})"

fun Update.from(): User = when {
    message() != null -> message().from()
    callbackQuery() != null -> callbackQuery().from()
    else -> TODO()
}

fun Update.chatId(): Long = when {
    message() != null -> message().chat().id()
    callbackQuery() != null -> callbackQuery().message().chat().id()
    else -> TODO()
}

fun Update.messageId(): Int = when {
    message() != null -> message().messageId()
    callbackQuery() != null -> callbackQuery().message().messageId()
    else -> TODO()
}

fun Update.verbose(): String = when {
    message() != null -> message().text()
    callbackQuery() != null -> "(callback): ${callbackQuery().data()}"
    else -> TODO()
}
