/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.parser;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.nio.channels.Channels;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyFile;
import org.jruby.RubyHash;
import org.jruby.RubyIO;
import org.jruby.ast.Node;
import org.jruby.lexer.ByteListLexerSource;
import org.jruby.lexer.GetsLexerSource;
import org.jruby.lexer.LexerSource;
import org.jruby.lexer.yacc.SyntaxException;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.LoadServiceResourceInputStream;
import org.jruby.util.ByteList;

/**
 * Serves as a simple facade for all the parsing magic.
 */
public class Parser {
    private final Ruby runtime;
    private volatile long totalTime;
    private volatile int totalBytes;

    public Parser(Ruby runtime) {
        this.runtime = runtime;
    }

    public long getTotalTime() {
        return totalTime;
    }

    public int getTotalBytes() {
        return totalBytes;
    }
    
    public Node parse(String file, ByteList content, DynamicScope blockScope,
            ParserConfiguration configuration) {
        configuration.setDefaultEncoding(content.getEncoding());
        RubyArray list = getLines(configuration, runtime, file);
        LexerSource lexerSource = new ByteListLexerSource(file, configuration.getLineNumber(), content, list);
        return parse(file, lexerSource, blockScope, configuration);
    }

    public Node parse(String file, byte[] content, DynamicScope blockScope,
            ParserConfiguration configuration) {
        RubyArray list = getLines(configuration, runtime, file);
        ByteList in = new ByteList(content, configuration.getDefaultEncoding());
        LexerSource lexerSource = new ByteListLexerSource(file, configuration.getLineNumber(), in,  list);
        return parse(file, lexerSource, blockScope, configuration);
    }

    public Node parse(String file, InputStream content, DynamicScope blockScope,
            ParserConfiguration configuration) {
        if (content instanceof LoadServiceResourceInputStream) {
            return parse(file, ((LoadServiceResourceInputStream) content).getBytes(), blockScope, configuration);
        } else {
            RubyArray list = getLines(configuration, runtime, file);
            boolean requiresClosing = false;
            RubyIO io;
            if (content instanceof FileInputStream) {
                io = new RubyFile(runtime, file, ((FileInputStream) content).getChannel());
            } else {
                requiresClosing = true;
                io = RubyIO.newIO(runtime, Channels.newChannel(content));
            }
            LexerSource lexerSource = new GetsLexerSource(file, configuration.getLineNumber(), io, list, configuration.getDefaultEncoding());

            try {
                return parse(file, lexerSource, blockScope, configuration);
            } finally {
                if (requiresClosing && runtime.getObject().getConstantAt("DATA") != io) io.close();

                // In case of GetsLexerSource we actually will dispatch to gets which will increment $.
                // We do not want that in the case of raw parsing.
                runtime.setCurrentLine(0);
            }
        }
    }

    public Node parse(String file, LexerSource lexerSource, DynamicScope blockScope,
            ParserConfiguration configuration) {
        // We only need to pass in current scope if we are evaluating as a block (which
        // is only done for evals).  We need to pass this in so that we can appropriately scope
        // down to captured scopes when we are parsing.
        if (blockScope != null) {
            configuration.parseAsBlock(blockScope);
        }

        long startTime = System.nanoTime();
        RubyParser parser = new RubyParser(lexerSource, runtime.getWarnings());
        RubyParserResult result;
        try {
            result = parser.parse(configuration);
            if (parser.lexer.isEndSeen() && configuration.isSaveData()) {
                IRubyObject verbose = runtime.getVerbose();
                runtime.setVerbose(runtime.getNil());
                runtime.defineGlobalConstant("DATA", lexerSource.getRemainingAsIO());
                runtime.setVerbose(verbose);
            }
        } catch (IOException e) {
            // Enebo: We may want to change this error to be more specific,
            // but I am not sure which conditions leads to this...so lame message.
            throw runtime.newSyntaxError("Problem reading source: " + e);
        } catch (SyntaxException e) {
            throw runtime.newSyntaxError(e.getFile() + ":" + (e.getLine() + 1) + ": " + e.getMessage());
        }

        Node ast = result.getAST();
        
        totalTime += System.nanoTime() - startTime;
        totalBytes += lexerSource.getOffset();

        return ast;
    }

    private RubyArray getLines(ParserConfiguration configuration, Ruby runtime, String file) {
        RubyArray list = null;
        IRubyObject scriptLines = runtime.getObject().getConstantAt("SCRIPT_LINES__");
        if (!configuration.isEvalParse() && scriptLines != null) {
            if (scriptLines instanceof RubyHash) {
                list = runtime.newArray();
                ((RubyHash) scriptLines).op_aset(runtime.getCurrentContext(), runtime.newString(file), list);
            }
        }
        return list;
    }
}
