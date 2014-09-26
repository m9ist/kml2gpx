package ru.m9ist;

import java.io.*;

/**
 * Производит запуск конвертации одного формата в другой
 */
public class Launcher {
    public static void main(final String[] args) throws FileNotFoundException {
        System.out.println("Start working...");

        final String fileName = "D:\\Projects\\kml2gpx\\doc.kml";
        final String encoding = "UTF-8";
        System.out.println("Opening file " + fileName);
        final FileInputStream fis = new FileInputStream(fileName);
        final StringBuilder sb = new StringBuilder();
        try {
            @SuppressWarnings("IOResourceOpenedButNotSafelyClosed")
            final BufferedReader br = new BufferedReader(new InputStreamReader(new DataInputStream(fis), encoding));
            while (br.ready())
                sb.append(br.readLine()).append("\n");
        } catch (final Exception ignored) {
            sb.setLength(0);
        } finally {
            try {
                fis.close();
            } catch (final IOException ignored) {
                ignored.printStackTrace();
            }
        }
        if (sb.length() == 0) {
            System.out.println("File is empty or with error!");
            return;
        }
    }
}
