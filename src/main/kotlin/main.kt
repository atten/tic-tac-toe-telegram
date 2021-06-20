fun main(args: Array<String>) {

    val bot = Bot(System.getenv("BOT_TOKEN"))
    bot.listen()
}