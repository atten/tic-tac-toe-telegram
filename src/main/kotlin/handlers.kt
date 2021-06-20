import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.*

class NewGameHandler(bot: TelegramBot) : UpdateHandler(bot) {
    override fun handle(update: Update): Boolean {
        if (update.message().text() == "/new_game") {
            sendResponse(update, "aaa")
            return true
        }
        return false
    }

}


