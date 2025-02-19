import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Train {

    private static final Logger log = LoggerFactory.getLogger(Train.class);

    public static void main(String[] args) {
        // Убедитесь, что указаны корректные пути к данным и модели
        String trainingDataPath = "./test_set";  // Путь к обучающим данным
        String modelPath = "./animalModel.zip";  // Путь для сохранения модели
        log.info("Запуск обучения модели...");
        // Создаем объект нейронной сети
        NeuralNetwork neuralNetwork = new NeuralNetwork();
        // Обучаем модель
        neuralNetwork.trainModel();
        log.info("Обучение завершено и модель сохранена.");
    }
}