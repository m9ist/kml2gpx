package ru.m9ist;

import javax.swing.text.BadLocationException;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * Парсит поток из KML файла
 */
public class HTMLParser extends HTMLEditorKit.ParserCallback {
    public void parseString(final String data) {
        final Reader reader = new StringReader(data);
        final HTMLEditorKit.Parser parser = new ParserDelegator();
        try {
            parser.parse(reader, this, true);
            reader.close();
        } catch (final IOException ignored) {
            ignored.printStackTrace();
        }
    }

    @Override
    public void handleStartTag(final HTML.Tag tag, final MutableAttributeSet attrSet, final int pos) {
        final String tagName = tag.toString();
        int i = 0;
    }

    @Override
    public void handleText(final char[] data, final int pos) {
        int i = 0;
    }

    @Override
    public void handleEndOfLineString(final String data) {
        int i = 0;
    }

    @Override
    public void handleEndTag(final HTML.Tag tag, final int pos) {
        final String tagName = tag.toString();
        int i = 0;
    }

    @Override
    public void flush() throws BadLocationException {
        int i = 0;
    }
}
