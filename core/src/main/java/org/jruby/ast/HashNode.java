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
 * Copyright (C) 2001-2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2001-2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
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

package org.jruby.ast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jruby.ast.types.ILiteralNode;
import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.util.KeyValuePair;

/**
 * A Literal Hash that can represent either a {a=&amp;b, c=&amp;d} type expression or the list 
 * of default values or kwarg in a method call.
 */
public class HashNode extends Node implements ILiteralNode {
    private final List<KeyValuePair<Node,Node>> pairs = new ArrayList<>();
    // contains at least one **k {a: 1, **k}, {**{}, **{}}
    private boolean hasRestKwarg = false;

    // Is this hash made up of only symbol characters (only of interest if this is !literal - simple kwargs processing)
    private boolean hasOnlySymbolKeys = true;

    // Is this a hash literal foo({a: 1}) vs a keyword hash foo(a: 1)
    private boolean isLiteral = false;

    public HashNode(int line) {
        super(line, false);
    }
    
    public HashNode(int line, KeyValuePair<Node,Node> pair) {
        this(line);

        add(pair);
    }

    public void setIsLiteral() {
        isLiteral = true;
    }

    public boolean isLiteral() {
        return isLiteral;
    }

    /**
     * Detect presence of a rest kwarg (**kw).
     *
     * @return true if it contains at least one rest kwarg.
     */
    public boolean hasRestKwarg() {
       return hasRestKwarg;
    }

    /**
     * Detect whether only rest kwargs make up this hash.  Common
     * case is **a which is HashNode{[(null, a)]}.  Less common is
     * **a, **b which is HashNode{[(null, a), (null, b)].}
     *
     * @return true is only rest kwargs
     */
    public boolean hasOnlyRestKwargs() {
        if (!hasRestKwarg) return false;

        for (KeyValuePair pair: pairs) {
            if (pair.getKey() != null) return false;
        }

        return true;
    }

    public NodeType getNodeType() {
        return NodeType.HASHNODE;
    }

    public HashNode add(KeyValuePair<Node,Node> pair) {
        Node key = pair.getKey();

        if (key != null && key.containsVariableAssignment() ||
                pair.getValue() != null && pair.getValue().containsVariableAssignment()) {
            containsVariableAssignment = true;
        }

        if (key == null) {
            hasRestKwarg = true;
            hasOnlySymbolKeys = false;
        } else if (!(pair.getKey() instanceof SymbolNode)) {
            hasOnlySymbolKeys = false;
        }

        pairs.add(pair);

        return this;
    }

    public boolean removeAll(final Collection<KeyValuePair<Node, Node>> pairsToRemove) {
        boolean changed = this.pairs.removeAll(pairsToRemove);

        if (changed) {
            // reset as we're to re-built internal state
            hasRestKwarg = false;
            isLiteral = false;
            hasOnlySymbolKeys = true;
            // clear pairs and add what is meant to be kept:
            Object[] pairsCopy = this.pairs.toArray();
            this.pairs.clear();
            for (int i = 0; i < pairsCopy.length; i++) {
                add((KeyValuePair<Node, Node>) pairsCopy[i]);
            }
            return true;
        }
        return false;
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public <T> T accept(NodeVisitor<T> iVisitor) {
        return iVisitor.visitHashNode(this);
    }

    public boolean isEmpty() {
        return pairs.isEmpty();
    }

    public List<KeyValuePair<Node,Node>> getPairs() {
        return pairs;
    }

    public List<Node> childNodes() {
        List<Node> children = new ArrayList<>();

        for (KeyValuePair<Node,Node> pair: pairs) {
            children.add(pair.getKey());
            children.add(pair.getValue());
        }

        return children;
    }

    @Override
    public String toStringExtraInfo() {
        return isLiteral() ?
                "literal" :
                (hasRestKwarg() ?
                        (hasOnlyRestKwargs() ? "onlykwrest" : "mixedkwrest") :
                        "kwarg");
    }

    public boolean hasOnlySymbolKeys() {
        return hasOnlySymbolKeys;
    }

    @Deprecated
    public boolean isMaybeKwargs() {
        return !isLiteral;
    }
}
