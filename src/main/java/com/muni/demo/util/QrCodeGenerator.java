package com.muni.demo.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * Utility class to generate QR Codes for shortened URLs.
 */
public final class QrCodeGenerator {

    private QrCodeGenerator() {
        // Prevent instantiation
    }

    /**
     * Generates a QR Code as PNG bytes.
     *
     * @param text   The content to encode in the QR code (typically the short URL).
     * @param width  The width of the QR code image.
     * @param height The height of the QR code image.
     * @return Raw PNG image bytes.
     * @throws WriterException If ZXing fails to generate the code.
     * @throws IOException     If there is an error writing the image to the byte array stream.
     */
    public static byte[] generateQrCodeImage(String text, int width, int height) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);

        try (ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream()) {
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            return pngOutputStream.toByteArray();
        }
    }

    /**
     * Generates a QR Code and encodes it as a Base64 PNG data URI.
     *
     * @param text   The content to encode in the QR code.
     * @param width  The width of the QR code image.
     * @param height The height of the QR code image.
     * @return Base64 data URI string (e.g., "data:image/png;base64,iVBORw0KGgo...").
     * @throws WriterException If ZXing fails to generate the code.
     * @throws IOException     If there is an error writing the image to the byte array stream.
     */
    public static String generateQrCodeBase64(String text, int width, int height) throws WriterException, IOException {
        byte[] imageBytes = generateQrCodeImage(text, width, height);
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        return "data:image/png;base64," + base64Image;
    }
}
