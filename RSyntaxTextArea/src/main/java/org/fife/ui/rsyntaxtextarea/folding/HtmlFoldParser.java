/*
 * 09/30/2012
 *
 * HtmlFoldParser.java - Fold parser for HTML 5 and PHP.
 *
 * This library is distributed under a modified BSD license.  See the included
 * LICENSE file for details.
 */
package org.fife.ui.rsyntaxtextarea.folding;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import javax.swing.text.BadLocationException;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenTypes;


/**
 * Fold parser for HTML 5, PHP and JSP.  For HTML, we currently don't fold
 * <em>everything</em> possible, just the "big" stuff.  For PHP, we only fold
 * the "big" HTML stuff and PHP regions, not code blocks in the actual PHP.
 * For JSP we only fold the "big" HTML stuff and JSP blocks, not anything in
 * the actual Java code.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class HtmlFoldParser implements FoldParser {

	/**
	 * Constant denoting we're folding HTML.
	 */
	public static final int LANGUAGE_HTML	= -1;

	/**
	 * Constant denoting we're folding PHP.
	 */
	public static final int LANGUAGE_PHP	= 0;

	/**
	 * Constant denoting we're folding JSP.
	 */
	public static final int LANGUAGE_JSP	= 1;

	/**
	 * The language we're folding.
	 */
	private final int language;

	/**
	 * The set of tags we allow to be folded.  These are tags that must have
	 * explicit close tags in both HTML 4 and HTML 5.
	 */
	private static final Set<String> FOLDABLE_TAGS;

	private static final char[] MARKUP_CLOSING_TAG_START = "</".toCharArray();
	//private static final char[] MARKUP_SHORT_TAG_END = "/>".toCharArray();
	private static final char[] MLC_START = "<!--".toCharArray();
	private static final char[] MLC_END   = "-->".toCharArray();

	private static final char[] PHP_START = "<?".toCharArray(); // <? and <?php
	private static final char[] PHP_END = "?>".toCharArray();

	// Scriptlets, declarations, and expressions all start the same way.
	private static final char[] JSP_START = "<%".toCharArray();
	private static final char[] JSP_END = "%>".toCharArray();

	private static final char[][] LANG_START = { PHP_START, JSP_START };
	private static final char[][] LANG_END   = { PHP_END, JSP_END };

	private static final char[] JSP_COMMENT_START = "<%--".toCharArray();
	private static final char[] JSP_COMMENT_END   = "--%>".toCharArray();

	static {
		FOLDABLE_TAGS = new HashSet<>();
		FOLDABLE_TAGS.add("body");
		FOLDABLE_TAGS.add("canvas");
		FOLDABLE_TAGS.add("div");
		FOLDABLE_TAGS.add("form");
		FOLDABLE_TAGS.add("head");
		FOLDABLE_TAGS.add("html");
		FOLDABLE_TAGS.add("ol");
		FOLDABLE_TAGS.add("pre");
		FOLDABLE_TAGS.add("script");
		FOLDABLE_TAGS.add("span");
		FOLDABLE_TAGS.add("style");
		FOLDABLE_TAGS.add("table");
		FOLDABLE_TAGS.add("tfoot");
		FOLDABLE_TAGS.add("thead");
		FOLDABLE_TAGS.add("tr");
		FOLDABLE_TAGS.add("td");
		FOLDABLE_TAGS.add("ul");
	}


	/**
	 * Constructor.
	 *
	 * @param language The language to fold, such as {@link #LANGUAGE_PHP}.
	 */
	public HtmlFoldParser(int language) {
		if (language<LANGUAGE_HTML || language>LANGUAGE_JSP) {
			throw new IllegalArgumentException("Invalid language: " + language);
		}
		this.language = language;
	}


	@Override
	public List<Fold> getFolds(RSyntaxTextArea textArea) {

		List<Fold> folds = new ArrayList<>();
		Stack<String> tagNameStack = new Stack<>();
		boolean inSubLanguage = false;

		Fold currentFold = null;
		int lineCount = textArea.getLineCount();
		boolean inMLC = false;
		boolean inJSPMLC = false;
		TagCloseInfo tci = new TagCloseInfo();

		try {

			for (int line=0; line<lineCount; line++) {

				Token t = textArea.getTokenListForLine(line);
				while (t!=null && t.isPaintable()) {

					// If we're folding PHP.  Note that PHP folding can only be
					// "one level deep," so our logic here is simple.
					if (language>=0 && t.getType()==TokenTypes.MARKUP_TAG_DELIMITER) {

						// <?, <?php, <%, <%!, ...
						if (t.startsWith(LANG_START[language])) {
							if (currentFold==null) {
								currentFold = new Fold(FoldType.CODE, textArea, t.getOffset());
								folds.add(currentFold);
							}
							else {
								currentFold = currentFold.createChild(FoldType.CODE, t.getOffset());
							}
							inSubLanguage = true;
						}

						// ?> or %>
						else if (t.startsWith(LANG_END[language]) && currentFold != null) {
							int phpEnd = t.getEndOffset() - 1;
							currentFold.setEndOffset(phpEnd);
							Fold parentFold = currentFold.getParent();
							// Don't add fold markers for single-line blocks
							if (currentFold.isOnSingleLine()) {
								removeFold(currentFold, folds);
							}
							currentFold = parentFold;
							inSubLanguage = false;
							t = t.getNextToken();
							continue;
						}

					}

					if (!inSubLanguage) {

						if (t.getType()== TokenTypes.MARKUP_COMMENT) {

							// Continuing an MLC from a previous line
							if (inMLC) {
								// Found the end of the MLC starting on a previous line...
								if (t.endsWith(MLC_END)) {
									int mlcEnd = t.getEndOffset() - 1;
									currentFold.setEndOffset(mlcEnd);
									Fold parentFold = currentFold.getParent();
									// Don't add fold markers for single-line blocks
									if (currentFold.isOnSingleLine()) {
										removeFold(currentFold, folds);
									}
									currentFold = parentFold;
									inMLC = false;
								}
								// Otherwise, this MLC is continuing on to yet
								// another line.
							}

							// Continuing a JS MLC from a previous line
							else if (inJSPMLC) {
								// Found the end of the MLC starting on a previous line...
								if (t.endsWith(JSP_COMMENT_END)) {
									int mlcEnd = t.getEndOffset() - 1;
									currentFold.setEndOffset(mlcEnd);
									Fold parentFold = currentFold.getParent();
									// Don't add fold markers for single-line blocks
									if (currentFold.isOnSingleLine()) {
										removeFold(currentFold, folds);
									}
									currentFold = parentFold;
									inJSPMLC = false;
								}
								// Otherwise, this MLC is continuing on to yet
								// another line.
							}

							// Starting an MLC that ends on a later line...
							else if (t.startsWith(MLC_START) && !t.endsWith(MLC_END)) {
								if (currentFold==null) {
									currentFold = new Fold(FoldType.COMMENT, textArea, t.getOffset());
									folds.add(currentFold);
								}
								else {
									currentFold = currentFold.createChild(FoldType.COMMENT, t.getOffset());
								}
								inMLC = true;
							}

							// Starting a JSP comment that ends on a later line...
							else if (language==LANGUAGE_JSP &&
									t.startsWith(JSP_COMMENT_START) &&
									!t.endsWith(JSP_COMMENT_END)) {
								if (currentFold==null) {
									currentFold = new Fold(FoldType.COMMENT, textArea, t.getOffset());
									folds.add(currentFold);
								}
								else {
									currentFold = currentFold.createChild(FoldType.COMMENT, t.getOffset());
								}
								inJSPMLC = true;
							}

						}

						// If we're starting a new tag...
						else if (t.isSingleChar(TokenTypes.MARKUP_TAG_DELIMITER, '<')) {
							Token tagStartToken = t;
							Token tagNameToken = t.getNextToken();
							if (isFoldableTag(tagNameToken)) {
								int newLine = getTagCloseInfo(tagNameToken, textArea, line, tci);
								if (tci.line==-1) { // EOF reached before end of tag
									return folds;
								}
								// We have found either ">" or "/>" with tci.
								//System.out.println(line + ", "+ tci + ", " + t);
								Token tagCloseToken = tci.closeToken;
								if (tagCloseToken.isSingleChar(TokenTypes.MARKUP_TAG_DELIMITER, '>')) {
									if (currentFold==null) {
										currentFold = new Fold(FoldType.CODE, textArea,
												tagStartToken.getOffset());
										folds.add(currentFold);
									}
									else {
										currentFold = currentFold.createChild(FoldType.CODE,
												tagStartToken.getOffset());
									}
									tagNameStack.push(tagNameToken.getLexeme());
								}
								t = tagCloseToken; // Continue parsing after tag
								line = newLine;
							}
						}

						// If we've found a closing tag (e.g. "</div>").
						else if (t.is(TokenTypes.MARKUP_TAG_DELIMITER, MARKUP_CLOSING_TAG_START)) {
							if (currentFold!=null) {
								Token tagNameToken = t.getNextToken();
								if (isFoldableTag(tagNameToken) &&
										isEndOfLastFold(tagNameStack, tagNameToken)) {
									tagNameStack.pop();
									currentFold.setEndOffset(t.getOffset());
									Fold parentFold = currentFold.getParent();
									// Don't add fold markers for single-line blocks
									if (currentFold.isOnSingleLine()) {
										removeFold(currentFold, folds);
									}
									currentFold = parentFold;
									t = tagNameToken;
								}
							}
						}

					}

					t = t.getNextToken();

				}

			}

		} catch (BadLocationException ble) { // Should never happen
			ble.printStackTrace();
		}

		return folds;

	}


	/**
	 * Grabs the token representing the closing of a tag (i.e.
	 * "<code>&gt;</code>" or "<code>/&gt;</code>").  This should only be
	 * called after a tag name has been parsed, to ensure  the "closing" of
	 * other tags is not identified.
	 *
	 * @param tagNameToken The token denoting the name of the tag.
	 * @param textArea The text area whose contents are being parsed.
	 * @param line The line we're currently on.
	 * @param info On return, information about the closing of the tag is
	 *        returned in this object.
	 * @return The line number of the closing tag token.
	 */
	private int getTagCloseInfo(Token tagNameToken, RSyntaxTextArea textArea,
			int line, TagCloseInfo info) {

		info.reset();
		Token t = tagNameToken.getNextToken();

		do {

			while (t!=null && t.getType()!=TokenTypes.MARKUP_TAG_DELIMITER) {
				t = t.getNextToken();
			}

			if (t!=null) {
				info.closeToken = t;
				info.line = line;
				break;
			}

		} while (++line<textArea.getLineCount() &&
				(t=textArea.getTokenListForLine(line))!=null);

		return line;

	}


	/**
	 * Returns whether a closing tag ("<code>&lt;/...&gt;</code>") with a
	 * specific name is the closing tag of our current fold region.
	 *
	 * @param tagNameStack The stack of fold regions.
	 * @param tagNameToken The tag name of the most recently parsed closing
	 *        tag.
	 * @return Whether it's the end of the current fold region.
	 */
	private static boolean isEndOfLastFold(Stack<String> tagNameStack,
			Token tagNameToken) {
		if (tagNameToken!=null && tagNameToken.getLexeme() != null && !tagNameStack.isEmpty()) {
			return tagNameToken.getLexeme().equalsIgnoreCase(tagNameStack.peek());
		}
		return false;
	}


	/**
	 * Returns whether a tag is one we allow as a foldable region.
	 *
	 * @param tagNameToken The tag's name token.  This may be <code>null</code>.
	 * @return Whether this tag can be a foldable region.
	 */
	private static boolean isFoldableTag(Token tagNameToken) {
		return tagNameToken!=null && tagNameToken.getLexeme() != null &&
				FOLDABLE_TAGS.contains(tagNameToken.getLexeme().toLowerCase());
	}


	/**
	 * If this fold has a parent fold, this method removes it from its parent.
	 * Otherwise, it's assumed to be the most recent (top-level) fold in the
	 * <code>folds</code> list, and is removed from that.
	 *
	 * @param fold The fold to remove.
	 * @param folds The list of top-level folds.
	 */
	private static void removeFold(Fold fold, List<Fold> folds) {
		if (!fold.removeFromParent()) {
			folds.remove(folds.size()-1);
		}
	}


	/**
	 * A simple wrapper for the token denoting the closing of a tag (i.e.
	 * "<code>&gt;</code>" or "<code>/&gt;</code>").
	 */
	private static final class TagCloseInfo {

		private Token closeToken;
		private int line;

		void reset() {
			closeToken = null;
			line = -1;
		}

		@Override
		public String toString() {
			return "[TagCloseInfo: " +
					"closeToken=" + closeToken +
					", line=" + line +
					"]";
		}

	}


}
