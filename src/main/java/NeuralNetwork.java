import org.datavec.api.io.labels.ParentPathLabelGenerator;
import org.datavec.api.split.FileSplit;
import org.datavec.image.loader.NativeImageLoader;
import org.datavec.image.recordreader.ImageRecordReader;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.layers.ConvolutionLayer;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.conf.layers.SubsamplingLayer;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.util.ModelSerializer;
//import org.deeplearning4j.nn.api.Optimizer;
import org.deeplearning4j.nn.conf.Updater;
import org.nd4j.evaluation.classification.Evaluation;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

public class NeuralNetwork {

    private static final Logger log = LoggerFactory.getLogger(NeuralNetwork.class);

    private MultiLayerNetwork model;
    private final int height = 224;            // Высота изображения
    private final int width = 224;             // Ширина изображения
    private final int channels = 3;            // Количество цветовых каналов (RGB)
    private final int batchSize = 16;          // Размер батча (уменьшен для стабильности)
    private final int numEpochs = 10;         // Количество эпох для обучения
    private final String[] animalLabels = {"cat", "dog", "bird"}; // Метки классов животных
    private final String trainingDataPath = "./test_set"; // Путь к обучающим данным
    private final String modelPath = "./animalModel.zip"; // Путь для сохранения модели

    // Конструктор: загружаем модель, если она существует, или создаём новую
    public NeuralNetwork() {
        File modelFile = new File(modelPath);
        if (modelFile.exists()) {
            try {
                model = ModelSerializer.restoreMultiLayerNetwork(modelFile);
                log.info("Модель загружена из файла.");
            } catch (IOException e) {
                log.error("Ошибка загрузки модели: {}", e.getMessage());
                model = createModel();
            }
        } else {
            model = createModel();
            //log.info("Создана новая модель.");
        }
    }

    // Создание архитектуры модели
    private MultiLayerNetwork createModel() {
        MultiLayerConfiguration config = new org.deeplearning4j.nn.conf.NeuralNetConfiguration.Builder()
                .seed(123)
                .updater(Updater.ADAM)               // Используем Adam для более стабильного обучения
                .updater(new org.nd4j.linalg.learning.config.Sgd(0.01)) // Скорость обучения для оптимизатора SGD
                .list()
                .layer(new ConvolutionLayer.Builder(5, 5)
                        .nIn(channels)
                        .stride(1, 1)
                        .nOut(32)
                        .activation(Activation.RELU)
                        .build())
                .layer(new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX)
                        .kernelSize(2, 2)
                        .stride(2, 2)
                        .build())
                .layer(new ConvolutionLayer.Builder(3, 3)
                        .stride(1, 1)
                        .nOut(64)
                        .activation(Activation.RELU)
                        .build())
                .layer(new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX)
                        .kernelSize(2, 2)
                        .stride(2, 2)
                        .build())
                .layer(new DenseLayer.Builder().nOut(128)
                        .activation(Activation.RELU)
                        .build())
                .layer(new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                        .nOut(animalLabels.length)
                        .activation(Activation.SOFTMAX)
                        .build())
                .setInputType(InputType.convolutionalFlat(height, width, channels))
                .build();

        MultiLayerNetwork newModel = new MultiLayerNetwork(config);
        newModel.init();
        newModel.setListeners(new ScoreIterationListener(10));  // Слушатель для отслеживания ошибок
        return newModel;
    }

    // Обучение модели
    public void trainModel() {
        try {
            File trainData = new File(trainingDataPath);
            FileSplit fileSplit = new FileSplit(trainData, NativeImageLoader.ALLOWED_FORMATS, new Random(123));
            ParentPathLabelGenerator labelMaker = new ParentPathLabelGenerator();
            ImageRecordReader recordReader = new ImageRecordReader(height, width, channels, labelMaker);
            recordReader.initialize(fileSplit);

            RecordReaderDataSetIterator dataIter = new RecordReaderDataSetIterator(recordReader, batchSize, 1, animalLabels.length);
            ImagePreProcessingScaler scaler = new ImagePreProcessingScaler(0, 1);
            dataIter.setPreProcessor(scaler);

            for (int i = 0; i < numEpochs; i++) {
                log.info("Обучение на эпохе {}", i + 1);
                model.fit(dataIter);
                log.info("Эпоха {} завершена", i + 1);

                // Оценка на каждой эпохе
                evaluateModel(trainingDataPath);

                dataIter.reset();  // Сброс данных после каждой эпохи
            }

            saveModel();
        } catch (IOException e) {
            log.error("Ошибка загрузки данных: {}", e.getMessage());
        }
    }

    // Оценка модели
    public void evaluateModel(String testDataPath) {
        try {
            File testData = new File(testDataPath);
            FileSplit fileSplit = new FileSplit(testData, NativeImageLoader.ALLOWED_FORMATS, new Random(123));
            ParentPathLabelGenerator labelMaker = new ParentPathLabelGenerator();
            ImageRecordReader recordReader = new ImageRecordReader(height, width, channels, labelMaker);
            recordReader.initialize(fileSplit);

            RecordReaderDataSetIterator testIter = new RecordReaderDataSetIterator(recordReader, batchSize, 1, animalLabels.length);
            ImagePreProcessingScaler scaler = new ImagePreProcessingScaler(0, 1);
            testIter.setPreProcessor(scaler);

            Evaluation eval = model.evaluate(testIter);
            log.info("Результаты оценки:\n{}", eval.stats());
        } catch (IOException e) {
            log.error("Ошибка оценки модели: {}", e.getMessage());
        }
    }

    // Предсказание на изображении
    public String predict(BufferedImage image) {
        try {
            NativeImageLoader loader = new NativeImageLoader(height, width, channels);
            INDArray imageMatrix = loader.asMatrix(image);

            ImagePreProcessingScaler scaler = new ImagePreProcessingScaler(0, 1);
            scaler.transform(imageMatrix);

            INDArray output = model.output(imageMatrix);
            int predictedClass = Nd4j.argMax(output, 1).getInt(0);

            return animalLabels[predictedClass];
        } catch (IOException e) {
            log.error("Ошибка обработки изображения: {}", e.getMessage());
            return null;
        }
    }

    // Сохранение модели
    private void saveModel() {
        try {
            File modelFile = new File(modelPath);
            modelFile.getParentFile().mkdirs();
            ModelSerializer.writeModel(model, modelFile, true);
            log.info("Модель сохранена в файл.");
        } catch (IOException e) {
            log.error("Ошибка сохранения модели: {}", e.getMessage());
        }
    }
}
