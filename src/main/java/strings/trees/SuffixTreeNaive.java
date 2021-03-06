package strings.trees;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/*
un-optimized, generalized suffix tree
capacity = 1113988 strings (based on available terminating chars from 0x7b to 0x10ffff)
 */
public class SuffixTreeNaive extends Tree {

    ArrayList<String> terminatingChars = new ArrayList<>();
    private static final int FIRST_TERM_CHAR_VALUE = 0x7b;

    public SuffixTreeNaive() {}

    public void addString(String s) {
        String termChar = nextTerminatingChar();
        terminatingChars.add(termChar);
        //s += termChar;
        final String st = s + termChar;
        //allSuffixes(s).stream().forEach(sfx -> addSuffix(sfx));
        IntStream.range(0, st.length() - 1).boxed()
                .forEach(i -> addSuffix(st.substring(i), i));
    }

    private String nextTerminatingChar() {
        if (terminatingChars.isEmpty())
            return new String(Character.toChars(FIRST_TERM_CHAR_VALUE));
        String current = currentTermChar();
        return new String(Character.toChars(current.codePointAt(0) + 1));
    }

    static Set<String> allSuffixes(String s) {
        return IntStream.range(0, s.length() - 1).boxed()
                .map(i -> s.substring(i))
                .collect(Collectors.toSet());
    }

    /*
    add node
    - if no edges start with the first char -> new edge
    - else find edge that starts with first char
        - do char-by-char comparison until mismatch
            - split the edge
                - new node
    returns new node
    */
    public TreeNode addSuffix(String str, Integer position) {
        TreeNode node = root;
        //special case - first time through
        if (!node.hasChildren()) {
            return node.addChild(str, position);
        }
        while (node.hasChildren()) {
            List<String> e = getEdgeWithSameFirstChar(node.getChildren(), str);
            //if no edges start with the first char -> add a new one
            if (e.isEmpty()) {
                return node.addChild(str, position);
            }
            //else find edge that starts with first char
            String edge = e.get(0);
            int m = getLastMatchingIndex(str, edge);
            String edgeRemainder = edge.substring(m + 1);
            String strRemainder = str.substring(m + 1);
            String match = edge.substring(0, getLastMatchingIndex(str, edge) + 1);
            //keep traversing?
            if (str.startsWith(edge)) {
                node = node.getChild(edge);
                str = strRemainder;
            }
            //if not, split the edge
            else {
                TreeNode oldNode = node.getChild(edge);
                TreeNode newNode = new TreeNode(match, node);
                node.removeChild(edge);
                node.addChild(match, newNode);
                newNode.addChild(edgeRemainder, oldNode);
                return newNode.addChild(strRemainder, position);
            }
        }
        return null;
    }

    public Integer countNodes() {
        return breadthFirstTraversal(root).size();
    }

    public List<TreeNode> getCommonSubStringNodes() {
        return breadthFirstTraversal(root).stream()
                .filter(n -> (subTreeContainsAllInputs(n) && n.getParent() != null))
                .collect(Collectors.toList());
    }

    public Integer countCommonSubStrings() {
        return getCommonSubStringNodes().size();
    }

    public List<String> getCommonSubStrings() {
        return getCommonSubStringNodes().stream()
                .map(n -> nodeValue(n))
                .sorted( (s1, s2) -> s2.length() - s1.length())
                .collect(Collectors.toList());
    }

    public List<String> getLongestCommonSubStrings() {
        List<String> xs = getCommonSubStrings();
        Integer k = xs.get(0).length();
        return xs.stream()
                .filter(s -> s.length() == k)
                .collect(Collectors.toList());
    }

    //TODO: just use a sortedSet for the terminatingChars?
    // - check that it sorts by codepoint for single-char strings
    public boolean subTreeContainsAllInputs(TreeNode node) {
        HashSet<String> tc = new HashSet<>(terminatingChars);
        return getAllTerminatingCharsInTree(node).equals(tc);
    }

    public Set<String> getAllTerminatingCharsInTree(TreeNode node) {
        return breadthFirstTraversal(node).stream()
                .filter(n -> !n.hasChildren())
                .map(n -> lastChar(n.getValue()) )
                .collect(Collectors.toSet());
    }

    public String nodeValue(TreeNode n) {
        String s = "";
        LinkedList<String> stack = new LinkedList<>();
        while (n.getParent() != null) {
            String e = getEdge(n.getParent(), n);
            stack.push(e);
            n = n.getParent();
        }
        while (!stack.isEmpty()) {
            s = s + stack.pop();
        }
        return s;
    }

    public HashMap<String,List<Integer>> getStringPositions(String s){
        HashMap<String,List<Integer>> results = new HashMap<>();
        TreeNode node = find(s);
        for (TreeNode n :  breadthFirstTraversal(node).stream().filter(l -> !l.hasChildren()).collect(Collectors.toList())){
            String tc = lastChar(n.getValue());
            if (results.containsKey(tc)){
                List<Integer> positions = results.get(tc);
                positions.add(n.p);
                results.put(tc, positions);
            } else {
                List<Integer> positions = new ArrayList<>();
                positions.add(n.p);
                results.put(tc,positions);
            }
        }
        return results;
    }

    private String lastChar(String s){
        return s.substring(s.length() - 1);
    }

    private String getEdge(TreeNode parent, TreeNode child) {
        return parent.getChildren().entrySet().stream()
                .filter(e -> e.getValue().equals(child))
                .map(e -> e.getKey())
                .collect(Collectors.toList())
                .get(0);
    }

    public TreeNode find(String str) {
        TreeNode node = root;
        while (node.hasChildren()) {
            List<String> e = getEdgeWithSameFirstChar(node.getChildren(), str);
            if (e.isEmpty())
                return null;
            String edge = e.get(0);
            if (str.equals(edge) && isEndOfSuffix(node.getChild(edge))) {
                return node.getChild(edge);
            } else if (str.equals(edge.substring(0, edge.length() - 1)) && edgeContainsTerminus(edge)) {
                return node;
            } else if (str.startsWith(edge)) {
                node = node.getChild(edge);
                str = str.substring(edge.length());
            } else {
                return null;
            }
        }
        return null;
    }

    public boolean hasSuffix(String str) {
        TreeNode node = root;
        while (node.hasChildren()) {
            List<String> e = getEdgeWithSameFirstChar(node.getChildren(), str);
            if (e.isEmpty())
                return false;
            String edge = e.get(0);
            if (str.equals(edge) && isEndOfSuffix(node.getChild(edge))) {
                return true;
            } else if (str.equals(edge.substring(0, edge.length() - 1)) && edgeContainsTerminus(edge)) {
                return true;
            } else if (str.startsWith(edge)) {
                node = node.getChild(edge);
                str = str.substring(edge.length());
            } else {
                return false;
            }
        }
        return false;
    }

    private boolean isEndOfSuffix(TreeNode node) {
        if (!node.hasChildren())
            return true;
        return node.getChildren().keySet().stream()
                .anyMatch(k -> terminatingChars.contains(k));
    }

    private boolean edgeContainsTerminus(String edge) {
        return terminatingChars.stream().anyMatch(c -> c.equals(lastChar(edge)));
    }

    public String currentTermChar() {
        return terminatingChars.get(terminatingChars.size() - 1);
    }

    public String previousTermChar() {
        if (terminatingChars.size() == 1) {
            return terminatingChars.get(0);
        }
        return terminatingChars.get(terminatingChars.size() - 2);
    }


}
