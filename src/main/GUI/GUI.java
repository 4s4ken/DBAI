import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.sql.*;

public class GUI {
    private static Connection connection;
    private static BufferedImage originalImage;
    public static void launch() {
        // Создаем экземпляр DatabaseHandler и подключаемся к базе данных
        DatabaseHandler dbHandler = new DatabaseHandler();
        dbHandler.connect();
        connection = dbHandler.getConnection();


        // Создаем основное окно GUI
        JFrame w1 = new JFrame();
        w1.setSize(1200, 900);
        w1.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JLabel mainMenu = new JLabel(new ImageIcon("./Gui/2.png"), SwingConstants.LEFT );
        JLabel Mlab = new JLabel(new ImageIcon("./Gui/urfu.png"));

        Mlab.setBounds(100, 100, 563, 701);
        w1.add(Mlab);

        mainMenu.setBounds(0, 0, 1200, 900);
        w1.add(mainMenu);

        JButton LoadButton = new JButton(new ImageIcon("./Gui/LoadImage2.png"));
        LoadButton.setBorderPainted(false);
        LoadButton.setBounds(800, 0, 400, 300);
        LoadButton.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                LoadButton.setIcon(new ImageIcon("./Gui/LoadImage1.png"));
            }

            public void mouseExited(MouseEvent e) {
                LoadButton.setIcon(new ImageIcon("./Gui/LoadImage2.png"));
            }

            public void mouseClicked(MouseEvent e) {
                // Создаем объект JFileChooser
                JFileChooser fileChooser = new JFileChooser();

                // Указываем текущую папку проекта как начальную
                fileChooser.setCurrentDirectory(new File(".")); // "." означает текущую директорию

                // Устанавливаем фильтр для изображений
                fileChooser.setFileFilter(new FileNameExtensionFilter(
                        "Image files", "jpg", "png", "jpeg"));

                // Открываем диалог выбора файла
                int returnValue = fileChooser.showOpenDialog(null);

                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    System.out.println("Выбран файл: " + selectedFile.getAbsolutePath());

                    try {
                        byte[] imageData = Files.readAllBytes(selectedFile.toPath());
                        String fileName = selectedFile.getName();
                        // Сохранить исходное изображение
                        originalImage = ImageIO.read(selectedFile);

                        // Добавляем изображение в базу данных
                        try (PreparedStatement stmt = connection.prepareStatement(
                                "INSERT INTO images (name, data) VALUES (?, ?)")) {
                            stmt.setString(1, fileName);
                            stmt.setBytes(2, imageData);
                            stmt.executeUpdate();
                            JOptionPane.showMessageDialog(null, "Изображение успешно загружено в базу данных!");
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(null, "Ошибка при загрузке изображения: " + ex.getMessage());
                    }
                }
            }
        });


        JButton GenerateImage = new JButton(new ImageIcon("./Gui/GenImage2.png"));
        GenerateImage.setBorderPainted(false);
        GenerateImage.setBounds(800, 300, 400, 300);
        GenerateImage.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                GenerateImage.setIcon(new ImageIcon("./Gui/GenImage1.png"));
            }

            public void mouseExited(MouseEvent e) {
                GenerateImage.setIcon(new ImageIcon("./Gui/GenImage2.png"));
            }
            public void mouseClicked(MouseEvent e) {
                NeuralNetwork neuralNetwork = new NeuralNetwork(); // Экземпляр нейросети
                ImageProcessor imageProcessor = new ImageProcessor(); // Экземпляр для обработки изображений
                DatabaseHandler dbHandler = new DatabaseHandler(); // Экземпляр для работы с базой данных

                dbHandler.connect(); // Подключение к базе данных

                // Получаем последний ID из таблицы images
                int imageId = dbHandler.getRowCount("images");

                if (imageId > 0) { // Проверяем, что таблица не пустая
                    byte[] imageData = dbHandler.getImageData(imageId); // Получаем данные изображения

                    if (imageData != null) {
                        // Преобразуем данные изображения в объект BufferedImage
                        BufferedImage image = imageProcessor.convertToImage(imageData);

                        // Предобрабатываем изображение
                        BufferedImage processedImage = imageProcessor.preprocessImage(image);

                        // Генерируем прогноз с помощью нейросети
                        String prediction = neuralNetwork.predict(processedImage);



                        // Создаем окно с выводом изображения и предикта
                        int newWidth = 500;
                        int newHeight = 500;
                        if (originalImage != null) {
                            // Масштабируем изображение
                            Image scaledImage = originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
                            ImageIcon imageIcon = new ImageIcon(scaledImage);

                            // Создаем окно
                            JFrame w2 = new JFrame();
                            w2.setSize(newWidth, newHeight + 60);
                            w2.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

                            // Добавляем JLabel с изображением
                            JLabel imageLabel = new JLabel(imageIcon);
                            w2.add(imageLabel, BorderLayout.NORTH);
                            JLabel predictLabel = new JLabel("На фотографии изображен(а) " + prediction, SwingConstants.CENTER);
                            predictLabel.setFont(new Font("Comic Sans", Font.PLAIN, 14));
                            w2.add(predictLabel, BorderLayout.SOUTH);
                            w2.setVisible(true);

                            // Показываем диалог с вопросом
                            int response = JOptionPane.showConfirmDialog(
                                    w2,
                                    "Верно ли нейросеть сделала прогноз?",
                                    "Подтвердите прогноз",
                                    JOptionPane.YES_NO_OPTION,
                                    JOptionPane.QUESTION_MESSAGE
                            );

                            String finalLabel = prediction; // Изначально метка равна прогнозу
                            if (response == JOptionPane.NO_OPTION) {
                                // Запрос ручного ввода правильной метки
                                finalLabel = JOptionPane.showInputDialog(w2, "Введите правильную метку:");
                                if (finalLabel == null || finalLabel.isEmpty()) {
                                    JOptionPane.showMessageDialog(w2, "Метка не введена. Прогноз не будет сохранён.");
                                    return; // Выход, если метка не введена
                                }
                            }
                            //Сохранения предикта в таблицу
                            dbHandler.savePredictionWithLabel(imageId, prediction, finalLabel);



                            JOptionPane.showMessageDialog(w2, "Метка успешно сохранена!");
                        } else {
                            JOptionPane.showMessageDialog(null, "Ошибка. Вы не загрузили изображение.");
                        }
                    } else {
                        JOptionPane.showMessageDialog(null, "Изображение с ID " + imageId + " не найдено.");
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "В таблице images нет данных.");
                }

                dbHandler.closeConnection(); // Закрываем соединение с базой данных
            }


        });

        JButton StorageImage = new JButton(new ImageIcon("./Gui/StorageImage2.png"));
        StorageImage.setBorderPainted(false);
        StorageImage.setBounds(800, 600, 400, 300);
        StorageImage.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                StorageImage.setIcon(new ImageIcon("./Gui/StorageImage1.png"));
            }

            public void mouseExited(MouseEvent e) {
                StorageImage.setIcon(new ImageIcon("./Gui/StorageImage2.png"));
            }

            public void mouseClicked(MouseEvent e) {
                // Создаем массив с вариантами выбора
                String[] options = {"Images", "Predict"};

                // Показываем диалоговое окно для выбора таблицы
                int choice = JOptionPane.showOptionDialog(
                        null,
                        "Выберите таблицу для отображения:",
                        "Выбор таблицы",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        options,
                        options[0]
                );

                // В зависимости от выбора открываем соответствующую таблицу
                if (choice == 0) {
                    showTable("images");
                } else if (choice == 1) {
                    showTable("predict");
                }
            }

        });

        w1.add(LoadButton);
        w1.add(GenerateImage);
        w1.add(StorageImage);
        w1.setLayout(null);
        w1.setVisible(true);

        // Добавляем shutdown hook для закрытия соединения
        Runtime.getRuntime().addShutdownHook(new Thread(dbHandler::closeConnection));
    }

    private static void showTable(String tableName) {
        JFrame tableFrame = new JFrame("Table: " + tableName);
        tableFrame.setSize(800, 600);
        tableFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JTable table = new JTable();
        JScrollPane scrollPane = new JScrollPane(table);
        tableFrame.add(scrollPane);

        DefaultTableModel model = new DefaultTableModel();
        table.setModel(model);

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName)) {

            // Получаем метаданные и добавляем заголовки в таблицу
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                model.addColumn(metaData.getColumnName(i));
            }

            // Добавляем строки в таблицу
            while (rs.next()) {
                Object[] rowData = new Object[columnCount];
                for (int i = 1; i <= columnCount; i++) {
                    rowData[i - 1] = rs.getObject(i);
                }
                model.addRow(rowData);
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error retrieving table data: " + e.getMessage());
        }

        tableFrame.setVisible(true);
    }








}