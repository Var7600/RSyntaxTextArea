/*
 * 02/26/2004
 *
 * SyntaxScheme.java - The set of colors and tokens used by an RSyntaxTextArea
 * to color tokens.
 *
 * This library is distributed under a modified BSD license.  See the included
 * LICENSE file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.TextAttribute;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;


/**
 * The set of colors and styles used by an <code>RSyntaxTextArea</code> to
 * color tokens.<p>
 * You can use this class to programmatically set the fonts and colors used in
 * an RSyntaxTextArea, but for more powerful, externalized control, consider
 * using {@link Theme}s instead.
 *
 * @author Robert Futrell
 * @version 1.0
 * @see Theme
 */
@SuppressWarnings("checkstyle:magicnumber")
public class SyntaxScheme implements Cloneable, TokenTypes {

	private Style[] styles;

	private static final String VERSION			= "*ver1";


	/**
	 * Creates a color scheme that either has all color values set to
	 * a default value or set to <code>null</code>.
	 *
	 * @param useDefaults If <code>true</code>, all color values will
	 *        be set to default colors; if <code>false</code>, all colors
	 *        will be initially <code>null</code>.
	 */
	public SyntaxScheme(boolean useDefaults) {
		styles = new Style[DEFAULT_NUM_TOKEN_TYPES];
		if (useDefaults) {
			restoreDefaults(null);
		}
	}


	/**
	 * Creates a default color scheme.
	 *
	 * @param baseFont The base font to use.  Keywords will be a bold version
	 *        of this font, and comments will be an italicized version of this
	 *        font.
	 */
	public SyntaxScheme(Font baseFont) {
		this(baseFont, true);
	}


	/**
	 * Creates a default color scheme.
	 *
	 * @param baseFont The base font to use.  Keywords will be a bold version
	 *        of this font, and comments will be an italicized version of this
	 *        font.
	 * @param fontStyles Whether bold and italic should be used in the scheme
	 *        (vs. all tokens using a plain font).
	 */
	public SyntaxScheme(Font baseFont, boolean fontStyles) {
		styles = new Style[DEFAULT_NUM_TOKEN_TYPES];
		restoreDefaults(baseFont, fontStyles);
	}


	/**
	 * Changes the "base font" for this syntax scheme.  This is called by
	 * <code>RSyntaxTextArea</code> when its font changes via
	 * <code>setFont()</code>.  This looks for tokens that use a derivative of
	 * the text area's old font (but bolded and/or italicized) and makes them
	 * use the new font with those styles instead.  This is desirable because
	 * most programmers prefer a single font to be used in their text editor,
	 * but might want bold (say for keywords) or italics.
	 *
	 * @param oldFont The old font of the text area.
	 * @param newFont The new font of the text area.
	 */
	void changeBaseFont(Font oldFont, Font newFont) {
		for (Style style : styles) {
			if (style != null && style.font != null) {
				if (style.font.getFamily().equals(oldFont.getFamily()) &&
					style.font.getSize2D() == oldFont.getSize2D()) {
					int styleFontStyle = style.font.getStyle(); // Keep bold or italic
					style.font = newFont.deriveFont(styleFontStyle);
				}
			}
		}
	}


	/**
	 * Returns a deep copy of this color scheme.
	 *
	 * @return The copy.
	 */
	@Override
	public Object clone() {
		SyntaxScheme shcs;
		try {
			shcs = (SyntaxScheme)super.clone();
		} catch (CloneNotSupportedException cnse) { // Never happens
			cnse.printStackTrace();
			return null;
		}
		shcs.styles = new Style[styles.length];
		for (int i=0; i<styles.length; i++) {
			Style s = styles[i];
			if (s!=null) {
				shcs.styles[i] = (Style)s.clone();
			}
		}
		return shcs;
	}


	/**
	 * Tests whether this color scheme is the same as another color scheme.
	 *
	 * @param otherScheme The color scheme to compare to.
	 * @return <code>true</code> if this color scheme and
	 *         <code>otherScheme</code> are the same scheme;
	 *         <code>false</code> otherwise.
	 */
	@Override
	public boolean equals(Object otherScheme) {

		// No need for null check; instanceof takes care of this for us,
		// i.e. "if (!(null instanceof Foo))" evaluates to "true".
		if (!(otherScheme instanceof SyntaxScheme)) {
			return false;
		}

		Style[] otherSchemes = ((SyntaxScheme)otherScheme).styles;

		int length = styles.length;
		for (int i=0; i<length; i++) {
			if (styles[i]==null) {
				if (otherSchemes[i]!=null) {
					return false;
				}
			}
			else if (!styles[i].equals(otherSchemes[i])) {
				return false;
			}
		}
		return true;

	}


	/**
	 * Returns a hex string representing an RGB color, of the form
	 * <code>"$rrggbb"</code>.
	 *
	 * @param c The color.
	 * @return The string representation of the color.
	 */
	private static String getHexString(Color c) {
		return "$" + Integer.toHexString((c.getRGB() & 0xffffff)+0x1000000).
									substring(1);
	}


	/**
	 * Returns the specified style.
	 *
	 * @param index The index of the style.
	 * @return The style.
	 * @see #setStyle(int, Style)
	 * @see #getStyleCount()
	 */
	public Style getStyle(int index) {
		return styles[index];
	}


	/**
	 * Returns the number of styles.
	 *
	 * @return The number of styles.
	 * @see #getStyle(int)
	 */
	public int getStyleCount() {
		return styles.length;
	}


	/**
	 * Used by third party implementors e.g. SquirreL SQL. Most applications do
	 * not need to call this method.
	 * <p>
	 * Note that the returned array is not a copy of the style data; editing the
	 * array will modify the styles used by any <code>RSyntaxTextArea</code>
	 * using this scheme.
	 *
	 * @return The style array.
	 * @see #setStyles(Style[])
	 */
	public Style[] getStyles() {
		return styles;
	}


	/**
	 * This is implemented to be consistent with {@link #equals(Object)}.
	 * This is a requirement to keep FindBugs happy.
	 *
	 * @return The hash code for this object.
	 */
	@Override
	public int hashCode() {
		// Keep me fast.  Iterating over *all* syntax schemes contained is
		// probably much slower than a "bad" hash code here.
		int hashCode = 0;
		for (Style style : styles) {
			if (style != null) {
				hashCode ^= style.hashCode();
				break;
			}
		}
		return hashCode;
	}


	/**
	 * Loads a syntax scheme from an input stream.<p>
	 *
	 * Consider using the {@link Theme} class for saving and loading RSTA
	 * styles rather than using this API.
	 *
	 * @param baseFont The font to use as the "base" for the syntax scheme.
	 *        If this is <code>null</code>, a default monospaced font is used.
	 * @param in The stream to load from.  It is up to the caller to close this
	 *        stream when they are done.
	 * @return The syntax scheme.
	 * @throws IOException If an IO error occurs.
	 */
	public static SyntaxScheme load(Font baseFont, InputStream in)
									throws IOException {
		if (baseFont==null) {
			baseFont = RSyntaxTextArea.getDefaultFont();
		}
		return SyntaxSchemeLoader.load(baseFont, in);
	}


	/**
	 * Loads a syntax highlighting color scheme from a string created from
	 * <code>toCommaSeparatedString</code>.  This method is useful for saving
	 * and restoring color schemes.<p>
	 *
	 * Consider using the {@link Theme} class for saving and loading RSTA
	 * styles rather than using this API.
	 *
	 * @param string A string generated from {@link #toCommaSeparatedString()}.
	 * @return A color scheme.
	 * @see #toCommaSeparatedString()
	 */
	public static SyntaxScheme loadFromString(String string) {
		return loadFromString(string, DEFAULT_NUM_TOKEN_TYPES);
	}


	/**
	 * Loads a syntax highlighting color scheme from a string created from
	 * <code>toCommaSeparatedString</code>.  This method is useful for saving
	 * and restoring color schemes.<p>
	 *
	 * Consider using the {@link Theme} class for saving and loading RSTA
	 * styles rather than using this API.
	 *
	 * @param string A string generated from {@link #toCommaSeparatedString()}.
	 * @param tokenTypeCount The number of token types saved in this string.
	 *        This should be the number of token types saved by your custom
	 *        SyntaxScheme subclass,
	 *        or {@link TokenTypes#DEFAULT_NUM_TOKEN_TYPES} if you used the
	 *        standard implementation (which most people will).
	 * @return A color scheme.
	 * @see #loadFromString(String)
	 * @see #toCommaSeparatedString()
	 */
	public static SyntaxScheme loadFromString(String string,
			int tokenTypeCount) {

		SyntaxScheme scheme = new SyntaxScheme(true);

		try {

			if (string!=null) {

				String[] tokens = string.split(",", -1);

				// Check the version string, use defaults if incompatible
				if (tokens.length==0 || !VERSION.equals(tokens[0])) {
					return scheme; // Still set to defaults
				}

				int tokenCount = tokenTypeCount*7 + 1; // Version string
				if (tokens.length!=tokenCount) {
					throw new Exception(
						"Not enough tokens in packed color scheme: expected " +
						tokenCount + ", found " + tokens.length);
				}

				// Loop through each token style.  Format:
				// "index,(fg|-),(bg|-),(t|f),((font,style,size)|(-,,))"
				for (int i=0; i<tokenTypeCount; i++) {

					int pos = i*7 + 1;
					int integer = Integer.parseInt(tokens[pos]); // == i
					if (integer!=i) {
						throw new Exception("Expected " + i + ", found " +
											integer);
					}

					Color fg = null;
					String temp = tokens[pos+1];
					if (!"-".equals(temp)) { // "-" => keep fg as null
						fg = stringToColor(temp);
					}
					Color bg = null; temp = tokens[pos+2];
					if (!"-".equals(temp)) { // "-" => keep bg as null
						bg = stringToColor(temp);
					}

					// Check for "true" or "false" since we don't want to
					// accidentally suck in an int representing the next
					// packed color, and any string != "true" means false.
					temp = tokens[pos+3];
					if (!"t".equals(temp) && !"f".equals(temp)) {
						throw new Exception("Expected 't' or 'f', found " + temp);
					}
					boolean underline = "t".equals(temp);

					Font font = null;
					String family = tokens[pos+4];
					if (!"-".equals(family)) {
						int style= Integer.parseInt(tokens[pos+5]);
						int size = Integer.parseInt(tokens[pos+6]);
						Map<TextAttribute, Object> fontFamilyAttrs = new HashMap<>();
						fontFamilyAttrs.put(TextAttribute.FAMILY, family);
						font = RSyntaxTextArea.getDefaultFont()
							.deriveFont(fontFamilyAttrs)
							.deriveFont(style, size);
					}
					scheme.styles[i] = new Style(fg, bg, font, underline);

				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return scheme;

	}


	void refreshFontMetrics(Graphics2D g2d) {
		// It is assumed that any rendering hints are already applied to g2d.
		for (Style s : styles) {
			if (s != null) {
				s.fontMetrics = s.font == null ? null :
					g2d.getFontMetrics(s.font);
			}
		}
	}


	/**
	 * Restores all colors and fonts to their default values.
	 *
	 * @param baseFont The base font to use when creating this scheme.  If
	 *        this is <code>null</code>, then a default monospaced font is
	 *        used.
	 */
	public void restoreDefaults(Font baseFont) {
		restoreDefaults(baseFont, true);
	}


	/**
	 * Restores all colors and fonts to their default values.
	 *
	 * @param baseFont The base font to use when creating this scheme.  If
	 *        this is <code>null</code>, then a default monospaced font is
	 *        used.
	 * @param fontStyles Whether bold and italic should be used in the scheme
	 *        (vs. all tokens using a plain font).
	 */
	public void restoreDefaults(Font baseFont, boolean fontStyles) {

		// Colors used by tokens.
		Color comment			= new Color(0,128,0);
		Color docComment		= new Color(164,0,0);
		Color markupComment		= new Color(0, 96, 0);
		Color keyword			= Color.BLUE;
		Color dataType			= new Color(0,128,128);
		Color function			= new Color(173,128,0);
		Color preprocessor		= new Color(128,128,128);
		Color operator			= new Color(128, 64, 64);
		Color regex				= new Color(0,128,164);
		Color variable			= new Color(255,153,0);
		Color literalNumber		= new Color(100,0,200);
		Color literalString		= new Color(220,0,156);
		Color error			= new Color(148,148,0);

		// (Possible) special font styles for keywords and comments.
		if (baseFont==null) {
			baseFont = RSyntaxTextArea.getDefaultFont();
		}
		Font commentFont = baseFont;
		Font keywordFont = baseFont;
		if (fontStyles) {
			commentFont = baseFont.deriveFont(Font.ITALIC);
			keywordFont = baseFont.deriveFont(Font.BOLD);
		}

		styles[COMMENT_EOL]				= new Style(comment, null, commentFont);
		styles[COMMENT_MULTILINE]			= new Style(comment, null, commentFont);
		styles[COMMENT_DOCUMENTATION]		= new Style(docComment, null, commentFont);
		styles[COMMENT_KEYWORD]			= new Style(new Color(255,152,0), null, commentFont);
		styles[COMMENT_MARKUP]			= new Style(Color.gray, null, commentFont);
		styles[RESERVED_WORD]				= new Style(keyword, null, keywordFont);
		styles[RESERVED_WORD_2]			= new Style(keyword, null, keywordFont);
		styles[FUNCTION]					= new Style(function);
		styles[LITERAL_BOOLEAN]			= new Style(literalNumber);
		styles[LITERAL_NUMBER_DECIMAL_INT]	= new Style(literalNumber);
		styles[LITERAL_NUMBER_FLOAT]		= new Style(literalNumber);
		styles[LITERAL_NUMBER_HEXADECIMAL]	= new Style(literalNumber);
		styles[LITERAL_STRING_DOUBLE_QUOTE]	= new Style(literalString);
		styles[LITERAL_CHAR]				= new Style(literalString);
		styles[LITERAL_BACKQUOTE]			= new Style(literalString);
		styles[DATA_TYPE]				= new Style(dataType, null, keywordFont);
		styles[VARIABLE]					= new Style(variable);
		styles[REGEX]						= new Style(regex);
		styles[ANNOTATION]				= new Style(Color.gray);
		styles[IDENTIFIER]				= new Style(null);
		styles[WHITESPACE]				= new Style(Color.gray);
		styles[SEPARATOR]				= new Style(Color.RED);
		styles[OPERATOR]					= new Style(operator);
		styles[PREPROCESSOR]				= new Style(preprocessor);
		styles[MARKUP_TAG_DELIMITER]		= new Style(Color.RED);
		styles[MARKUP_TAG_NAME]			= new Style(Color.BLUE);
		styles[MARKUP_TAG_ATTRIBUTE]		= new Style(new Color(63,127,127));
		styles[MARKUP_TAG_ATTRIBUTE_VALUE]= new Style(literalString);
		styles[MARKUP_COMMENT]              = new Style(markupComment, null, commentFont);
		styles[MARKUP_DTD]              = new Style(function);
		styles[MARKUP_PROCESSING_INSTRUCTION] = new Style(preprocessor);
		styles[MARKUP_CDATA]				= new Style(new Color(0xcc6600));
		styles[MARKUP_CDATA_DELIMITER]		= new Style(new Color(0x008080));
		styles[MARKUP_ENTITY_REFERENCE]		= new Style(dataType);
		styles[ERROR_IDENTIFIER]			= new Style(error);
		styles[ERROR_NUMBER_FORMAT]		= new Style(error);
		styles[ERROR_STRING_DOUBLE]		= new Style(error);
		styles[ERROR_CHAR]				= new Style(error);

		// Issue #34: If an application modifies TokenTypes to add new built-in
		// token types, we'll get NPEs if not all styles are initialized.
		for (int i=0; i<styles.length; i++) {
			if (styles[i]==null) {
				styles[i] = new Style();
			}
		}

	}


	/**
	 * Sets a style to use when rendering a token type.
	 *
	 * @param type The token type.
	 * @param style The style for the token type.
	 * @see #getStyle(int)
	 */
	public void setStyle(int type, Style style) {
		styles[type] = style;
	}


	/**
	 * Used by third party implementors e.g. SquirreL SQL. Most applications do
	 * not need to call this method; individual styles can be set via
	 * {@link #setStyle(int, Style)}.
	 *
	 * @param styles The new array of styles to use.  Note that this should
	 *        have length of at least
	 *        {@link TokenTypes#DEFAULT_NUM_TOKEN_TYPES}.
	 * @see #setStyle(int, Style)
	 * @see #getStyles()
	 */
	public void setStyles(Style[] styles) {
		this.styles = styles;
	}


	/**
	 * Returns the color represented by a string.  If the first char in the
	 * string is '<code>$</code>', it is assumed to be in hex, otherwise it is
	 * assumed to be decimal.  So, for example, both of these:
	 * <pre>
	 * "$00ff00"
	 * "65280"
	 * </pre>
	 * will return <code>new Color(0, 255, 0)</code>.
	 *
	 * @param s The string to evaluate.
	 * @return The color.
	 */
	private static Color stringToColor(String s) {
		// Check for decimal as well as hex, for backward
		// compatibility (fix from GwynEvans on forums)
		char ch = s.charAt(0);
		return new Color((ch=='$' || ch=='#') ?
				Integer.parseInt(s.substring(1),16) :
				Integer.parseInt(s));
	}


	/**
	 * Returns this syntax highlighting scheme as a comma-separated list of
	 * values as follows:
	 * <ul>
	 *   <li>If a color is non-null, it is added as a 24-bit integer
	 *      of the form <code>((r&lt;*lt;16) | (g&lt;*lt;8) | (b))</code>; if
	 *      it is <code>null</code>, it is added as "<i>-,</i>".
	 *   <li>The font and style (bold/italic) is added as an integer like so:
	 *       "<i>family,</i> <i>style,</i> <i>size</i>".
	 *   <li>The entire syntax highlighting scheme is thus one long string of
	 *       color schemes of the format "<i>i,[fg],[bg],uline,[style]</i>,
	 *       where:
	 *       <ul>
	 *          <li><code>i</code> is the index of the syntax scheme.
	 *          <li><i>fg</i> and <i>bg</i> are the foreground and background
	 *              colors for the scheme, and may be null (represented by
	 *              <code>-</code>).
	 *          <li><code>uline</code> is whether the font should be
	 *              underlined, and is either <code>t</code> or <code>f</code>.
	 *          <li><code>style</code> is the <code>family,style,size</code>
	 *              triplet described above.
	 *       </ul>
	 * </ul>
	 *
	 * @return A string representing the rgb values of the colors.
	 * @see #loadFromString(String)
	 */
	public String toCommaSeparatedString() {

		StringBuilder sb = new StringBuilder(VERSION);
		sb.append(',');

		for (int i=0; i<styles.length; i++) {

			sb.append(i).append(',');

			Style ss = styles[i];
			if (ss==null) { // Only true for i==0 (NULL token)
				sb.append("-,-,f,-,,,");
				continue;
			}

			Color c = ss.foreground;
			sb.append(c!=null ? (getHexString(c) + ",") : "-,");
			c = ss.background;
			sb.append(c!=null ? (getHexString(c) + ",") : "-,");

			sb.append(ss.underline ? "t," : "f,");

			Font font = ss.font;
			if (font!=null) {
				sb.append(font.getFamily()).append(',').
					append(font.getStyle()).append(',').
						append(font.getSize()).append(',');
			}
			else {
				sb.append("-,,,");
			}

		}

		return sb.substring(0,sb.length()-1); // Take off final ','.

	}

	/**
	 * Loads a <code>SyntaxScheme</code> from an XML file.
	 */
	private static class SyntaxSchemeLoader extends DefaultHandler {

		private Font baseFont;
		private SyntaxScheme scheme;

		SyntaxSchemeLoader(Font baseFont) {
			scheme = new SyntaxScheme(baseFont);
		}

		public static SyntaxScheme load(Font baseFont, InputStream in)
				throws IOException {
			SyntaxSchemeLoader parser;
			try {
				XMLReader reader = XMLReaderFactory.createXMLReader();
				parser = new SyntaxSchemeLoader(baseFont);
				parser.baseFont = baseFont;
				reader.setContentHandler(parser);
				InputSource is = new InputSource(in);
				is.setEncoding("UTF-8");
				reader.parse(is);
			} catch (SAXException se) {
				throw new IOException(se.toString());
			}
			return parser.scheme;
		}

		@Override
		public void startElement(String uri, String localName, String qName,
								Attributes attrs) {

			if ("style".equals(qName)) {

				String type = attrs.getValue("token");

				Field field;
				try {
					field = SyntaxScheme.class.getField(type);
				} catch (RuntimeException re) {
					throw re; // FindBugs
				} catch (Exception e) {
					e.printStackTrace();
					return;
				}

				if (field.getType()==int.class) {

					int index;
					try {
						index = field.getInt(scheme);
					} catch (IllegalArgumentException | IllegalAccessException e) {
						e.printStackTrace();
						return;
					}

					String fgStr = attrs.getValue("fg");
					if (fgStr!=null) {
						Color fg = stringToColor(fgStr);
						scheme.styles[index].foreground = fg;
					}

					String bgStr = attrs.getValue("bg");
					if (bgStr!=null) {
						Color bg = stringToColor(bgStr);
						scheme.styles[index].background = bg;
					}

					boolean styleSpecified = false;
					boolean bold = false;
					boolean italic = false;
					String boldStr = attrs.getValue("bold");
					if (boldStr!=null) {
						bold = Boolean.parseBoolean(boldStr);
						styleSpecified = true;
					}
					String italicStr = attrs.getValue("italic");
					if (italicStr!=null) {
						italic = Boolean.parseBoolean(italicStr);
						styleSpecified = true;
					}
					if (styleSpecified) {
						int style = 0;
						if (bold) {
							style |= Font.BOLD;
						}
						if (italic) {
							style |= Font.ITALIC;
						}
						scheme.styles[index].font = baseFont.deriveFont(style);
					}

					String ulineStr = attrs.getValue("underline");
					if (ulineStr!=null) {
						boolean uline= Boolean.parseBoolean(ulineStr);
						scheme.styles[index].underline = uline;
					}

				}

			}

		}

	}


}
