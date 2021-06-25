import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.*
import com.pengrad.telegrambot.model.request.*
import fm.force.training.AI
import fm.force.training.Board
import fm.force.training.Cell

enum class CallbackCodes {
    OCCUPIED_CELL,
    MOVE,
}

/**
 * Transform board instance into serialized representation (used in bot callbacks)
 */
fun Board.serialize(): String {
    return "${width}_${strokeToWin}_${state.map { it.ordinal }.joinToString("")}"
}

/**
 * Constructor for Board from serialized representation (used in bot callbacks)
 */
fun Board(representation: String): Board {
    val (width, strokeToWin, stateRepresentation) = representation.split('_')
    val cellValues = Cell.values()
    return Board(
        width = width.toInt(),
        strokeToWin = strokeToWin.toInt(),
        state = stateRepresentation.toCharArray().map { it.digitToInt() }.map { cellValues[it] }.toTypedArray()
    )
}

/**
 * Fabric method for bot keyboard from game board
 */
fun makeKeyboard(board: Board, isLocked: Boolean = false): InlineKeyboardMarkup {
    val keys = board.state.mapIndexed { index, cell ->
        when (cell) {
            Cell.P1 -> InlineKeyboardButton("X").callbackData(CallbackCodes.OCCUPIED_CELL.toString())
            Cell.P2 -> InlineKeyboardButton("O").callbackData(CallbackCodes.OCCUPIED_CELL.toString())
            Cell.EMPTY -> InlineKeyboardButton(" ").callbackData(
                if (isLocked)
                    CallbackCodes.OCCUPIED_CELL.toString()
                else
                    CallbackCodes.MOVE.toString() + '_' + index.toString() + '_' + board.move(index).serialize()
            )
        }
    }

    val kb = InlineKeyboardMarkup()
    keys.chunked(board.width).forEach { kb.addRow(*it.toTypedArray()) }
    return kb
}


class NewGameHandler(bot: TelegramBot, private val aiList: List<AI>) : UpdateHandler(bot) {
    override fun handle(update: Update): Boolean {
        val text = update.message()?.text()
        if (text != null && text.startsWith("/new_game")) {
            // syntax: /new_game_3x3_player1
            val (_, _, dimensions, player) = text.split('_')
            val playerNum = player.last().digitToInt()
            val boardSize = dimensions.first().digitToInt()

            val board = Board(boardSize, boardSize)
            val kb = if (playerNum == 1) {
                // blank board, human moves first
                makeKeyboard(board)
            } else {
                // first move by AI
                val aiMove = aiList.firstOrNull { it.hasMove(board) }?.preferredMove(board)!!
                makeKeyboard(board.move(aiMove))
            }

            sendResponse(update, "Your turn!", kb)
            return true
        }
        return false
    }
}

class NormalMoveHandler(bot: TelegramBot, private val aiList: List<AI>) : UpdateHandler(bot) {
    override fun handle(update: Update): Boolean {
        val query = update.callbackQuery()
        query?.data()
            ?.takeIf { it.startsWith(CallbackCodes.MOVE.toString()) }
            ?.removePrefix(CallbackCodes.MOVE.toString() + "_")
            ?.let { data ->
                val (part1, part2) = data.split('_', limit=2)
                val playerMove = part1.toInt()
                var board = Board(part2)
                val aiMove = aiList.firstOrNull { it.hasMove(board) }?.preferredMove(board)

                aiMove?.let { board = board.move(aiMove) }
                val text = if (aiMove == null) {
                    if (board.isWinningStroke(playerMove))
                        "You win!"
                    else
                        "Game over!"
                } else {
                    if (board.isWinningStroke(aiMove)) {
                        "You lose!"
                    } else if (board.isFull) {
                        "Game over!"
                    } else {
                        "Your turn!"
                    }
                }

                val isLocked = listOf(aiMove, playerMove).firstNotNullOf { it }.let { board.isWinningStroke(it) }

                sendResponse(update, text, makeKeyboard(board, isLocked), edit = true)
                return true
            }
        return false
    }
}

class WrongMoveHandler(bot: TelegramBot) : UpdateHandler(bot) {
    override fun handle(update: Update): Boolean {
        val query = update.callbackQuery()
        query?.data()
            ?.takeIf { it == CallbackCodes.OCCUPIED_CELL.toString() }
            ?.let {
                val template = "Wrong move"
                val originalText = query.message().text()!!
                val text = if (originalText.startsWith(template)) "$originalText!" else template
                sendResponse(update, text, query.message().replyMarkup(), edit = true)
                return true
            }
        return false
    }
}