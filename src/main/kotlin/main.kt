fun main(args: Array<String>) {

    Bot(
        token = System.getenv("BOT_TOKEN"),
        trainingDataDir = System.getenv("TRAINING_DATA_DIR")
    ).listen()
}