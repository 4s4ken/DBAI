import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;

public class ImageProcessor {

    // Метод для преобразования байтового массива в BufferedImage
    public BufferedImage convertToImage(byte[] imageData) {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(imageData);
            return ImageIO.read(bis);
        } catch (Exception e) {
            System.out.println("Ошибка преобразования изображения: " + e.getMessage());
            return null;
        }
    }

    // Метод для предобработки изображения перед передачей в нейронную сеть
    public BufferedImage preprocessImage(BufferedImage image) {
        // Здесь можно выполнить нормализацию, изменение размера изображения и т.д.
        // Например, изменить размер до 224x224 пикселей:
        BufferedImage resizedImage = new BufferedImage(224, 224, BufferedImage.TYPE_INT_RGB);
        resizedImage.getGraphics().drawImage(image, 0, 0, 224, 224, null);
        return resizedImage;
    }
}