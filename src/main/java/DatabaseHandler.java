import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHandler {
    private Connection connection;

    // Метод подключения к базе данных
    public void connect() {
        try {
            String url = "jdbc:postgresql://localhost:5432/postgres";
            String user = "postgres";
            String password = "postgres";

            connection = DriverManager.getConnection(url, user, password);
            System.out.println("Подключение к базе данных выполнено.");
        } catch (SQLException e) {
            System.err.println("Ошибка подключения: " + e.getMessage());
        }
    }
    // Новый метод для возврата соединения
    public Connection getConnection() {
        return connection;
    }

    public int getRowCount(String tableName) {
        String countQuery = "SELECT COUNT(*) FROM " + tableName;
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(countQuery)) {

            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Ошибка при подсчете строк в таблице: " + e.getMessage());
        }
        return 0;
    }

    // Метод для загрузки изображений из папки в базу данных
    public void uploadImage() {
        try {
            Path currentDir = Paths.get("").toAbsolutePath();
            Path subfolderPath = currentDir.resolve("animal");
            System.out.println("Path to subfolder: " + subfolderPath);

            // Проверяем, что папка существует
            if (!Files.exists(subfolderPath) || !Files.isDirectory(subfolderPath)) {
                System.out.println("Subfolder does not exist or is not a directory.");
                return;
            }

            // Сканируем папку и фильтруем изображения
            List<Path> imageFiles = new ArrayList<>();
            Files.list(subfolderPath)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".jpg") || path.toString().endsWith(".png") || path.toString().endsWith(".jpeg"))
                    .forEach(imageFiles::add);

            System.out.println("Found " + imageFiles.size() + " image(s).");

            String insertQuery = "INSERT INTO images (name, data) VALUES (?, ?)";
            for (Path imagePath : imageFiles) {
                byte[] imageData = Files.readAllBytes(imagePath);
                System.out.println("Loaded image: " + imagePath.getFileName() + " (size: " + imageData.length + " bytes)");

                try (PreparedStatement statement = connection.prepareStatement(insertQuery)) {
                    statement.setString(1, imagePath.getFileName().toString());
                    statement.setBytes(2, imageData);
                    statement.executeUpdate();
                    System.out.println("Image saved to database: " + imagePath.getFileName());
                } catch (SQLException e) {
                    System.err.println("Ошибка сохранения изображения: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Метод сохранения предиктов и меток
    public void savePredictionWithLabel(int imageId, String prediction, String label) {
        String insertQuery = "INSERT INTO Predict (image_id, predict, label) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(insertQuery)) {
            statement.setInt(1, imageId);
            statement.setString(2, prediction);
            statement.setString(3, label);
            statement.executeUpdate();
            System.out.println("Результат предсказания сохранен: image_id=" + imageId + ", predict=" + prediction + ", label=" + label);
        } catch (SQLException e) {
            System.err.println("Ошибка сохранения результата предсказания: " + e.getMessage());
        }
    }

    // Метод для получения изображения из базы данных
    public byte[] getImageData(int imageId) {
        String query = "SELECT data FROM images WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, imageId);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getBytes("data");
            }
        } catch (SQLException e) {
            System.err.println("Ошибка получения изображения: " + e.getMessage());
        }
        return null;
    }

    // Закрытие соединения с базой данных
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Соединение с базой данных закрыто.");
            }
        } catch (SQLException e) {
            System.out.println("Ошибка закрытия соединения: " + e.getMessage());
        }
    }
    public static void main(String[] args) {
        DatabaseHandler dbHandler = new DatabaseHandler();
        dbHandler.connect();          // Подключение к базе данных
        dbHandler.uploadImage();      // Загрузка изображений в базу данных
        dbHandler.closeConnection();  // Закрытие соединения
    }
}
