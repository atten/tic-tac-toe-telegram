import com.pengrad.telegrambot.*
import com.pengrad.telegrambot.model.*
import com.pengrad.telegrambot.model.request.Keyboard
import com.pengrad.telegrambot.request.SendMessage
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*


abstract class UpdateHandler(
    val bot: TelegramBot
) {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    abstract fun handle(update: Update): Boolean

    fun sendResponse(update: Update, text: String? = "", keyboard: Keyboard? = null) {
        val msg = SendMessage(update.message().chat().id(), text)
        keyboard?.let { msg.replyMarkup(it) }
        bot.execute(msg)
        logger.info("{} {} <- {}", Date(), update.message().from().verbose(), text)
    }
}


class Bot(
    token: String
) {
    private val bot = TelegramBot(token)
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val handlers: List<UpdateHandler> = listOf(
        LoggingHandler(bot),
        NewGameHandler(bot)
    )

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
//        println(update)
        logger.info("{} {} -> {}", Date(), update.message().from().verbose(), update.verbose())
        return false
    }
}

fun User.verbose() = "${firstName()} ${lastName()} (${username()}, id=${id()})"
fun Update.verbose(): String = message().text()
