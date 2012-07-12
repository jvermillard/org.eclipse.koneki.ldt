/*******************************************************************************
 * Copyright (c) 2009, 2012 Sierra Wireless and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/

package org.eclipse.koneki.ldt.ui.internal.editor.text;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.dltk.ui.text.AbstractScriptScanner;
import org.eclipse.dltk.ui.text.IColorManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWhitespaceDetector;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.NumberRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;
import org.eclipse.jface.text.rules.WordRule;

public class LuaCodeScanner extends AbstractScriptScanner {

	@SuppressWarnings("nls")
	private static String[] fgKeywords = { "and", "break", "do", "else", "elseif", "end", "false", "for", "function", "if", "in", "local", "nil",
			"not", "or", "repeat", "return", "then", "true", "until", "while" };

	private static String[] fgTokenProperties = new String[] { ILuaColorConstants.LUA_STRING, ILuaColorConstants.LUA_SINGLE_LINE_COMMENT,
			ILuaColorConstants.LUA_MULTI_LINE_COMMENT, ILuaColorConstants.LUA_NUMBER, ILuaColorConstants.LUA_DEFAULT, ILuaColorConstants.LUA_KEYWORD };

	public LuaCodeScanner(IColorManager manager, IPreferenceStore store) {
		super(manager, store);
		this.initialize();
	}

	protected String[] getTokenProperties() {
		return fgTokenProperties;
	}

	protected List<IRule> createRules() {
		List<IRule> rules = new ArrayList<IRule>();
		IToken keyword = this.getToken(ILuaColorConstants.LUA_KEYWORD);
		IToken comment = this.getToken(ILuaColorConstants.LUA_SINGLE_LINE_COMMENT);
		IToken multiline = this.getToken(ILuaColorConstants.LUA_MULTI_LINE_COMMENT);
		IToken numbers = this.getToken(ILuaColorConstants.LUA_NUMBER);
		IToken other = this.getToken(ILuaColorConstants.LUA_DEFAULT);

		// Add rule for multi-line comments
		rules.add(new MultiLineStringOrCommentRule(multiline, true));

		// Add rule for single line comments.
		rules.add(new EndOfLineRule("--", comment)); //$NON-NLS-1$

		// Add generic whitespace rule.
		rules.add(new WhitespaceRule(new LuaWhitespaceDetector()));

		// Add word rule for keywords.
		WordRule wordRule = new WordRule(new LuaWordDetector(), other);
		for (int i = 0; i < fgKeywords.length; i++) {
			wordRule.addWord(fgKeywords[i], keyword);
		}
		rules.add(wordRule);

		// Add number recognition
		NumberRule numberRule = new LuaNumberRule(numbers);
		rules.add(numberRule);

		// Default case
		this.setDefaultReturnToken(other);
		return rules;
	}

	/**
	 * Indicates if argument is a white space
	 * 
	 * @param char Tested character
	 */
	private static class LuaWhitespaceDetector implements IWhitespaceDetector {
		public boolean isWhitespace(char character) {
			return Character.isWhitespace(character);
		}
	}

	private static class LuaWordDetector implements IWordDetector {
		/**
		 * Indicates if argument is part of a word
		 * 
		 * @param char Tested character
		 */
		public boolean isWordPart(char character) {
			return Character.isJavaIdentifierPart(character);
		}

		/**
		 * Indicates if argument starts of a word
		 * 
		 * @param char Tested character
		 */
		public boolean isWordStart(char character) {
			return Character.isJavaIdentifierStart(character);
		}
	}

	private static class LuaNumberRule extends NumberRule {
		public LuaNumberRule(IToken token) {
			super(token);
		}

		public IToken evaluate(ICharacterScanner scanner) {
			int c = scanner.read();
			int p = c;
			if (Character.isDigit((char) c) || c == '.' || c == '-') {
				boolean hex = false;
				if (fColumn == UNDEFINED || (fColumn == scanner.getColumn() - 1)) {
					do {
						p = c;
						c = scanner.read();
						if (c == 'x' || c == 'X' && !hex) {
							hex = true;
							p = c;
							c = scanner.read();
						}
					} while (Character.isDigit((char) c) || (hex && Character.digit((char) c, 16) != -1));
					if (c != 'e' && c != 'E') {
						scanner.unread();
					}
					if (p == '.') {
						scanner.unread();
						return Token.UNDEFINED;
					}
					return fToken;
				}
			}
			scanner.unread();
			return Token.UNDEFINED;
		}
	}
}
