import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EvaluateModel {

    private static final Logger log = LoggerFactory.getLogger(EvaluateModel.class);

    public static void main(String[] args) {
        String trainingDataPath = "./test_set";  // Путь к обучающим данным
        String modelPath = "./animalModel.zip";  // Путь для сохранения модели
        log.info("Запуск обучения модели...");
        // Создаем объект нейронной сети
        NeuralNetwork neuralNetwork = new NeuralNetwork();

        neuralNetwork.evaluateModel("./2/");
    }
}